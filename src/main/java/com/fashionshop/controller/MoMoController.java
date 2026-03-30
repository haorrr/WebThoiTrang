package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.entity.Order;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.OrderRepository;
import com.fashionshop.repository.UserRepository;
import com.fashionshop.service.MoMoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/momo")
@RequiredArgsConstructor
@Tag(name = "MoMo Payment", description = "MoMo e-wallet payment integration")
public class MoMoController {

    private final MoMoService moMoService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /** Create MoMo payment — returns payUrl for redirect */
    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPayment(
            @RequestBody Map<String, Long> body,
            Authentication auth) {

        Long orderId = body.get("orderId");
        if (orderId == null) {
            throw new BadRequestException("orderId is required");
        }

        Long userId = getCurrentUserId(auth);
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!"MOMO".equals(order.getPaymentMethod())) {
            throw new BadRequestException("Order payment method is not MOMO");
        }

        String payUrl = moMoService.createPayment(orderId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("payUrl", payUrl)));
    }

    /** IPN endpoint — PUBLIC, called by MoMo server after payment */
    @PostMapping("/ipn")
    public ResponseEntity<Void> handleIPN(@RequestBody Map<String, Object> payload) {
        moMoService.handleIPN(payload);
        return ResponseEntity.noContent().build(); // HTTP 204 as required by MoMo
    }

    /** Return URL — PUBLIC, browser redirect after payment completes */
    @GetMapping("/return")
    public void handleReturn(
            @RequestParam(required = false, defaultValue = "") String orderId,
            @RequestParam(required = false, defaultValue = "-1") String resultCode,
            HttpServletResponse response) throws IOException {

        String status = "0".equals(resultCode) ? "success" : "failed";
        response.sendRedirect("/payment-result.html?status=" + status + "&orderId=" + orderId);
    }

    private Long getCurrentUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow().getId();
    }
}
