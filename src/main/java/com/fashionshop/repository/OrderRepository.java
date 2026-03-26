package com.fashionshop.repository;

import com.fashionshop.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.product", "coupon"})
    Optional<Order> findById(Long id);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findByUserIdAndStatus(Long userId, Order.Status status, Pageable pageable);

    Page<Order> findByStatus(Order.Status status, Pageable pageable);
}
