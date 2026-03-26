package com.fashionshop.controller;

import com.fashionshop.dto.ApiResponse;
import com.fashionshop.dto.request.ChatRequest;
import com.fashionshop.dto.response.ChatResponse;
import com.fashionshop.service.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "Gemini AI-powered features")
public class AiController {

    private final GeminiService geminiService;

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI fashion assistant")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest req) {
        String reply = geminiService.chat(req.getMessage());
        return ResponseEntity.ok(ApiResponse.ok(ChatResponse.builder().reply(reply).build()));
    }

    @PostMapping("/generate-description")
    @Operation(summary = "Generate AI product description")
    public ResponseEntity<ApiResponse<ChatResponse>> generateDescription(
            @RequestParam String productName,
            @RequestParam String category,
            @RequestParam(defaultValue = "") String features) {
        String description = geminiService.generateProductDescription(productName, category, features);
        return ResponseEntity.ok(ApiResponse.ok(ChatResponse.builder().reply(description).build()));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get AI product recommendations")
    public ResponseEntity<ApiResponse<ChatResponse>> getRecommendations(
            @RequestParam String preferences,
            @RequestParam(defaultValue = "500000") String budget) {
        String recommendations = geminiService.getRecommendations(preferences, budget);
        return ResponseEntity.ok(ApiResponse.ok(ChatResponse.builder().reply(recommendations).build()));
    }
}
