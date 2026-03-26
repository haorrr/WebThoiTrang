# Phase 06 — AI (Google Gemini) Integration

## Context Links

- [plan.md](plan.md)
- [Research 02 — Gemini API & Prompts](research/researcher-02-report.md)
- Depends on: Phase 04 (Product entity, ProductService)

---

## Overview

| Field | Value |
|-------|-------|
| Date | 2026-03-26 |
| Priority | MEDIUM |
| Implementation Status | PENDING |
| Review Status | PENDING |
| Description | Integrate Google Gemini 1.5 Flash for four features: auto-generate product descriptions, fashion chatbot, product recommendations, and search autocomplete suggestions. |

---

## Key Insights

- Use the official Google AI Java SDK (`google-ai-java:0.9.0`) — simpler than Spring AI which is still in Milestone releases and has more config overhead.
- All Gemini calls are wrapped in a single `GeminiService.generateContent(String prompt)` method — keeps it testable and mockable.
- Gemini free tier (API key, not Vertex) is sufficient for academic project: 15 requests/min, 1M tokens/day.
- AI endpoints are expensive — add simple in-memory rate limiting (token bucket per IP) in Phase 7; for now just add a comment/TODO.
- `aiDescription` field on Product is optional — if Gemini call fails, log error and return empty string; never block product creation.
- Chatbot has no session state — each request is stateless (send product context as system prompt each time). YAGNI for conversation history.

---

## Requirements

### Functional
- `POST /api/products/{id}/ai-description` (ADMIN) — generate and save AI description for product
- `POST /api/ai/chat` — fashion chatbot, stateless, any user
- `POST /api/ai/recommendations?productId=` — return 3-5 similar product suggestions by AI
- `POST /api/ai/search-suggestions?q=` — return 5 autocomplete queries

### Technical
- Gemini model: `gemini-1.5-flash` (free tier)
- Max tokens per request: 500 (sufficient for all use cases)
- Timeout: 10 seconds; on timeout → return fallback response
- Prompts are externalized as constants in `GeminiPrompts.java` (not hardcoded inline)

---

## Architecture

### Package Structure
```
com.fashionshop/
├── service/
│   └── GeminiService.java
├── controller/
│   └── AiController.java
├── dto/
│   ├── request/
│   │   └── ChatRequest.java
│   └── response/
│       ├── ChatResponse.java
│       └── AiSuggestionsResponse.java
└── util/
    └── GeminiPrompts.java
```

### Gemini Call Pattern
```java
GenerativeModel model = new GenerativeModel("gemini-1.5-flash", apiKey);
GenerateContentResponse response = model.generateContent(prompt);
String text = response.getCandidates().get(0).getContent().getParts().get(0).getText();
```

### Prompt Templates (in `GeminiPrompts`)
```
PRODUCT_DESCRIPTION:
"Write a compelling 100-word fashion product description in Vietnamese for:
Product: {name}
Category: {category}
Price: {price}VND
{existingDescription}
Focus on style, material feel, and outfit pairing suggestions."

CHATBOT:
"You are a helpful fashion assistant for a Vietnamese fashion store.
Available products: {productList}
Customer question: {message}
Respond concisely in Vietnamese, max 150 words."

RECOMMENDATIONS:
"Given this fashion product: {productName} (category: {category}, price: {price}),
suggest 5 search keywords to find similar complementary fashion items.
Return as JSON array of strings only."

SEARCH_SUGGESTIONS:
"Suggest 5 Vietnamese fashion search queries that complete: '{query}'
Return as JSON array of strings only."
```

---

## Related Code Files

### Create
- `src/main/java/com/fashionshop/service/GeminiService.java`
- `src/main/java/com/fashionshop/controller/AiController.java`
- `src/main/java/com/fashionshop/dto/request/ChatRequest.java`
- `src/main/java/com/fashionshop/dto/response/ChatResponse.java`
- `src/main/java/com/fashionshop/dto/response/AiSuggestionsResponse.java`
- `src/main/java/com/fashionshop/util/GeminiPrompts.java`

