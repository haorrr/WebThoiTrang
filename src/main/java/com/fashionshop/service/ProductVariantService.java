package com.fashionshop.service;

import com.fashionshop.dto.request.ProductVariantRequest;
import com.fashionshop.dto.response.ProductVariantResponse;
import com.fashionshop.entity.Product;
import com.fashionshop.entity.ProductVariant;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.ProductRepository;
import com.fashionshop.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariants(Long productId) {
        return variantRepository.findByProductId(productId).stream()
                .map(ProductVariantResponse::from).toList();
    }

    @Transactional
    public ProductVariantResponse create(Long productId, ProductVariantRequest req) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        // Check uniqueness
        if (req.getSize() != null || req.getColor() != null) {
            variantRepository.findByProductIdAndSizeAndColor(productId, req.getSize(), req.getColor())
                    .ifPresent(v -> { throw new BadRequestException("Variant with same size/color already exists"); });
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .size(req.getSize())
                .color(req.getColor())
                .colorCode(req.getColorCode())
                .sku(req.getSku())
                .stock(req.getStock() != null ? req.getStock() : 0)
                .priceAdjustment(req.getPriceAdjustment() != null ? req.getPriceAdjustment() : java.math.BigDecimal.ZERO)
                .build();

        return ProductVariantResponse.from(variantRepository.save(variant));
    }

    @Transactional
    public ProductVariantResponse update(Long productId, Long variantId, ProductVariantRequest req) {
        ProductVariant variant = findVariant(productId, variantId);

        if (req.getSize() != null) variant.setSize(req.getSize());
        if (req.getColor() != null) variant.setColor(req.getColor());
        if (req.getColorCode() != null) variant.setColorCode(req.getColorCode());
        if (req.getSku() != null) variant.setSku(req.getSku());
        if (req.getStock() != null) variant.setStock(req.getStock());
        if (req.getPriceAdjustment() != null) variant.setPriceAdjustment(req.getPriceAdjustment());

        return ProductVariantResponse.from(variantRepository.save(variant));
    }

    @Transactional
    public void delete(Long productId, Long variantId) {
        ProductVariant variant = findVariant(productId, variantId);
        variant.softDelete();
        variantRepository.save(variant);
    }

    @Transactional
    public ProductVariantResponse adjustStock(Long productId, Long variantId, int quantity) {
        ProductVariant variant = findVariant(productId, variantId);
        int newStock = variant.getStock() + quantity;
        if (newStock < 0) {
            throw new BadRequestException("Stock cannot go below 0. Current: " + variant.getStock());
        }
        variant.setStock(newStock);
        return ProductVariantResponse.from(variantRepository.save(variant));
    }

    private ProductVariant findVariant(Long productId, Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
        if (!variant.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("ProductVariant", variantId);
        }
        return variant;
    }
}
