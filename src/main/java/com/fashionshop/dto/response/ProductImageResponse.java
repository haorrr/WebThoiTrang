package com.fashionshop.dto.response;

import com.fashionshop.entity.ProductImage;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductImageResponse {

    private Long id;
    private String imageUrl;
    private boolean isPrimary;
    private int sortOrder;

    public static ProductImageResponse from(ProductImage img) {
        return ProductImageResponse.builder()
                .id(img.getId())
                .imageUrl(img.getImageUrl())
                .isPrimary(img.isPrimary())
                .sortOrder(img.getSortOrder())
                .build();
    }
}