### Modify
- `src/main/java/com/fashionshop/service/ProductService.java` — add `generateAiDescription(Long id)`
- `src/main/java/com/fashionshop/controller/ProductController.java` — add `POST /{id}/ai-description`
- `src/main/resources/application.yml` — add Gemini config

---

## Implementation Steps

1. **Add Gemini config to `application.yml`**:
   ```yaml
   gemini:
     api-key: ${GEMINI_API_KEY}
     model: gemini-1.5-flash
     max-tokens: 500
     timeout-seconds: 10
   ```

2. **Create `GeminiPrompts` utility class** with static String constants for all prompt templates. Use `String.format()` or simple `replace()` for variable substitution.

3. **Create `GeminiService`** (`@Service`):
   ```java
   @Service
   public class GeminiService {
     @Value("${gemini.api-key}") private String apiKey;
     @Value("${gemini.model}") private String modelName;

     public String generateContent(String prompt) {
       try {
         GenerativeModel model = new GenerativeModel(modelName, apiKey);
         GenerateContentResponse response = model.generateContent(prompt);
         return response.getCandidates().get(0)
           .getContent().getParts().get(0).getText();
       } catch (Exception e) {
         log.error("Gemini API error: {}", e.getMessage());
         return ""; // graceful fallback
       }
     }

     public List<String> generateJsonList(String prompt) {
       String raw = generateContent(prompt);
       // Parse JSON array from response; if parse fails, return empty list
       try {
         return objectMapper.readValue(raw, List.class);
       } catch (Exception e) {
         return List.of();
       }
     }
   }
   ```

4. **Add `generateAiDescription` to `ProductService`**:
   ```java
   public ProductResponse generateAiDescription(Long id) {
     Product product = findProductById(id);
     String prompt = GeminiPrompts.PRODUCT_DESCRIPTION
       .replace("{name}", product.getName())
       .replace("{category}", product.getCategory().getName())
       .replace("{price}", product.getPrice().toString())
       .replace("{existingDescription}", Optional.ofNullable(product.getDescription()).orElse(""));
     String description = geminiService.generateContent(prompt);
     if (!description.isBlank()) {
       product.setAiDescription(description);
       productRepository.save(product);
     }
     return mapToProductResponse(product);
   }
   ```

5. **Create `ChatRequest` DTO**:
   ```java
   public class ChatRequest {
     @NotBlank @Size(max = 500)
     private String message;
   }
   ```

6. **Create `AiController`**:
   ```java
   @RestController
   @RequestMapping("/api/ai")
   @RequiredArgsConstructor
   public class AiController {

     @PostMapping("/chat")
     public ResponseEntity<ApiResponse<ChatResponse>> chat(
         @Valid @RequestBody ChatRequest req) {
       // Fetch top 10 active products as context (name + price only, keep prompt short)
       List<String> productContext = productService.getTopProductsForContext();
       String prompt = GeminiPrompts.CHATBOT
         .replace("{productList}", String.join(", ", productContext))
         .replace("{message}", req.getMessage());
       String answer = geminiService.generateContent(prompt);
       return ResponseEntity.ok(ApiResponse.ok(new ChatResponse(answer)));
     }

     @PostMapping("/recommendations")
     public ResponseEntity<ApiResponse<AiSuggestionsResponse>> recommendations(
         @RequestParam Long productId) {
       Product p = productService.findById(productId);
       String prompt = GeminiPrompts.RECOMMENDATIONS
         .replace("{productName}", p.getName())
         .replace("{category}", p.getCategory().getName())
         .replace("{price}", p.getPrice().toString());
       List<String> keywords = geminiService.generateJsonList(prompt);
       return ResponseEntity.ok(ApiResponse.ok(new AiSuggestionsResponse(keywords)));
     }

     @PostMapping("/search-suggestions")
     public ResponseEntity<ApiResponse<AiSuggestionsResponse>> searchSuggestions(
         @RequestParam String q) {
       String prompt = GeminiPrompts.SEARCH_SUGGESTIONS.replace("{query}", q);
       List<String> suggestions = geminiService.generateJsonList(prompt);
       return ResponseEntity.ok(ApiResponse.ok(new AiSuggestionsResponse(suggestions)));
     }
   }
   ```

