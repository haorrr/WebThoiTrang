package com.fashionshop.service;

import com.fashionshop.dto.response.DashboardResponse;
import com.fashionshop.entity.Order;
import com.fashionshop.entity.Product;
import com.fashionshop.entity.User;
import com.fashionshop.repository.OrderRepository;
import com.fashionshop.repository.ProductRepository;
import com.fashionshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public DashboardResponse getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDateTime last30Days = now.minusDays(30);

        // Revenue
        BigDecimal totalRevenue = orderRepository.sumRevenue();
        BigDecimal revenueThisMonth = orderRepository.sumRevenueBetween(startOfMonth, now);
        BigDecimal revenueLastMonth = orderRepository.sumRevenueBetween(startOfLastMonth, startOfMonth);
        double growthPercent = calcGrowth(revenueLastMonth, revenueThisMonth);

        // Orders by status
        long totalOrders = orderRepository.count();
        long ordersThisMonth = orderRepository.countByCreatedAtAfter(startOfMonth);
        long pending = orderRepository.countByStatus(Order.Status.PENDING);
        long confirmed = orderRepository.countByStatus(Order.Status.CONFIRMED);
        long shipping = orderRepository.countByStatus(Order.Status.SHIPPING);
        long delivered = orderRepository.countByStatus(Order.Status.DELIVERED);
        long cancelled = orderRepository.countByStatus(Order.Status.CANCELLED);

        // Users
        long totalUsers = userRepository.countByDeletedAtIsNull();
        long newUsersThisMonth = userRepository.countByDeletedAtIsNullAndCreatedAtAfter(startOfMonth);
        long activeUsers = userRepository.countByDeletedAtIsNullAndStatus(User.Status.ACTIVE);

        // Products
        long totalProducts = productRepository.countByDeletedAtIsNull();
        long outOfStock = productRepository.countByDeletedAtIsNullAndStock(0);
        List<Product> lowStockList = productRepository.findLowStock(5, PageRequest.of(0, 10));
        long lowStock = lowStockList.size();

        // Revenue chart (last 30 days)
        List<Object[]> rawChart = orderRepository.revenueByDay(last30Days);
        List<DashboardResponse.RevenueByDay> revenueChart = rawChart.stream()
                .map(row -> DashboardResponse.RevenueByDay.builder()
                        .date(row[0].toString())
                        .orders(((Number) row[1]).longValue())
                        .revenue(new BigDecimal(row[2].toString()))
                        .build())
                .toList();

        // Products by category
        List<Object[]> rawCats = productRepository.countByCategory();
        Map<String, Long> productsByCategory = new LinkedHashMap<>();
        for (Object[] row : rawCats) {
            String catName = row[0] != null ? row[0].toString() : "Uncategorized";
            productsByCategory.put(catName, ((Number) row[1]).longValue());
        }

        // Low stock table
        List<DashboardResponse.LowStockProduct> lowStockTable = lowStockList.stream()
                .map(p -> DashboardResponse.LowStockProduct.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .slug(p.getSlug())
                        .stock(p.getStock())
                        .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                        .build())
                .toList();

        // Recent orders (last 10)
        List<DashboardResponse.RecentOrder> recentOrders = orderRepository
                .findAll(PageRequest.of(0, 10,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                .getContent().stream()
                .map(o -> DashboardResponse.RecentOrder.builder()
                        .id(o.getId())
                        .customerName(o.getUser().getName())
                        .customerEmail(o.getUser().getEmail())
                        .totalAmount(o.getTotalAmount().subtract(o.getDiscountAmount()))
                        .status(o.getStatus().name())
                        .createdAt(o.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build())
                .toList();

        return DashboardResponse.builder()
                .totalRevenue(totalRevenue)
                .revenueThisMonth(revenueThisMonth)
                .revenueLastMonth(revenueLastMonth)
                .revenueGrowthPercent(growthPercent)
                .totalOrders(totalOrders)
                .ordersThisMonth(ordersThisMonth)
                .ordersPending(pending)
                .ordersConfirmed(confirmed)
                .ordersShipping(shipping)
                .ordersDelivered(delivered)
                .ordersCancelled(cancelled)
                .totalUsers(totalUsers)
                .newUsersThisMonth(newUsersThisMonth)
                .activeUsers(activeUsers)
                .totalProducts(totalProducts)
                .outOfStockProducts(outOfStock)
                .lowStockProducts(lowStock)
                .revenueChart(revenueChart)
                .productsByCategory(productsByCategory)
                .lowStockList(lowStockTable)
                .recentOrders(recentOrders)
                .build();
    }

    private double calcGrowth(BigDecimal prev, BigDecimal curr) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return curr.subtract(prev)
                .divide(prev, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
}
