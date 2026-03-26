package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.dto.request.UpdateProfileRequest;
import com.fashionshop.dto.request.UpdateUserRequest;
import com.fashionshop.dto.response.UserResponse;
import com.fashionshop.entity.User;
import com.fashionshop.repository.UserRepository;
import com.fashionshop.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management and profile")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (Admin)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUsers(search, status, sort, page, size)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get own profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(Authentication auth) {
        String email = getEmail(auth);
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyProfile(email)));
    }

    @PutMapping("/me")
    @Operation(summary = "Update own profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            Authentication auth, @Valid @RequestBody UpdateProfileRequest req) {
        String email = getEmail(auth);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", userService.updateMyProfile(email, req)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID (Admin)")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user (Admin)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("User updated", userService.updateUser(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete user (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id, Authentication auth) {
        Long currentUserId = getCurrentUserId(auth);
        userService.deleteUser(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle user status ACTIVE/INACTIVE (Admin)")
    public ResponseEntity<ApiResponse<UserResponse>> toggleStatus(
            @PathVariable Long id, Authentication auth) {
        Long currentUserId = getCurrentUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok("Status updated", userService.toggleStatus(id, currentUserId)));
    }

    private String getEmail(Authentication auth) {
        return ((UserDetails) auth.getPrincipal()).getUsername();
    }

    private Long getCurrentUserId(Authentication auth) {
        String email = getEmail(auth);
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow();
    }
}
