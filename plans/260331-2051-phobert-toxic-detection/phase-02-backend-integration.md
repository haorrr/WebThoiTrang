# Phase 02 — Spring Boot Backend Integration

## 2.1 `pom.xml` — Add WebFlux (WebClient)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

> Spring Boot 3.2 supports spring-web + spring-webflux mixed mode (servlet + reactive HTTP client).

---

## 2.2 Flyway Migration: `V11__toxic_detection.sql`

```sql
-- V11: add toxic_score column + system_config keys for PhoBERT detection

ALTER TABLE reviews
  ADD COLUMN IF NOT EXISTS toxic_score DECIMAL(5,4) DEFAULT NULL;

INSERT INTO system_config (key, value, description) VALUES
  ('toxic_detection.enabled',   'false', 'Enable PhoBERT toxic review auto-detection'),
  ('toxic_detection.threshold', '0.70',  'Score >= threshold → keep PENDING for admin review')
ON CONFLICT (key) DO NOTHING;
```

---

## 2.3 `Review.java` — add field

```java
@Column(name = "toxic_score", precision = 5, scale = 4)
private BigDecimal toxicScore;
```

---

## 2.4 `ReviewResponse.java` — expose field

```java
private BigDecimal toxicScore;
```

In `from(Review r)` builder:
```java
.toxicScore(r.getToxicScore())
```

---

## 2.5 New: `config/ToxicDetectorConfig.java`

```java
@Configuration
public class ToxicDetectorConfig {

    @Value("${app.toxic-detector.url:http://toxic-detector:8000}")
    private String detectorUrl;

    @Bean
    public WebClient toxicDetectorWebClient() {
        return WebClient.builder()
            .baseUrl(detectorUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}
```

Add to `application.yml` under `app:`:
```yaml
  toxic-detector:
    url: ${TOXIC_DETECTOR_URL:http://toxic-detector:8000}
```

---

## 2.6 New: `service/ToxicDetectionService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ToxicDetectionService {

    private final WebClient toxicDetectorWebClient;
    private final SystemConfigRepository configRepo;
    private final ReviewRepository reviewRepository;

    @Cacheable(value = "system_config", key = "'toxic_detection.enabled'")
    public boolean isEnabled() {
        return configRepo.findById("toxic_detection.enabled")
            .map(c -> "true".equalsIgnoreCase(c.getValue()))
            .orElse(false);
    }

    @Cacheable(value = "system_config", key = "'toxic_detection.threshold'")
    public double getThreshold() {
        return configRepo.findById("toxic_detection.threshold")
            .map(c -> Double.parseDouble(c.getValue()))
            .orElse(0.70);
    }

    public void analyzeAsync(Long reviewId, String text) {
        if (!isEnabled()) { autoApprove(reviewId); return; }

        toxicDetectorWebClient.post()
            .uri("/classify")
            .bodyValue(Map.of("text", text))
            .retrieve()
            .bodyToMono(ToxicResult.class)
            .timeout(Duration.ofSeconds(5))
            .doOnSuccess(r  -> applyResult(reviewId, r))
            .doOnError(e -> {
                log.warn("Toxic detector unavailable for review {}: {}", reviewId, e.getMessage());
                autoApprove(reviewId);
            })
            .subscribe();
    }

    @Transactional
    void applyResult(Long reviewId, ToxicResult result) {
        reviewRepository.findById(reviewId).ifPresent(r -> {
            r.setToxicScore(BigDecimal.valueOf(result.score()));
            double threshold = getThreshold();
            if (!result.toxic() || result.score() < threshold) {
                r.setStatus(Review.Status.APPROVED);
            }
            // if toxic → keep PENDING, admin must approve manually
            reviewRepository.save(r);
        });
    }

    @Transactional
    void autoApprove(Long reviewId) {
        reviewRepository.findById(reviewId).ifPresent(r -> {
            r.setStatus(Review.Status.APPROVED);
            reviewRepository.save(r);
        });
    }

    public record ToxicResult(boolean toxic, double score, String label) {}
}
```

---

## 2.7 `ReviewService.java` — trigger after save

In `createReview()`, replace `return ReviewResponse.from(reviewRepository.save(review))` with:

```java
Review saved = reviewRepository.save(review);
String comment = req.getComment();
if (comment != null && !comment.isBlank()) {
    toxicDetectionService.analyzeAsync(saved.getId(), comment);
} else {
    // No text to analyze — auto-approve immediately
    saved.setStatus(Review.Status.APPROVED);
    reviewRepository.save(saved);
}
return ReviewResponse.from(saved);
```

Same pattern in `updateReview()` after saving — re-trigger analysis (comment changed = re-score).

---

## 2.8 `RedisConfig.java` — add `system_config` named cache

In `cacheManager` bean, add to builder:
```java
.withCacheConfiguration("system_config",
    config.entryTtl(Duration.ofSeconds(60)))
```

---

## 2.9 Cache eviction on config update

In `SystemConfigController.java`, annotate the update method:
```java
@CacheEvict(value = "system_config", allEntries = true)
@PutMapping("/{key}")
public ResponseEntity<...> update(@PathVariable String key, ...) { ... }
```

---

## Test Checklist

- [ ] `toxic_detection.enabled = false` → review immediately APPROVED, no HTTP call made
- [ ] `toxic_detection.enabled = true` + clean text → APPROVED, `toxic_score` populated
- [ ] `toxic_detection.enabled = true` + toxic text → PENDING, `toxic_score >= 0.70`
- [ ] toxic-detector container stopped → review APPROVED after ~5s (check logs for WARN)
- [ ] `updateReview()` → re-triggers analysis, old score overwritten
- [ ] Cache: toggle feature → verify 60s TTL via Redis CLI `ttl system_config::toxic_detection.enabled`
