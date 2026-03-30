package com.fashionshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "flash_sale_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flash_sale_id", nullable = false)
    private FlashSale flashSale;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Product product;

    @Column(name = "stock_limit")
    private Integer stockLimit;

    @Column(name = "sold_count", nullable = false)
    @Builder.Default
    private Integer soldCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
