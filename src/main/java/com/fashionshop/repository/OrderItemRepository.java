package com.fashionshop.repository;

import com.fashionshop.entity.OrderItem;
import com.fashionshop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("""
        SELECT COUNT(oi) FROM OrderItem oi
        WHERE oi.order.user.id = :userId
          AND oi.product.id = :productId
          AND oi.order.status = :status
        """)
    Long countByUserAndProductAndOrderStatus(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("status") Order.Status status);
}
