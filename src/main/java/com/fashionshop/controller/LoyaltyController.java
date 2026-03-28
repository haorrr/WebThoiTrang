package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.repository.UserRepository;
import com.fashionshop.service.LoyaltyService;
import com.fashionshop.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
@Tag(name = "Loyalty & Referral", description = "Loyalty points and referral system")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final ReferralService referralService;
    private final UserRepository userRepository;

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

    private Long getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow().getId();
    }
}
