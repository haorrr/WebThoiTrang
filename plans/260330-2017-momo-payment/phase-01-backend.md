# Phase 01 — Backend: MoMo Service & Controller

**Status:** 🔲 pending
**Dependencies:** None

## Context

- Order entity: `src/main/java/com/fashionshop/entity/Order.java`
- Pattern to follow: `CloudinaryService.java` (env vars, @RequiredArgsConstructor, try-catch)
- SecurityConfig: `src/main/java/com/fashionshop/security/SecurityConfig.java`
- Migrations: `src/main/resources/db/migration/` (latest: V8__review_images.sql)
- Existing `paymentMethod` field on Order is a plain String ("COD", "BANK_TRANSFER")

## Requirements

1. Store MoMo transaction data on Order
2. Create MoMoService to sign requests and call MoMo API
3. Create MoMoController with 3 endpoints:
   - `POST /api/payment/momo/create` — authenticated, creates payment
   - `POST /api/payment/momo/ipn` — PUBLIC, receives MoMo webhook
   - `GET /api/payment/momo/return` — PUBLIC, handles browser redirect
4. IPN endpoint MUST be public (no JWT) — MoMo calls it server-to-server

## Implementation Steps

### Step 1 — Flyway Migration `V9__payment_fields.sql`

```sql
ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS payment_status   VARCHAR(30)  DEFAULT 'N_A',
  ADD COLUMN IF NOT EXISTS momo_transaction_id VARCHAR(100),
  ADD COLUMN IF NOT EXISTS payment_url      TEXT;
```

**PaymentStatus values:** `N_A` (COD/BANK_TRANSFER), `PENDING_PAYMENT`, `PAID`, `FAILED`

---

### Step 2 — Update `Order.java`

Add three fields after `paymentMethod`:

```java
@Enumerated(EnumType.STRING)
@Column(name = "payment_status")
@Builder.Default
private PaymentStatus paymentStatus = PaymentStatus.N_A;

@Column(name = "momo_transaction_id")
private String momoTransactionId;

@Column(name = "payment_url")
@Lob
private String paymentUrl;

// Inner enum
public enum PaymentStatus { N_A, PENDING_PAYMENT, PAID, FAILED }
```

---

### Step 3 — application.yml (add momo block)

```yaml
app:
  momo:
    partner-code: ${MOMO_PARTNER_CODE:MOMO}
    access-key: ${MOMO_ACCESS_KEY:F8BBA842ECF85}
    secret-key: ${MOMO_SECRET_KEY:K951B6PE1waDMi640xX08PD3vg6EkVlz}
    endpoint: ${MOMO_ENDPOINT:https://test-payment.momo.vn}
    redirect-url: ${MOMO_REDIRECT_URL:http://localhost:8080/api/payment/momo/return}
    ipn-url: ${MOMO_IPN_URL:http://localhost:8080/api/payment/momo/ipn}
```

> **Note on IPN URL:** In local dev, MoMo cannot call localhost. Use ngrok:
> `ngrok http 8080` → set `MOMO_IPN_URL=https://xxxx.ngrok-free.app/api/payment/momo/ipn`

---

### Step 4 — Create `MoMoConfig.java` (properties binding)

**Path:** `src/main/java/com/fashionshop/config/MoMoConfig.java`

```java
@Configuration
@ConfigurationProperties(prefix = "app.momo")
@Data
public class MoMoConfig {
    private String partnerCode;
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String redirectUrl;
    private String ipnUrl;
}
```

---

### Step 5 — Create `MoMoService.java`

