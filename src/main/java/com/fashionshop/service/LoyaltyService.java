package com.fashionshop.service;

import com.fashionshop.dto.response.LoyaltyPointResponse;
import com.fashionshop.entity.LoyaltyPoint;
import com.fashionshop.entity.Order;
import com.fashionshop.entity.User;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.LoyaltyPointRepository;
import com.fashionshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyPointRepository loyaltyPointRepository;
    private final UserRepository userRepository;

    // 1 point per 10,000 VND spent
    private static final int POINTS_PER_10K = 1;
    // 1 point = 1,000 VND discount
    private static final int VND_PER_POINT = 1000;

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long userId) {
        int total = loyaltyPointRepository.getTotalPoints(userId);
        Page<LoyaltyPointResponse> history = loyaltyPointRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 20))
                .map(LoyaltyPointResponse::from);
        return Map.of("totalPoints", total, "cashValue", total * VND_PER_POINT, "history", history.getContent());
    }

    @Transactional
    public void earnFromOrder(User user, Order order) {
        int points = order.getTotalAmount()
                .subtract(order.getDiscountAmount())
                .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(POINTS_PER_10K))
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
            throw new BadRequestException("Insufficient points. Available: " + available);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        loyaltyPointRepository.save(LoyaltyPoint.builder()
                .user(user)
                .points(-points)
                .type(LoyaltyPoint.Type.REDEEMED)
                .description("Đổi " + points + " điểm")
                .build());

        return BigDecimal.valueOf((long) points * VND_PER_POINT);
    }

    @Transactional
    public void awardReferralBonus(User referrer, User newUser) {
        int bonusPoints = 50;
        loyaltyPointRepository.save(LoyaltyPoint.builder()
                .user(referrer)
                .points(bonusPoints)
                .type(LoyaltyPoint.Type.REFERRAL_BONUS)
                .description("Thưởng giới thiệu: " + newUser.getName())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build());
    }
}
