package com.fashionshop.repository;

import com.fashionshop.entity.FlashSale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface FlashSaleRepository extends JpaRepository<FlashSale, Long> {

    List<FlashSale> findByStatus(FlashSale.Status status);

    Page<FlashSale> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT fs FROM FlashSale fs WHERE fs.status = 'SCHEDULED' AND fs.startsAt <= :now AND fs.deletedAt IS NULL")
    List<FlashSale> findScheduledToActivate(LocalDateTime now);

    @Query("SELECT fs FROM FlashSale fs WHERE fs.status = 'ACTIVE' AND fs.endsAt <= :now AND fs.deletedAt IS NULL")
    List<FlashSale> findActiveToEnd(LocalDateTime now);
}
