package com.fashionshop.dto.response;

import com.fashionshop.entity.LoyaltyPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyPointResponse {

    private Long id;
    private Integer points;
    private String type;
    private String description;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public static LoyaltyPointResponse from(LoyaltyPoint lp) {
        return LoyaltyPointResponse.builder()
                .id(lp.getId())
                .points(lp.getPoints())
                .type(lp.getType().name())
                .description(lp.getDescription())
                .expiresAt(lp.getExpiresAt())
                .createdAt(lp.getCreatedAt())
                .build();
    }
}
