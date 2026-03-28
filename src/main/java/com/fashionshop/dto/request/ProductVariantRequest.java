package com.fashionshop.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantRequest {

    private String size;
    private String color;
    private String colorCode;
    private String sku;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock = 0;

    private BigDecimal priceAdjustment = BigDecimal.ZERO;
}