7. **Add `POST /{id}/ai-description` to `ProductController`**:
   ```java
   @PostMapping("/{id}/ai-description")
   @PreAuthorize("hasRole('ADMIN')")
   public ResponseEntity<ApiResponse<ProductResponse>> generateAiDescription(
       @PathVariable Long id) {
     return ResponseEntity.ok(ApiResponse.ok(productService.generateAiDescription(id)));
   }
   ```

8. **Add `getTopProductsForContext()` to `ProductService`** — returns `List<String>` like `["Áo thun trắng - 150,000đ", ...]`, max 10 products (to keep prompt size reasonable).

9. **Test** all 4 AI endpoints with Postman. Verify graceful degradation when API key is invalid (should return empty/fallback, not 500).

---

## Todo List

- [ ] Add Gemini config to `application.yml`
- [ ] Create `GeminiPrompts` constants class
- [ ] Implement `GeminiService` with error handling
- [ ] Add `generateAiDescription()` to `ProductService`
- [ ] Create `ChatRequest`, `ChatResponse`, `AiSuggestionsResponse` DTOs
- [ ] Implement `AiController` (3 endpoints)
- [ ] Add `POST /{id}/ai-description` to `ProductController`
- [ ] Add `getTopProductsForContext()` to `ProductService`
- [ ] Test chatbot endpoint
- [ ] Test AI description generation + verify saved to DB
- [ ] Test search suggestions + recommendations
- [ ] Verify graceful fallback on Gemini error

---

## Success Criteria

- `POST /api/products/{id}/ai-description` → 200; product's `aiDescription` field updated in DB
- `POST /api/ai/chat` with `{"message": "Tôi muốn mặc gì hôm nay?"}` → 200 with text response
- `POST /api/ai/search-suggestions?q=áo` → list of 5 search query strings
- `POST /api/ai/recommendations?productId=1` → list of 5 related keywords
- When `GEMINI_API_KEY` is missing/invalid → all endpoints return 200 with empty `data`, NOT 500

---

## Risk Assessment

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Gemini SDK version incompatibility with Java 21 | Medium | Test at project setup; fallback to raw HTTP call via `RestTemplate` if SDK fails |
| Gemini returns JSON with extra text wrapping the array | High | Strip markdown code blocks (` ```json ` prefix/suffix) before parsing |
| Rate limit exceeded (15 req/min on free tier) | Medium | Add TODO comment for Phase 7 rate limiting; for now 429 from Gemini is caught and returns empty |
| Long response time (>10s) blocking thread | Low | Set HTTP timeout in SDK config; add `@Async` if needed |
| Prompt injection via `message` field | Medium | Sanitize user input: strip control chars; limit to 500 chars |

---

## Security Considerations

- `POST /api/ai/chat` should be accessible to authenticated users only (optional for demo) — add `@PreAuthorize("isAuthenticated()")` or keep public with rate limiting
- `GEMINI_API_KEY` is an env var; never logged or exposed in responses
- User chat messages should not contain raw HTML — strip or encode before putting in prompt
- Gemini output is displayed as text only — no HTML rendering on frontend to prevent XSS

---

## Next Steps

- Phase 7 (Redis) can cache `getTopProductsForContext()` result (cache key: `"ai:product-context"`, TTL 5 min)
- Phase 7 adds rate limiting to `/api/ai/**` endpoints (simple in-memory token bucket)
