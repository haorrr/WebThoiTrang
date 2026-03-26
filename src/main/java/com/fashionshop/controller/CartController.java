package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.dto.request.AddCartItemRequest;
import com.fashionshop.dto.request.UpdateCartItemRequest;
import com.fashionshop.dto.response.CartItemResponse;
import com.fashionshop.dto.response.CartResponse;
import com.fashionshop.repository.UserRepository;
import com.fashionshop.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management")
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(userId)));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartItemResponse>> addItem(
            Authentication auth,
            @Valid @RequestBody AddCartItemRequest req) {
        Long userId = getCurrentUserId(auth);
        try {
            CartItemResponse response = cartService.addItem(userId, req);
            return ResponseEntity.ok(ApiResponse.ok("Item added to cart", response));
        } catch (RuntimeException e) {
            if ("UPDATED".equals(e.getMessage())) {
                return ResponseEntity.ok(ApiResponse.ok("Cart item quantity updated", null));
            }
            throw e;
        }
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateItem(
            Authentication auth,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest req) {
        Long userId = getCurrentUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(cartService.updateItem(userId, itemId, req)));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            Authentication auth,
            @PathVariable Long itemId) {
        Long userId = getCurrentUserId(auth);
        cartService.removeItem(userId, itemId);
        return ResponseEntity.ok(ApiResponse.ok("Item removed from cart", null));
    }

    @DeleteMapping
    @Operation(summary = "Clear entire cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication auth) {
        Long userId = getCurrentUserId(auth);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared", null));
    }

    private Long getCurrentUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow().getId();
    }
}
