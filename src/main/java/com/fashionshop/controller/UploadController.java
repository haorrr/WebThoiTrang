package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "File upload to Cloudinary")
@SecurityRequirement(name = "bearerAuth")
public class UploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/image")
    @Operation(summary = "Upload image to Cloudinary (authenticated)")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        String url = cloudinaryService.upload(file, "fashion-shop/misc");
        return ResponseEntity.ok(ApiResponse.ok("Uploaded successfully", Map.of("url", url)));
    }
}
