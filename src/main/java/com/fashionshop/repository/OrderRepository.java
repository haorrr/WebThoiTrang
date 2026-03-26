package com.fashionshop.repository;

import com.fashionshop.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.product", "coupon"})
    Optional<Order> findById(Long id);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findByUserIdAndStatus(Long userId, Order.Status status, Pageable pageable);

    Page<Order> findByStatus(Order.Status status, Pageable pageable);

    long countByStatus(Order.Status status);

    @Query("SELECT COALESCE(SUM(o.totalAmount - o.discountAmount), 0) FROM Order o WHERE o.status = 'DELIVERED'")
    BigDecimal sumRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount - o.discountAmount), 0) FROM Order o WHERE o.status = 'DELIVERED' AND o.createdAt >= :from AND o.createdAt < :to")
    BigDecimal sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT DATE(o.createdAt) as day, COUNT(o) as cnt, COALESCE(SUM(o.totalAmount - o.discountAmount), 0) as rev " +
           "FROM Order o WHERE o.status = 'DELIVERED' AND o.createdAt >= :from " +
           "GROUP BY DATE(o.createdAt) ORDER BY DATE(o.createdAt)")
    List<Object[]> revenueByDay(@Param("from") LocalDateTime from);

    long countByCreatedAtAfter(LocalDateTime after);
}
