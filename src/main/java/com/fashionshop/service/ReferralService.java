package com.fashionshop.service;

import com.fashionshop.entity.User;
import com.fashionshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private final UserRepository userRepository;
    private final LoyaltyService loyaltyService;

    @Transactional
    public String getOrGenerateCode(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.fashionshop.exception.ResourceNotFoundException("User", userId));
        if (user.getReferralCode() == null) {
            String code = generateUniqueCode();
            user.setReferralCode(code);
            userRepository.save(user);
        }
        return user.getReferralCode();
    }

    @Transactional
    public void applyReferral(User newUser, String referralCode) {
        if (referralCode == null || referralCode.isBlank()) return;
        Optional<User> referrer = userRepository.findByReferralCode(referralCode.toUpperCase());
        referrer.ifPresent(r -> {
            if (!r.getId().equals(newUser.getId())) {
                newUser.setReferredBy(r);
                loyaltyService.awardReferralBonus(r, newUser);
            }
        });
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (userRepository.existsByReferralCode(code));
        return code;
    }
}
