package com.fashionshop.dto.response;

import com.fashionshop.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {

    private Long id;
    private String name;
    private String slug;
    private BigDecimal price;
    private BigDecimal salePrice;
    private BigDecimal flashPrice;
    private String flashEndsAt;
    private Integer stock;
    private String status;
    private String primaryImageUrl;
    private String categoryName;
    private java.util.Set<String> availableSizes;
    private java.util.Set<String> availableColors;

    public static ProductSummaryResponse from(Product p) {
        String primaryImage = p.getImages().stream()
                .filter(i -> i.isPrimary())
                .findFirst()
                .map(i -> i.getImageUrl())
                .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());

        java.util.Set<String> sizes = p.getVariants().stream()
                .filter(v -> v.getSize() != null && v.getStock() > 0)
                .map(v -> v.getSize())
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> colors = p.getVariants().stream()
                .filter(v -> v.getColor() != null && v.getStock() > 0)
                .map(v -> v.getColor())
                .collect(java.util.stream.Collectors.toSet());

        return ProductSummaryResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .slug(p.getSlug())
                .price(p.getPrice())
                .salePrice(p.getSalePrice())
                .stock(p.getStock())
                .status(p.getStatus().name())
                .primaryImageUrl(primaryImage)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .availableSizes(sizes)
                .availableColors(colors)
                .build();
    }
}
