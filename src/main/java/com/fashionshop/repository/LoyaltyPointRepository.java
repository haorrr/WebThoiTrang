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

    @Query("SELECT COALESCE(SUM(lp.points), 0) FROM LoyaltyPoint lp WHERE lp.user.id = :userId AND lp.type = :type")
    int getTotalByType(@Param("userId") Long userId, @Param("type") String type);

    @Query(value = """
            SELECT u.id, u.name, u.email,
                   COALESCE(SUM(CASE WHEN lp.expires_at IS NULL OR lp.expires_at > NOW() THEN lp.points ELSE 0 END), 0) AS total_points
            FROM users u
            LEFT JOIN loyalty_points lp ON lp.user_id = u.id
            WHERE u.deleted_at IS NULL
            GROUP BY u.id, u.name, u.email
            ORDER BY total_points DESC
            """, nativeQuery = true)
    Page<Object[]> findUsersWithPoints(Pageable pageable);
}
