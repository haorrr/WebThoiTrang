package com.fashionshop.service;

import com.fashionshop.config.GeminiConfig;
import com.fashionshop.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String SYSTEM_PROMPT =
            "Bạn là trợ lý mua sắm thời trang của FashionShop - một cửa hàng thời trang trực tuyến. " +
            "Hãy giúp khách hàng tìm kiếm sản phẩm phù hợp, tư vấn phong cách, và trả lời câu hỏi về sản phẩm. " +
            "Trả lời ngắn gọn, thân thiện bằng tiếng Việt.";

    public String chat(String userMessage) {
        try {
            String requestBody = buildChatRequest(userMessage);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(geminiConfig.buildApiUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Gemini API error: {} - {}", response.statusCode(), response.body());
                throw new BadRequestException("AI service unavailable. Please try again later.");
            }

            return extractText(response.body());
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini chat error", e);
            throw new BadRequestException("AI service unavailable. Please try again later.");
        }
    }

    public String generateProductDescription(String productName, String category, String features) {
        String prompt = String.format(
                "Viết mô tả sản phẩm hấp dẫn cho sản phẩm thời trang sau bằng tiếng Việt (150-200 từ):\n" +
                "Tên: %s\nDanh mục: %s\nTính năng: %s\n" +
                "Mô tả phải nêu bật điểm nổi bật, chất liệu, phong cách và đối tượng phù hợp.",
                productName, category, features);
        return chat(prompt);
    }

    public String getRecommendations(String userPreferences, String budget) {
        String prompt = String.format(
                "Dựa trên sở thích: %s và ngân sách: %s VNĐ, " +
                "hãy gợi ý 3-5 loại sản phẩm thời trang phù hợp từ FashionShop. " +
                "Trả lời ngắn gọn, kèm lý do tại sao phù hợp.",
                userPreferences, budget);
        return chat(prompt);
    }

    private String buildChatRequest(String message) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();

        // System instruction
        ObjectNode systemInstruction = objectMapper.createObjectNode();
        ObjectNode systemPart = objectMapper.createObjectNode();
        systemPart.put("text", SYSTEM_PROMPT);
        ArrayNode systemParts = objectMapper.createArrayNode();
        systemParts.add(systemPart);
        systemInstruction.set("parts", systemParts);
        root.set("systemInstruction", systemInstruction);

        // User content
        ArrayNode contents = objectMapper.createArrayNode();
        ObjectNode content = objectMapper.createObjectNode();
        content.put("role", "user");
        ArrayNode parts = objectMapper.createArrayNode();
        ObjectNode part = objectMapper.createObjectNode();
        part.put("text", message);
        parts.add(part);
        content.set("parts", parts);
        contents.add(content);
        root.set("contents", contents);

        // Generation config
        ObjectNode genConfig = objectMapper.createObjectNode();
        genConfig.put("maxOutputTokens", 1024);
        genConfig.put("temperature", 0.7);
        root.set("generationConfig", genConfig);

        return objectMapper.writeValueAsString(root);
    }

    private String extractText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            return "Xin lỗi, tôi không thể trả lời câu hỏi này. Vui lòng thử lại.";
        }
        return candidates.get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText("Xin lỗi, tôi không thể trả lời câu hỏi này. Vui lòng thử lại.");
    }
}
