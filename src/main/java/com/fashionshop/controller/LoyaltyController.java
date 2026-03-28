package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.repository.UserRepository;
import com.fashionshop.service.LoyaltyService;
import com.fashionshop.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
@Tag(name = "Loyalty & Referral", description = "Loyalty points and referral system")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final ReferralService referralService;
    private final UserRepository userRepository;

    // ─── User endpoints ───────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get my loyalty points summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(loyaltyService.getSummary(getUserId(auth))));
    }

    @GetMapping("/referral-code")
    @Operation(summary = "Get or generate my referral code")
    public ResponseEntity<ApiResponse<Map<String, String>>> getReferralCode(Authentication auth) {
        String code = referralService.getOrGenerateCode(getUserId(auth));
        return ResponseEntity.ok(ApiResponse.ok(Map.of("referralCode", code)));
    }

    // ─── Admin endpoints ─────────────────────────────────────────

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users with their loyalty points")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listUsersWithPoints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(loyaltyService.listUsersWithPoints(page, size)));
    }

    @GetMapping("/admin/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get loyalty program configuration")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfig() {
        return ResponseEntity.ok(ApiResponse.ok(loyaltyService.getConfig()));
    }

    @PutMapping("/admin/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update loyalty program configuration")
    public ResponseEntity<ApiResponse<Void>> updateConfig(@RequestBody Map<String, Integer> body) {
        loyaltyService.updateConfig(
            body.getOrDefault("spendPerPoint", 10000),
            body.getOrDefault("vndPerPoint",   1000),
            body.getOrDefault("referralBonus", 50)
        );
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/admin/adjust/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually adjust a user's loyalty points")
    public ResponseEntity<ApiResponse<Void>> adjustPoints(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        int points = ((Number) body.get("points")).intValue();
        String reason = (String) body.getOrDefault("reason", null);
        loyaltyService.adminAdjustPoints(userId, points, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private Long getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow().getId();
    }
}
