package com.fashionshop.dto.response;

import com.fashionshop.entity.FlashSale;
import com.fashionshop.entity.FlashSaleProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleResponse {

    private Long id;
    private String name;
    private BigDecimal discountPercent;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private String status;
    private List<FlashSaleProductEntry> products;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlashSaleProductEntry {
        private Long id;
        private Long productId;
        private String productName;
        private String primaryImageUrl;
        private BigDecimal originalPrice;
        private BigDecimal flashPrice;
        private Integer stockLimit;
        private Integer soldCount;
    }

    public static FlashSaleResponse from(FlashSale fs) {
        List<FlashSaleProductEntry> entries = fs.getProducts().stream().map(fsp -> {
            BigDecimal original = fsp.getProduct().getEffectivePrice();
            BigDecimal flash = original.multiply(
                    BigDecimal.ONE.subtract(fs.getDiscountPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
            ).setScale(0, RoundingMode.HALF_UP);
            String img = fsp.getProduct().getImages().stream()
                    .filter(i -> i.isPrimary()).findFirst()
                    .map(i -> i.getImageUrl())
                    .orElse(fsp.getProduct().getImages().isEmpty() ? null : fsp.getProduct().getImages().get(0).getImageUrl());
            return FlashSaleProductEntry.builder()
                    .id(fsp.getId())
                    .productId(fsp.getProduct().getId())
                    .productName(fsp.getProduct().getName())
                    .primaryImageUrl(img)
                    .originalPrice(original)
                    .flashPrice(flash)
                    .stockLimit(fsp.getStockLimit())
                    .soldCount(fsp.getSoldCount())
                    .build();
        }).toList();

        return FlashSaleResponse.builder()
                .id(fs.getId())
                .name(fs.getName())
                .discountPercent(fs.getDiscountPercent())
                .startsAt(fs.getStartsAt())
                .endsAt(fs.getEndsAt())
                .status(fs.getStatus().name())
                .products(entries)
                .createdAt(fs.getCreatedAt())
                .build();
    }
}
