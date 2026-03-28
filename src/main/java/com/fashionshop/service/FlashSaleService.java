package com.fashionshop.service;

import com.fashionshop.dto.request.FlashSaleRequest;
import com.fashionshop.dto.response.FlashSaleResponse;
import com.fashionshop.entity.FlashSale;
import com.fashionshop.entity.FlashSaleProduct;
import com.fashionshop.entity.Product;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.FlashSaleProductRepository;
import com.fashionshop.repository.FlashSaleRepository;
import com.fashionshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlashSaleService {

    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleProductRepository flashSaleProductRepository;
    private final ProductRepository productRepository;

    @Cacheable(value = "activeSales")
    @Transactional(readOnly = true)
    public List<FlashSaleResponse> getActiveSales() {
        return flashSaleRepository.findByStatus(FlashSale.Status.ACTIVE).stream()
                .map(FlashSaleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FlashSaleResponse getById(Long id) {
        FlashSale fs = flashSaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FlashSale", id));
        return FlashSaleResponse.from(fs);
    }

    @Transactional(readOnly = true)
    public Page<FlashSaleResponse> getAll(int page, int size) {
        return flashSaleRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(FlashSaleResponse::from);
    }

    @CacheEvict(value = "activeSales", allEntries = true)
    @Transactional
    public FlashSaleResponse create(FlashSaleRequest req) {
        if (!req.getEndsAt().isAfter(req.getStartsAt())) {
            throw new BadRequestException("ends_at must be after starts_at");
        }
        FlashSale fs = FlashSale.builder()
                .name(req.getName())
                .discountPercent(req.getDiscountPercent())
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .build();

        if (req.getProducts() != null) {
            for (FlashSaleRequest.ProductEntry pe : req.getProducts()) {
                Product product = productRepository.findById(pe.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product", pe.getProductId()));
                fs.getProducts().add(FlashSaleProduct.builder()
                        .flashSale(fs).product(product).stockLimit(pe.getStockLimit()).build());
            }
        }
        return FlashSaleResponse.from(flashSaleRepository.save(fs));
    }

    @CacheEvict(value = "activeSales", allEntries = true)
    @Transactional
    public FlashSaleResponse update(Long id, FlashSaleRequest req) {
        FlashSale fs = flashSaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FlashSale", id));
        if (fs.getStatus() != FlashSale.Status.SCHEDULED) {
            throw new BadRequestException("Only SCHEDULED flash sales can be updated");
        }
        fs.setName(req.getName());
        fs.setDiscountPercent(req.getDiscountPercent());
        fs.setStartsAt(req.getStartsAt());
        fs.setEndsAt(req.getEndsAt());
        return FlashSaleResponse.from(flashSaleRepository.save(fs));
    }

    @CacheEvict(value = "activeSales", allEntries = true)
    @Transactional
    public void delete(Long id) {
        FlashSale fs = flashSaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FlashSale", id));
        fs.softDelete();
        fs.setStatus(FlashSale.Status.CANCELLED);
        flashSaleRepository.save(fs);
    }

    @CacheEvict(value = "activeSales", allEntries = true)
    @Transactional
    public FlashSaleResponse addProduct(Long flashSaleId, Long productId, Integer stockLimit) {
        FlashSale fs = flashSaleRepository.findById(flashSaleId)
                .orElseThrow(() -> new ResourceNotFoundException("FlashSale", flashSaleId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        FlashSaleProduct fsp = FlashSaleProduct.builder()
                .flashSale(fs).product(product).stockLimit(stockLimit).build();
        fs.getProducts().add(fsp);
        return FlashSaleResponse.from(flashSaleRepository.save(fs));
    }

    @CacheEvict(value = "activeSales", allEntries = true)
    @Transactional
    public void removeProduct(Long flashSaleId, Long productId) {
        FlashSaleProduct fsp = flashSaleProductRepository
                .findByFlashSaleIdAndProductId(flashSaleId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("FlashSaleProduct", productId));
        flashSaleProductRepository.delete(fsp);
    }

    // Compute flash price for a product — returns null if no active flash sale
    @Transactional(readOnly = true)
    public BigDecimal getFlashPrice(Long productId, BigDecimal basePrice) {
        Optional<FlashSaleProduct> fsp = flashSaleProductRepository.findActiveByProductId(productId);
        return fsp.map(f -> {
            // Check stock_limit not exceeded
            if (f.getStockLimit() != null && f.getSoldCount() >= f.getStockLimit()) return null;
            BigDecimal discount = f.getFlashSale().getDiscountPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return basePrice.multiply(BigDecimal.ONE.subtract(discount)).setScale(0, RoundingMode.HALF_UP);
        }).orElse(null);
    }

    @Scheduled(fixedDelay = 60_000)
    @CacheEvict(value = "activeSales", allEntries = true)
    @Transactional
    public void syncFlashSaleStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<FlashSale> toActivate = flashSaleRepository.findScheduledToActivate(now);
        toActivate.forEach(fs -> {
            fs.setStatus(FlashSale.Status.ACTIVE);
            flashSaleRepository.save(fs);
            log.info("Flash sale '{}' activated", fs.getName());
        });

        List<FlashSale> toEnd = flashSaleRepository.findActiveToEnd(now);
        toEnd.forEach(fs -> {
            fs.setStatus(FlashSale.Status.ENDED);
            flashSaleRepository.save(fs);
            log.info("Flash sale '{}' ended", fs.getName());
        });
    }
}