**Path:** `src/main/java/com/fashionshop/service/MoMoService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MoMoService {

    private final MoMoConfig config;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    /** Create MoMo payment, return payUrl */
    public String createPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        String requestId = config.getPartnerCode() + System.currentTimeMillis();
        String momoOrderId = config.getPartnerCode() + "_" + orderId + "_" + System.currentTimeMillis();
        String orderInfo = "Thanh toán đơn hàng #" + orderId;
        long amount = order.getFinalAmount().longValue(); // use finalAmount (after discount)
        String extraData = "";

        // Build signature raw string (MUST follow this exact order)
        String rawHash = "accessKey=" + config.getAccessKey()
            + "&amount=" + amount
            + "&extraData=" + extraData
            + "&ipnUrl=" + config.getIpnUrl()
            + "&orderId=" + momoOrderId
            + "&orderInfo=" + orderInfo
            + "&partnerCode=" + config.getPartnerCode()
            + "&redirectUrl=" + config.getRedirectUrl()
            + "&requestId=" + requestId
            + "&requestType=payUrl";

        String signature = hmacSHA256(rawHash, config.getSecretKey());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", config.getPartnerCode());
        body.put("partnerName", "MAISON Fashion");
        body.put("storeId", "MaisonStore");
        body.put("requestId", requestId);
        body.put("amount", amount);
        body.put("orderId", momoOrderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", config.getRedirectUrl() + "?orderId=" + orderId);
        body.put("ipnUrl", config.getIpnUrl());
        body.put("requestType", "payUrl");
        body.put("extraData", extraData);
        body.put("lang", "vi");
        body.put("signature", signature);

        try {
            HttpClient client = HttpClient.newHttpClient();
            String json = objectMapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(config.getEndpoint() + "/v2/gateway/api/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> result = objectMapper.readValue(resp.body(), Map.class);

            int resultCode = (Integer) result.get("resultCode");
            if (resultCode != 0) {
                throw new BadRequestException("MoMo tạo thanh toán thất bại: " + result.get("message"));
            }

            String payUrl = (String) result.get("payUrl");

            // Update order
            order.setPaymentStatus(Order.PaymentStatus.PENDING_PAYMENT);
            order.setPaymentUrl(payUrl);
            orderRepository.save(order);

            return payUrl;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("MoMo createPayment error", e);
            throw new BadRequestException("Không thể kết nối MoMo. Vui lòng thử lại.");
        }
    }

    /** Handle IPN from MoMo — verify signature + update order status */
    public void handleIPN(Map<String, Object> payload) {
        // 1. Reconstruct signature
        String partnerCode  = (String) payload.get("partnerCode");
        String orderId      = (String) payload.get("orderId");   // e.g. "MOMO_12_1234567890"
        String requestId    = (String) payload.get("requestId");
        long   amount       = ((Number) payload.get("amount")).longValue();
        String orderInfo    = (String) payload.get("orderInfo");
        String orderType    = (String) payload.getOrDefault("orderType", "");
        long   transId      = ((Number) payload.get("transId")).longValue();
        int    resultCode   = (Integer) payload.get("resultCode");
        String message      = (String) payload.get("message");
        String payType      = (String) payload.getOrDefault("payType", "");
        String extraData    = (String) payload.getOrDefault("extraData", "");
        String receivedSig  = (String) payload.get("signature");

        String rawHash = "accessKey=" + config.getAccessKey()
            + "&amount=" + amount
            + "&extraData=" + extraData
            + "&message=" + message
            + "&orderId=" + orderId
            + "&orderInfo=" + orderInfo
            + "&orderType=" + orderType
            + "&partnerCode=" + partnerCode
            + "&payType=" + payType
            + "&requestId=" + requestId
            + "&responseTime=" + payload.get("responseTime")
            + "&resultCode=" + resultCode
            + "&transId=" + transId;

        String expectedSig = hmacSHA256(rawHash, config.getSecretKey());
        if (!expectedSig.equals(receivedSig)) {
            log.warn("MoMo IPN signature mismatch. Ignoring.");
            return;
        }

        // 2. Parse internal orderId (format: MOMO_{orderId}_{timestamp})
        String[] parts = orderId.split("_");
        if (parts.length < 2) return;
        Long internalOrderId;
        try { internalOrderId = Long.parseLong(parts[1]); }
        catch (NumberFormatException e) { return; }

        Order order = orderRepository.findById(internalOrderId).orElse(null);
        if (order == null) return;

        // 3. Idempotency — skip if already processed
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) return;

        if (resultCode == 0) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setMomoTransactionId(String.valueOf(transId));
            order.setStatus(Order.Status.CONFIRMED); // auto-confirm on payment
        } else {
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            order.setStatus(Order.Status.CANCELLED);
        }
        orderRepository.save(order);
        log.info("MoMo IPN processed: order={}, resultCode={}, transId={}", internalOrderId, resultCode, transId);
    }

    /** HMAC-SHA256 */
    public static String hmacSHA256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC error", e);
        }
    }
}
```

---

### Step 6 — Create `MoMoController.java`

**Path:** `src/main/java/com/fashionshop/controller/MoMoController.java`

```java
@RestController
@RequestMapping("/api/payment/momo")
@RequiredArgsConstructor
@Tag(name = "MoMo Payment")
public class MoMoController {

    private final MoMoService moMoService;
    private final OrderRepository orderRepository;

    /** Authenticated: init payment for given orderId */
    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPayment(
            @RequestBody Map<String, Long> body,
            Authentication auth) {
        Long orderId = body.get("orderId");
        // Security: verify order belongs to this user
        Long userId = getCurrentUserId(auth);
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getPaymentMethod().equals("MOMO")) {
            throw new BadRequestException("Order is not MOMO payment");
        }
        String payUrl = moMoService.createPayment(orderId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("payUrl", payUrl)));
    }

    /** PUBLIC: MoMo calls this after payment */
    @PostMapping("/ipn")
    public ResponseEntity<Void> handleIPN(@RequestBody Map<String, Object> payload) {
        moMoService.handleIPN(payload);
        return ResponseEntity.noContent().build(); // HTTP 204
    }

    /** PUBLIC: Browser redirect after payment (return URL) */
    @GetMapping("/return")
    public ResponseEntity<Void> handleReturn(
            @RequestParam String orderId,
            @RequestParam(required = false, defaultValue = "") String resultCode,
            HttpServletResponse response) throws IOException {
        String frontendUrl;
        if ("0".equals(resultCode)) {
            frontendUrl = "/payment-result.html?status=success&orderId=" + orderId;
        } else {
            frontendUrl = "/payment-result.html?status=failed&orderId=" + orderId;
        }
        response.sendRedirect(frontendUrl);
        return null;
    }

    private Long getCurrentUserId(Authentication auth) {
        // Same helper as other controllers
        return userRepository.findByEmail(auth.getName())
            .orElseThrow().getId();
    }
}
```

> Note: also inject `UserRepository` and use same `getCurrentUserId` pattern as OrderController.

---

### Step 7 — SecurityConfig: permit IPN + return endpoints

In `SecurityConfig.java`, add to the public matcher section:

```java
.requestMatchers("/api/payment/momo/ipn", "/api/payment/momo/return").permitAll()
```

---

### Step 8 — OrderRepository: add helper method

```java
Optional<Order> findByIdAndUserId(Long id, Long userId);
```
(if not already present — check OrderRepository.java first)

---

## Required Imports (MoMoService)

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
```

## Success Criteria

- [ ] V9 migration runs without error
- [ ] `POST /api/payment/momo/create` returns valid `payUrl`
- [ ] Redirecting to payUrl opens MoMo sandbox payment page
- [ ] After paying, IPN arrives and order status changes to `CONFIRMED` + `paymentStatus=PAID`
- [ ] Failed payment cancels order
- [ ] Duplicate IPN calls are ignored (idempotency)
