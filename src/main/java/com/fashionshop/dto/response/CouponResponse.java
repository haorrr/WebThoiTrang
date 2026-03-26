package com.fashionshop.dto.response;

import com.fashionshop.entity.Coupon;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CouponResponse {

    private Long id;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private Integer maxUses;
    private Integer usedCount;
    private LocalDateTime expiresAt;
    private String status;
    private LocalDateTime createdAt;

    public static CouponResponse from(Coupon c) {
        return CouponResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .discountType(c.getDiscountType().name())
                .discountValue(c.getDiscountValue())
                .minOrderAmount(c.getMinOrderAmount())
                .maxUses(c.getMaxUses())
                .usedCount(c.getUsedCount())
                .expiresAt(c.getExpiresAt())
                .status(c.getStatus().name())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
