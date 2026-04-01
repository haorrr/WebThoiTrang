package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.entity.SystemConfig;
import com.fashionshop.entity.User;
import com.fashionshop.repository.SystemConfigRepository;
import com.fashionshop.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Security", description = "Security events and public config")
public class SecurityEventController {

    private final SystemConfigRepository configRepository;
    private final UserRepository userRepository;

    /**
     * Public endpoint: returns only the three security config flags for the frontend.
     */
    @GetMapping("/api/public/security-config")
    @Operation(summary = "Get public security settings")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getSecurityConfig() {
        try {
            boolean disableRightClick  = getBool("security.disable_right_click",  true);
            boolean disableDevtoolsKey = getBool("security.disable_devtools_key", true);
            boolean autoBanDevtools    = getBool("security.auto_ban_devtools",     false);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "disableRightClick",  disableRightClick,
                    "disableDevtoolsKey", disableDevtoolsKey,
                    "autoBanDevtools",    autoBanDevtools
            )));
        } catch (Exception e) {
            // Fallback: return safe defaults if DB is unavailable
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "disableRightClick",  true,
                    "disableDevtoolsKey", true,
                    "autoBanDevtools",    false
            )));
        }
    }

    /**
     * Called by frontend when DevTools is detected. If auto-ban is enabled and user is
     * authenticated (non-admin), sets their status to INACTIVE.
     */
    @PostMapping("/api/security/devtools-detected")
    @Operation(summary = "Report DevTools opened event")
    public ResponseEntity<ApiResponse<String>> devtoolsDetected(Authentication auth) {
        boolean autoBan = getBool("security.auto_ban_devtools", false);
        if (!autoBan || auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.ok(ApiResponse.ok("noted"));
        }

        String email = ((UserDetails) auth.getPrincipal()).getUsername();
        userRepository.findByEmail(email).ifPresent(user -> {
            // Never ban admins
            if (user.getRole() == User.Role.ADMIN) return;
            user.setStatus(User.Status.INACTIVE);
            userRepository.save(user);
        });

        return ResponseEntity.ok(ApiResponse.ok("account_suspended"));
    }

    private boolean getBool(String key, boolean defaultVal) {
        return configRepository.findById(key)
                .map(SystemConfig::getValue)
                .map(v -> "true".equalsIgnoreCase(v.trim()))
                .orElse(defaultVal);
    }
}
