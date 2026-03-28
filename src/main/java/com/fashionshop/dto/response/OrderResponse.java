package com.fashionshop.dto.response;

import com.fashionshop.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private Long id;
    private Long userId;
    private String userEmail;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String shippingAddress;
    private String paymentMethod;
    private String couponCode;
    private String notes;
    private String trackingNumber;
    private LocalDate estimatedDelivery;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .userId(o.getUser().getId())
                .userEmail(o.getUser().getEmail())
                .status(o.getStatus().name())
                .totalAmount(o.getTotalAmount())
                .discountAmount(o.getDiscountAmount())
                .finalAmount(o.getTotalAmount().subtract(o.getDiscountAmount()))
                .shippingAddress(o.getShippingAddress())
                .paymentMethod(o.getPaymentMethod())
                .couponCode(o.getCoupon() != null ? o.getCoupon().getCode() : null)
                .notes(o.getNotes())
                .trackingNumber(o.getTrackingNumber())
                .estimatedDelivery(o.getEstimatedDelivery())
                .items(o.getItems().stream().map(OrderItemResponse::from).toList())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
