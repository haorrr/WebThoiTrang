package com.fashionshop.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;

    private String trackingNumber;

    private java.time.LocalDate estimatedDelivery;
}
