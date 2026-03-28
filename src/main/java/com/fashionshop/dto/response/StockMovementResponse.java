package com.fashionshop.dto.response;

import com.fashionshop.entity.StockMovement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Long variantId;
    private Integer quantity;
    private String type;
    private String reference;
    private LocalDateTime createdAt;

    public static StockMovementResponse from(StockMovement sm) {
        return StockMovementResponse.builder()
                .id(sm.getId())
                .productId(sm.getProduct().getId())
                .productName(sm.getProduct().getName())
                .variantId(sm.getVariant() != null ? sm.getVariant().getId() : null)
                .quantity(sm.getQuantity())
                .type(sm.getType().name())
                .reference(sm.getReference())
                .createdAt(sm.getCreatedAt())
                .build();
    }
}
