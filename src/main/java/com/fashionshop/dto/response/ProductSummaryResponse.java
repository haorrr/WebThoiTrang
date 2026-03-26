package com.fashionshop.dto.response;

import com.fashionshop.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductSummaryResponse {

    private Long id;
    private String name;
    private String slug;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Integer stock;
    private String status;
    private String primaryImageUrl;
    private String categoryName;

    public static ProductSummaryResponse from(Product p) {
        String primaryImage = p.getImages().stream()
                .filter(i -> i.isPrimary())
                .findFirst()
                .map(i -> i.getImageUrl())
                .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());

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
                .build();
    }
}
