package com.fashionshop.service;

import com.fashionshop.dto.response.LoyaltyPointResponse;
import com.fashionshop.entity.LoyaltyPoint;
import com.fashionshop.entity.Order;
import com.fashionshop.entity.SystemConfig;
import com.fashionshop.entity.User;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.LoyaltyPointRepository;
import com.fashionshop.repository.SystemConfigRepository;
import com.fashionshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyPointRepository loyaltyPointRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final UserRepository userRepository;

    private int getSpendPerPoint() {
        return Integer.parseInt(
            systemConfigRepository.findById("loyalty.spend_per_point")
                .map(SystemConfig::getValue).orElse("10000"));
    }

    private int getVndPerPoint() {
        return Integer.parseInt(
            systemConfigRepository.findById("loyalty.vnd_per_point")
                .map(SystemConfig::getValue).orElse("1000"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long userId) {
        int total = loyaltyPointRepository.getTotalPoints(userId);
        int vndPerPoint = getVndPerPoint();
        Page<LoyaltyPointResponse> history = loyaltyPointRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 20))
                .map(LoyaltyPointResponse::from);
        int totalEarned = loyaltyPointRepository.getTotalByType(userId, "EARNED");
        int totalRedeemed = Math.abs(loyaltyPointRepository.getTotalByType(userId, "REDEEMED"));
        return Map.of(
            "totalPoints", total,
            "cashValue", (long) total * vndPerPoint,
            "totalEarned", totalEarned,
            "totalRedeemed", totalRedeemed,
            "history", history.getContent()
        );
    }

    @Transactional
    public void earnFromOrder(User user, Order order) {
        int spendPerPoint = getSpendPerPoint();
        int points = order.getTotalAmount()
                .subtract(order.getDiscountAmount())
                .divide(BigDecimal.valueOf(spendPerPoint), 0, RoundingMode.DOWN)
                .intValue();
        if (points <= 0) return;

        loyaltyPointRepository.save(LoyaltyPoint.builder()
                .user(user)
                .order(order)
                .points(points)
                .type(LoyaltyPoint.Type.EARNED)
                .description("Tích điểm từ đơn hàng #" + order.getId())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build());
    }

    @Transactional
    public BigDecimal redeem(Long userId, int points) {
        int available = loyaltyPointRepository.getTotalPoints(userId);
        if (available < points) {
            throw new BadRequestException("Không đủ điểm. Hiện có: " + available);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        loyaltyPointRepository.save(LoyaltyPoint.builder()
                .user(user)
                .points(-points)
                .type(LoyaltyPoint.Type.REDEEMED)
                .description("Đổi " + points + " điểm")
                .build());

        int vndPerPoint = getVndPerPoint();
        return BigDecimal.valueOf((long) points * vndPerPoint);
    }

    @Transactional
    public void awardReferralBonus(User referrer, User newUser) {
        int bonusPoints = Integer.parseInt(
            systemConfigRepository.findById("loyalty.referral_bonus")
                .map(SystemConfig::getValue).orElse("50"));
        loyaltyPointRepository.save(LoyaltyPoint.builder()
                .user(referrer)
                .points(bonusPoints)
                .type(LoyaltyPoint.Type.REFERRAL_BONUS)
                .description("Thưởng giới thiệu: " + newUser.getName())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build());
    }

    // ─── Admin methods ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUsersWithPoints(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return loyaltyPointRepository.findUsersWithPoints(pageable).stream()
                .map(row -> Map.<String, Object>of(
                    "userId",      ((Number) row[0]).longValue(),
                    "name",        (String) row[1],
                    "email",       (String) row[2],
                    "totalPoints", ((Number) row[3]).intValue()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getConfig() {
        return Map.of(
            "spendPerPoint",  getSpendPerPoint(),
            "vndPerPoint",    getVndPerPoint(),
            "referralBonus",  Integer.parseInt(
                systemConfigRepository.findById("loyalty.referral_bonus")
                    .map(SystemConfig::getValue).orElse("50"))
        );
    }

    @Transactional
    public void updateConfig(int spendPerPoint, int vndPerPoint, int referralBonus) {
        saveConfig("loyalty.spend_per_point", String.valueOf(spendPerPoint));
        saveConfig("loyalty.vnd_per_point",   String.valueOf(vndPerPoint));
        saveConfig("loyalty.referral_bonus",  String.valueOf(referralBonus));
    }

    @Transactional
    public void adminAdjustPoints(Long userId, int points, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        loyaltyPointRepository.save(LoyaltyPoint.builder()
                .user(user)
                .points(points)
                .type(LoyaltyPoint.Type.EARNED)
                .description(reason != null ? reason : "Điều chỉnh thủ công bởi admin")
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build());
    }

    private void saveConfig(String key, String value) {
        SystemConfig config = systemConfigRepository.findById(key)
                .orElse(new SystemConfig(key, value, null));
        config.setValue(value);
        systemConfigRepository.save(config);
    }
}
