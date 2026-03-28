package com.fashionshop.service;

import com.fashionshop.dto.response.ProductSummaryResponse;
import com.fashionshop.entity.Product;
import com.fashionshop.entity.User;
import com.fashionshop.entity.WishlistItem;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.ProductRepository;
import com.fashionshop.repository.UserRepository;
import com.fashionshop.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(w -> ProductSummaryResponse.from(w.getProduct()))
                .toList();
    }

    @Transactional
    public Map<String, Object> toggle(Long userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            wishlistRepository.deleteByUserIdAndProductId(userId, productId);
            return Map.of("wishlisted", false);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        wishlistRepository.save(WishlistItem.builder().user(user).product(product).build());
        return Map.of("wishlisted", true);
    }

    @Transactional(readOnly = true)
    public boolean isWishlisted(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }
}
