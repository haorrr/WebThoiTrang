package com.fashionshop.dto.response;

import com.fashionshop.entity.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private Long id;
    private String size;
    private String color;
    private String colorCode;
    private String sku;
    private Integer stock;
    private BigDecimal priceAdjustment;

    public static ProductVariantResponse from(ProductVariant v) {
        return ProductVariantResponse.builder()
                .id(v.getId())
                .size(v.getSize())
                .color(v.getColor())
                .colorCode(v.getColorCode())
                .sku(v.getSku())
                .stock(v.getStock())
                .priceAdjustment(v.getPriceAdjustment())
                .build();
    }
}
