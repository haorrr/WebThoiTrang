package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.dto.request.FlashSaleRequest;
import com.fashionshop.dto.response.FlashSaleResponse;
import com.fashionshop.service.FlashSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Flash Sales", description = "Flash sale management")
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping("/api/flash-sales/active")
    @Operation(summary = "Get active flash sales (public)")
    public ResponseEntity<ApiResponse<List<FlashSaleResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.ok(flashSaleService.getActiveSales()));
    }

    @GetMapping("/api/flash-sales/{id}")
    @Operation(summary = "Get flash sale by ID (public)")
    public ResponseEntity<ApiResponse<FlashSaleResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(flashSaleService.getById(id)));
    }

    @GetMapping("/api/admin/flash-sales")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all flash sales (Admin)")
    public ResponseEntity<ApiResponse<Page<FlashSaleResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(flashSaleService.getAll(page, size)));
    }

    @PostMapping("/api/admin/flash-sales")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create flash sale (Admin)")
    public ResponseEntity<ApiResponse<FlashSaleResponse>> create(@Valid @RequestBody FlashSaleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Flash sale created", flashSaleService.create(req)));
    }

    @PutMapping("/api/admin/flash-sales/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update flash sale (Admin, SCHEDULED only)")
    public ResponseEntity<ApiResponse<FlashSaleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody FlashSaleRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Flash sale updated", flashSaleService.update(id, req)));
    }

    @DeleteMapping("/api/admin/flash-sales/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel/delete flash sale (Admin)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        flashSaleService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Flash sale cancelled", null));
    }

    @PostMapping("/api/admin/flash-sales/{id}/products")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add product to flash sale (Admin)")
    public ResponseEntity<ApiResponse<FlashSaleResponse>> addProduct(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(body.get("productId").toString());
        Integer stockLimit = body.containsKey("stockLimit") ? Integer.valueOf(body.get("stockLimit").toString()) : null;
        return ResponseEntity.ok(ApiResponse.ok("Product added", flashSaleService.addProduct(id, productId, stockLimit)));
    }

    @DeleteMapping("/api/admin/flash-sales/{id}/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove product from flash sale (Admin)")
    public ResponseEntity<ApiResponse<Void>> removeProduct(
            @PathVariable Long id, @PathVariable Long productId) {
        flashSaleService.removeProduct(id, productId);
        return ResponseEntity.ok(ApiResponse.ok("Product removed", null));
    }
}
