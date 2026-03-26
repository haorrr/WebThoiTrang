package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.dto.request.ProductRequest;
import com.fashionshop.dto.response.ProductResponse;
import com.fashionshop.dto.response.ProductSummaryResponse;
import com.fashionshop.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog management")
public class ProductController {

    private final ProductService productService;
    private final com.fashionshop.service.GeminiService geminiService;

    @GetMapping
    @Operation(summary = "List products with filters (public)")
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                productService.getProducts(search, categoryId, minPrice, maxPrice, status, sort, dir, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID (public)")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getProductById(id)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product by slug (public)")
    public ResponseEntity<ApiResponse<ProductResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getProductBySlug(slug)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create product (Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Product created", productService.createProduct(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product (Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Product updated", productService.updateProduct(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete product (Admin)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted", null));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle product status (Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated", productService.toggleStatus(id)));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload product image (Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> addImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean isPrimary) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Image uploaded", productService.addImage(id, file, isPrimary)));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product image (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long id, @PathVariable Long imageId) {
        productService.deleteImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Image deleted", null));
    }

    @PostMapping("/{id}/ai-description")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate AI description for product (Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> generateAiDescription(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String features) {
        ProductResponse product = productService.getProductById(id);
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : "Thời trang";
        String description = geminiService.generateProductDescription(
                product.getName(), categoryName, features);
        return ResponseEntity.ok(ApiResponse.ok("AI description generated",
                productService.updateAiDescription(id, description)));
    }
}
