package com.fashionshop.dto.response;

import com.fashionshop.entity.OrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String size;
    private String color;
    private String imageUrl;

    public static OrderItemResponse from(OrderItem item) {
        var product = item.getProduct();
        String img = null;
        String productName = "Sản phẩm không còn tồn tại";
        Long productId = null;
        String productSlug = null;
        BigDecimal salePrice = null;
        if (product != null) {
            img = product.getImages().stream()
                    .filter(i -> i.isPrimary()).findFirst()
                    .map(i -> i.getImageUrl())
                    .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());
            productId = product.getId();
            productName = product.getName();
            productSlug = product.getSlug();
            salePrice = product.getSalePrice();
        }
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(productId)
                .productName(productName)
                .productSlug(productSlug)
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .salePrice(salePrice)
                .size(item.getSize())
                .color(item.getColor())
                .imageUrl(img)
                .build();
    }
}
