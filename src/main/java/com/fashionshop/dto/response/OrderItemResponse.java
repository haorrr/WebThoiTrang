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
    private String size;
    private String color;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productSlug(item.getProduct().getSlug())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .size(item.getSize())
                .color(item.getColor())
                .build();
    }
}
