package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.dto.request.ProductVariantRequest;
import com.fashionshop.dto.response.ProductVariantResponse;
import com.fashionshop.service.ProductVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products/{productId}/variants")
@RequiredArgsConstructor
@Tag(name = "Product Variants", description = "Product variant management")
public class ProductVariantController {

    private final ProductVariantService variantService;

    @GetMapping
    @Operation(summary = "List variants for a product (public)")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> list(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(variantService.getVariants(productId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create variant (Admin)")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> create(
            @PathVariable Long productId, @RequestBody ProductVariantRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Variant created", variantService.create(productId, req)));
    }

    @PutMapping("/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update variant (Admin)")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> update(
            @PathVariable Long productId, @PathVariable Long variantId,
            @RequestBody ProductVariantRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Variant updated", variantService.update(productId, variantId, req)));
    }

    @DeleteMapping("/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete variant (Admin)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long productId, @PathVariable Long variantId) {
        variantService.delete(productId, variantId);
        return ResponseEntity.ok(ApiResponse.ok("Variant deleted", null));
    }

    @PatchMapping("/{variantId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Adjust variant stock (Admin)")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> adjustStock(
            @PathVariable Long productId, @PathVariable Long variantId,
            @RequestBody Map<String, Integer> body) {
        int quantity = body.getOrDefault("quantity", 0);
        return ResponseEntity.ok(ApiResponse.ok("Stock adjusted", variantService.adjustStock(productId, variantId, quantity)));
    }
}
