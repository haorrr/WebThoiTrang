package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.dto.response.ProductSummaryResponse;
import com.fashionshop.repository.UserRepository;
import com.fashionshop.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "User wishlist management")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get my wishlist")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getWishlist(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(wishlistService.getWishlist(getUserId(auth))));
    }

    @PostMapping("/{productId}")
    @Operation(summary = "Toggle product in wishlist")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggle(
            Authentication auth, @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(wishlistService.toggle(getUserId(auth), productId)));
    }

    @GetMapping("/check/{productId}")
    @Operation(summary = "Check if product is in wishlist")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> check(
            Authentication auth, @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("wishlisted", wishlistService.isWishlisted(getUserId(auth), productId))));
    }

    private Long getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow().getId();
    }
}
