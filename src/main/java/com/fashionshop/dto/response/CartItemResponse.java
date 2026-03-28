package com.fashionshop.dto.response;

import com.fashionshop.entity.CartItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private BigDecimal price;
    private BigDecimal salePrice;
    private BigDecimal flashPrice;
    private Integer quantity;
    private String size;
    private String color;
    private String imageUrl;

    public static CartItemResponse from(CartItem item) {
        String img = null;
        if (item.getProduct() != null && !item.getProduct().getImages().isEmpty()) {
            img = item.getProduct().getImages().stream()
                    .filter(i -> i.isPrimary())
                    .findFirst()
                    .map(i -> i.getImageUrl())
                    .orElse(item.getProduct().getImages().get(0).getImageUrl());
        }
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productSlug(item.getProduct().getSlug())
                .price(item.getProduct().getPrice())
                .salePrice(item.getProduct().getSalePrice())
                .quantity(item.getQuantity())
                .size(item.getSize())
                .color(item.getColor())
                .imageUrl(img)
                .build();
    }
}
