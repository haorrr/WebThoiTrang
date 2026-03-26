package com.fashionshop.service;

import com.fashionshop.dto.request.CouponRequest;
import com.fashionshop.dto.response.CouponResponse;
import com.fashionshop.dto.response.CouponValidationResponse;
import com.fashionshop.entity.Coupon;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponValidationResponse validateCoupon(String code, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElse(null);

        if (coupon == null || !coupon.isValid(orderAmount)) {
            String reason = "Invalid or expired coupon";
            if (coupon != null && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
                reason = "Minimum order amount is " + coupon.getMinOrderAmount();
            }
            return CouponValidationResponse.builder()
                    .valid(false)
                    .code(code)
                    .message(reason)
                    .build();
        }

        BigDecimal discount = coupon.calculateDiscount(orderAmount);
        return CouponValidationResponse.builder()
                .valid(true)
                .code(code)
                .discountType(coupon.getDiscountType().name())
                .discountValue(coupon.getDiscountValue())
                .discountAmount(discount)
                .finalAmount(orderAmount.subtract(discount))
                .message("Coupon applied successfully")
                .build();
    }

    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(CouponResponse::from).toList();
    }

    public CouponResponse getCouponById(Long id) {
        return CouponResponse.from(findCoupon(id));
    }

    @Transactional
    public CouponResponse createCoupon(CouponRequest req) {
        if (couponRepository.existsByCode(req.getCode().toUpperCase())) {
            throw new BadRequestException("Coupon code already exists: " + req.getCode());
        }

        Coupon coupon = Coupon.builder()
                .code(req.getCode().toUpperCase())
                .discountType(Coupon.DiscountType.valueOf(req.getDiscountType()))
                .discountValue(req.getDiscountValue())
                .minOrderAmount(req.getMinOrderAmount())
                .maxUses(req.getMaxUses())
                .expiresAt(req.getExpiresAt())
                .status(Coupon.Status.ACTIVE)
                .build();

        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse updateCoupon(Long id, CouponRequest req) {
        Coupon coupon = findCoupon(id);
        coupon.setDiscountType(Coupon.DiscountType.valueOf(req.getDiscountType()));
        coupon.setDiscountValue(req.getDiscountValue());
        coupon.setMinOrderAmount(req.getMinOrderAmount());
        coupon.setMaxUses(req.getMaxUses());
        coupon.setExpiresAt(req.getExpiresAt());
        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = findCoupon(id);
        coupon.softDelete();
        couponRepository.save(coupon);
    }

    @Transactional
    public CouponResponse toggleStatus(Long id) {
        Coupon coupon = findCoupon(id);
        coupon.setStatus(coupon.getStatus() == Coupon.Status.ACTIVE
                ? Coupon.Status.INACTIVE : Coupon.Status.ACTIVE);
        return CouponResponse.from(couponRepository.save(coupon));
    }

    private Coupon findCoupon(Long id) {
        return couponRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", id));
    }
}
