package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.dto.response.StockMovementResponse;
import com.fashionshop.repository.UserRepository;
import com.fashionshop.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Inventory", description = "Stock movement management")
public class InventoryController {

    private final InventoryService inventoryService;
    private final UserRepository userRepository;

    @GetMapping("/products/{productId}/movements")
    @Operation(summary = "Get stock movement history for a product")
    public ResponseEntity<ApiResponse<Page<StockMovementResponse>>> getMovements(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getMovements(productId, page, size)));
    }

    @PostMapping("/adjust")
    @Operation(summary = "Manually adjust stock")
    public ResponseEntity<ApiResponse<StockMovementResponse>> adjust(
            @RequestBody Map<String, Object> body, Authentication auth) {
        Long productId = Long.valueOf(body.get("productId").toString());
        Long variantId = body.containsKey("variantId") && body.get("variantId") != null
                ? Long.valueOf(body.get("variantId").toString()) : null;
        int quantity = Integer.parseInt(body.get("quantity").toString());
        String reason = body.getOrDefault("reason", "Manual adjustment").toString();
        Long adminId = userRepository.findByEmail(auth.getName()).orElseThrow().getId();
        return ResponseEntity.ok(ApiResponse.ok(
                inventoryService.adjustStock(productId, variantId, quantity, reason, adminId)));
    }
}
