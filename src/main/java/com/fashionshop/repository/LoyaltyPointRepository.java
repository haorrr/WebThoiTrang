package com.fashionshop.repository;

import com.fashionshop.entity.LoyaltyPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoyaltyPointRepository extends JpaRepository<LoyaltyPoint, Long> {

    Page<LoyaltyPoint> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(lp.points), 0) FROM LoyaltyPoint lp WHERE lp.user.id = :userId AND (lp.expiresAt IS NULL OR lp.expiresAt > CURRENT_TIMESTAMP)")
    int getTotalPoints(@Param("userId") Long userId);
}
