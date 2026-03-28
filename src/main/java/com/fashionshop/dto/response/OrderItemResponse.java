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
        String img = item.getProduct().getImages().stream()
                .filter(i -> i.isPrimary())
                .findFirst()
                .map(i -> i.getImageUrl())
                .orElse(item.getProduct().getImages().isEmpty() ? null
                        : item.getProduct().getImages().get(0).getImageUrl());
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productSlug(item.getProduct().getSlug())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .salePrice(item.getProduct().getSalePrice())
                .size(item.getSize())
                .color(item.getColor())
                .imageUrl(img)
                .build();
    }
}
