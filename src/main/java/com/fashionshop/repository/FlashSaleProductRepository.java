package com.fashionshop.repository;

import com.fashionshop.entity.FlashSaleProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FlashSaleProductRepository extends JpaRepository<FlashSaleProduct, Long> {

    @Query("""
        SELECT fsp FROM FlashSaleProduct fsp
        JOIN fsp.flashSale fs
        WHERE fsp.product.id = :productId
          AND fs.status = 'ACTIVE'
          AND fs.deletedAt IS NULL
        ORDER BY fs.startsAt DESC
        LIMIT 1
        """)
    Optional<FlashSaleProduct> findActiveByProductId(@Param("productId") Long productId);

    Optional<FlashSaleProduct> findByFlashSaleIdAndProductId(Long flashSaleId, Long productId);

    @Modifying
    @Query("UPDATE FlashSaleProduct fsp SET fsp.soldCount = fsp.soldCount + 1 WHERE fsp.id = :id")
    void incrementSoldCount(@Param("id") Long id);
}
