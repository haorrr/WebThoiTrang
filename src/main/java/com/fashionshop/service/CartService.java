package com.fashionshop.service;

import com.fashionshop.dto.request.AddCartItemRequest;
import com.fashionshop.dto.request.UpdateCartItemRequest;
import com.fashionshop.dto.response.CartItemResponse;
import com.fashionshop.dto.response.CartResponse;
import com.fashionshop.entity.CartItem;
import com.fashionshop.entity.Product;
import com.fashionshop.entity.ProductVariant;
import com.fashionshop.entity.User;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.CartItemRepository;
import com.fashionshop.repository.ProductRepository;
import com.fashionshop.repository.ProductVariantRepository;
import com.fashionshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        List<CartItemResponse> responses = items.stream().map(CartItemResponse::from).toList();

        BigDecimal subtotal = items.stream()
                .map(i -> i.getProduct().getEffectivePrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .items(responses)
                .totalItems(items.size())
                .subtotal(subtotal)
                .build();
    }

    @Transactional
    public CartItemResponse addItem(Long userId, AddCartItemRequest req) {
        Product product = productRepository.findById(req.getProductId())
                .filter(p -> !p.isDeleted() && p.getStatus() == Product.Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Product", req.getProductId()));

        // Validate stock — use variant stock if variantId provided
        ProductVariant variant = null;
        if (req.getVariantId() != null) {
            variant = variantRepository.findById(req.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", req.getVariantId()));
            if (variant.getStock() < req.getQuantity()) {
                throw new BadRequestException("Insufficient stock for variant. Available: " + variant.getStock());
            }
        } else if (product.getStock() < req.getQuantity()) {
            throw new BadRequestException("Insufficient stock. Available: " + product.getStock());
        }

        String size = variant != null ? variant.getSize() : req.getSize();
        String color = variant != null ? variant.getColor() : req.getColor();

        // Check if same item already in cart
        cartItemRepository.findByUserIdAndProductIdAndSizeAndColor(userId, req.getProductId(), size, color)
                .ifPresent(existing -> {
                    existing.setQuantity(existing.getQuantity() + req.getQuantity());
                    cartItemRepository.save(existing);
                    throw new RuntimeException("UPDATED"); // break flow trick
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        CartItem item = CartItem.builder()
                .user(user)
                .product(product)
                .variant(variant)
                .quantity(req.getQuantity())
                .size(size)
                .color(color)
                .build();

        return CartItemResponse.from(cartItemRepository.save(item));
    }

    @Transactional
    public CartItemResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest req) {
        CartItem item = cartItemRepository.findById(itemId)
                .filter(i -> i.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        if (req.getQuantity() > item.getProduct().getStock()) {
            throw new BadRequestException("Insufficient stock");
        }

        item.setQuantity(req.getQuantity());
        return CartItemResponse.from(cartItemRepository.save(item));
    }

    @Transactional
    public void removeItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .filter(i -> i.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));
        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}
