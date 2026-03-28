package com.fashionshop.service;

import com.fashionshop.dto.response.StockMovementResponse;
import com.fashionshop.entity.*;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovements(Long productId, int page, int size) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, PageRequest.of(page, size))
                .map(StockMovementResponse::from);
    }

    @Transactional
    public void recordMovement(Product product, ProductVariant variant, int quantity, StockMovement.Type type, String reference, User createdBy) {
        stockMovementRepository.save(StockMovement.builder()
                .product(product)
                .variant(variant)
                .quantity(quantity)
                .type(type)
                .reference(reference)
                .createdBy(createdBy)
                .build());
    }

    @Transactional
    public StockMovementResponse adjustStock(Long productId, Long variantId, int quantity, String reason, Long adminId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminId));

        ProductVariant variant = null;
        if (variantId != null) {
            variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
            int newStock = variant.getStock() + quantity;
            if (newStock < 0) throw new BadRequestException("Stock cannot go below 0");
            variant.setStock(newStock);
            variantRepository.save(variant);
        } else {
            int newStock = product.getStock() + quantity;
            if (newStock < 0) throw new BadRequestException("Stock cannot go below 0");
            product.setStock(newStock);
            productRepository.save(product);
        }

        StockMovement movement = StockMovement.builder()
                .product(product)
                .variant(variant)
                .quantity(quantity)
                .type(StockMovement.Type.MANUAL_ADJUST)
                .reference(reason)
                .createdBy(admin)
                .build();
        return StockMovementResponse.from(stockMovementRepository.save(movement));
    }
}
