package com.fashionshop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashionshop.config.MoMoConfig;
import com.fashionshop.entity.Order;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoMoService {

    private final MoMoConfig moMoConfig;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public String createPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Compute final amount (totalAmount - discountAmount - pointsDiscountAmount)
        BigDecimal finalAmount = order.getTotalAmount()
                .subtract(order.getDiscountAmount())
                .subtract(order.getPointsDiscountAmount());
        long amount = finalAmount.longValue();

        String requestId   = moMoConfig.getPartnerCode() + System.currentTimeMillis();
        String momoOrderId = moMoConfig.getPartnerCode() + "-" + orderId + "-" + System.currentTimeMillis();
        String orderInfo   = "Thanh toan don hang #" + orderId;
        String extraData   = "";
        String redirectUrl = moMoConfig.getRedirectUrl() + "?orderId=" + orderId;

        // Signature — alphabetical key order (MoMo v3 captureWallet spec)
        String rawHash = "accessKey="   + moMoConfig.getAccessKey()
                + "&amount="      + amount
                + "&extraData="   + extraData
                + "&ipnUrl="      + moMoConfig.getIpnUrl()
                + "&orderId="     + momoOrderId
                + "&orderInfo="   + orderInfo
                + "&partnerCode=" + moMoConfig.getPartnerCode()
                + "&redirectUrl=" + redirectUrl
                + "&requestId="   + requestId
                + "&requestType=captureWallet";

        String signature = hmacSHA256(rawHash, moMoConfig.getSecretKey());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", moMoConfig.getPartnerCode());
        body.put("storeName",   "MAISON Fashion");
        body.put("storeId",     "MaisonStore");
        body.put("requestId",   requestId);
        body.put("amount",      amount);
        body.put("orderId",     momoOrderId);
        body.put("orderInfo",   orderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl",      moMoConfig.getIpnUrl());
        body.put("requestType", "captureWallet");
        body.put("extraData",   extraData);
        body.put("lang",        "vi");
        body.put("signature",   signature);

        try {
            HttpClient client = HttpClient.newHttpClient();
            String json = objectMapper.writeValueAsString(body);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(moMoConfig.getEndpoint() + "/v2/gateway/api/create"))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("MoMo create response: {}", resp.body());

            Map<?, ?> result = objectMapper.readValue(resp.body(), Map.class);
            int resultCode = ((Number) result.get("resultCode")).intValue();

            if (resultCode != 0) {
                throw new BadRequestException("MoMo tao thanh toan that bai: " + result.get("message"));
            }

            String payUrl = (String) result.get("payUrl");
            order.setPaymentStatus(Order.PaymentStatus.PENDING_PAYMENT);
            order.setPaymentUrl(payUrl);
            orderRepository.save(order);

            return payUrl;

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("MoMo createPayment error for order {}", orderId, e);
            throw new BadRequestException("Khong the ket noi MoMo. Vui long thu lai.");
        }
    }

    @Transactional
    public void handleIPN(Map<String, Object> payload) {
        String partnerCode  = (String) payload.get("partnerCode");
        String orderId      = (String) payload.get("orderId");
        String requestId    = (String) payload.get("requestId");
        long   amount       = ((Number) payload.get("amount")).longValue();
        String orderInfo    = (String) payload.getOrDefault("orderInfo", "");
        String orderType    = (String) payload.getOrDefault("orderType", "momo_wallet");
        long   transId      = ((Number) payload.get("transId")).longValue();
        int    resultCode   = ((Number) payload.get("resultCode")).intValue();
        String message      = (String) payload.getOrDefault("message", "");
        String payType      = (String) payload.getOrDefault("payType", "");
        String extraData    = (String) payload.getOrDefault("extraData", "");
        Object responseTime = payload.get("responseTime");
        String receivedSig  = (String) payload.get("signature");

        // Verify HMAC signature
        String rawHash = "accessKey="    + moMoConfig.getAccessKey()
                + "&amount="      + amount
                + "&extraData="   + extraData
                + "&message="     + message
                + "&orderId="     + orderId
                + "&orderInfo="   + orderInfo
                + "&orderType="   + orderType
                + "&partnerCode=" + partnerCode
                + "&payType="     + payType
                + "&requestId="   + requestId
                + "&responseTime=" + responseTime
                + "&resultCode="  + resultCode
                + "&transId="     + transId;

        String expectedSig = hmacSHA256(rawHash, moMoConfig.getSecretKey());
        if (!expectedSig.equals(receivedSig)) {
            log.warn("MoMo IPN: signature mismatch for orderId={}", orderId);
            return;
        }

        // Parse internal order ID from momoOrderId format: PARTNERCODE-orderId-timestamp
        String[] parts = orderId.split("-");
        if (parts.length < 2) {
            log.warn("MoMo IPN: cannot parse orderId={}", orderId);
            return;
        }
        Long internalOrderId;
        try {
            internalOrderId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            log.warn("MoMo IPN: invalid orderId format={}", orderId);
            return;
        }

        Order order = orderRepository.findById(internalOrderId).orElse(null);
        if (order == null) {
            log.warn("MoMo IPN: order {} not found", internalOrderId);
            return;
        }

        // Idempotency — skip if already processed
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID
                || order.getPaymentStatus() == Order.PaymentStatus.FAILED) {
            log.info("MoMo IPN: order {} already processed, skipping", internalOrderId);
            return;
        }

        if (resultCode == 0) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setMomoTransactionId(String.valueOf(transId));
            order.setStatus(Order.Status.CONFIRMED);
            log.info("MoMo IPN: order {} PAID, transId={}", internalOrderId, transId);
        } else {
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            order.setStatus(Order.Status.CANCELLED);
            log.info("MoMo IPN: order {} FAILED, resultCode={}", internalOrderId, resultCode);
        }
        orderRepository.save(order);
    }

    public static String hmacSHA256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 error", e);
        }
    }
}
