package com.fashionshop.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "Discount type is required")
    @Pattern(regexp = "PERCENT|FIXED", message = "Type must be PERCENT or FIXED")
    private String discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin("0.01")
    private BigDecimal discountValue;

    @DecimalMin("0")
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Min(1)
    private Integer maxUses = 1;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry must be in the future")
    private LocalDateTime expiresAt;
}
