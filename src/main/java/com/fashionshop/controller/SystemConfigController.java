package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.entity.SystemConfig;
import com.fashionshop.repository.SystemConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
@Tag(name = "System Config", description = "Admin system configuration")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class SystemConfigController {

    private final SystemConfigRepository configRepository;

    @GetMapping
    @Operation(summary = "Get all system config keys")
    public ResponseEntity<ApiResponse<List<SystemConfig>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(configRepository.findAll()));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Update a config value")
    public ResponseEntity<ApiResponse<SystemConfig>> update(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        SystemConfig config = configRepository.findById(key)
                .orElse(new SystemConfig(key, body.get("value"), null));
        config.setValue(body.get("value"));
        return ResponseEntity.ok(ApiResponse.ok("Config updated", configRepository.save(config)));
    }
}
