package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.dto.request.CouponRequest;
import com.fashionshop.dto.request.ValidateCouponRequest;
import com.fashionshop.dto.response.CouponResponse;
import com.fashionshop.dto.response.CouponValidationResponse;
import com.fashionshop.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Coupon management")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Validate a coupon code")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validate(
            @Valid @RequestBody ValidateCouponRequest req) {
        CouponValidationResponse response = couponService.validateCoupon(req.getCode(), req.getOrderAmount());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // Admin CRUD
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all coupons (admin)")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(couponService.getAllCoupons()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get coupon by ID (admin)")
    public ResponseEntity<ApiResponse<CouponResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.getCouponById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create coupon (admin)")
    public ResponseEntity<ApiResponse<CouponResponse>> create(@Valid @RequestBody CouponRequest req) {
        CouponResponse response = couponService.createCoupon(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Coupon created", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update coupon (admin)")
    public ResponseEntity<ApiResponse<CouponResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.updateCoupon(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete coupon (admin)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.ok("Coupon deleted", null));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle coupon status (admin)")
    public ResponseEntity<ApiResponse<CouponResponse>> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.toggleStatus(id)));
    }
}
