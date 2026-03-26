package com.fashionshop.dto.response;

import com.fashionshop.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String aiDescription;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Integer stock;
    private String status;
    private CategoryResponse category;
    private List<ProductImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .slug(p.getSlug())
                .description(p.getDescription())
                .aiDescription(p.getAiDescription())
                .price(p.getPrice())
                .salePrice(p.getSalePrice())
                .stock(p.getStock())
                .status(p.getStatus().name())
                .category(p.getCategory() != null ? CategoryResponse.from(p.getCategory()) : null)
                .images(p.getImages().stream().map(ProductImageResponse::from).toList())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
