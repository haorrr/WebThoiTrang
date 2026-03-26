package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.dto.request.CreateReviewRequest;
import com.fashionshop.dto.request.UpdateReviewRequest;
import com.fashionshop.dto.response.ReviewResponse;
import com.fashionshop.repository.UserRepository;
import com.fashionshop.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get approved reviews for a product")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getProductReviews(productId, true, page, size)));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getMyReviews(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getUserReviews(userId, page, size)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            Authentication auth,
            @Valid @RequestBody CreateReviewRequest req) {
        Long userId = getCurrentUserId(auth);
        ReviewResponse response = reviewService.createReview(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Review submitted successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update own review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest req) {
        Long userId = getCurrentUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(reviewService.updateReview(userId, id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete own review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            Authentication auth,
            @PathVariable Long id) {
        Long userId = getCurrentUserId(auth);
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        reviewService.deleteReview(userId, id, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Review deleted", null));
    }

    // Admin endpoints
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all reviews (admin)")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getAllReviews(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getAllReviews(status, page, size)));
    }

    @PatchMapping("/admin/{id}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Moderate a review (approve/reject)")
    public ResponseEntity<ApiResponse<ReviewResponse>> moderateReview(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.moderateReview(id, status)));
    }

    private Long getCurrentUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow().getId();
    }
}
