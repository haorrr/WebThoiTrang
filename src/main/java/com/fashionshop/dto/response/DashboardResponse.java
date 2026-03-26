package com.fashionshop.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {

    // Summary cards
    private BigDecimal totalRevenue;
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueLastMonth;
    private double revenueGrowthPercent;

    private long totalOrders;
    private long ordersThisMonth;
    private long ordersPending;
    private long ordersConfirmed;
    private long ordersShipping;
    private long ordersDelivered;
    private long ordersCancelled;

    private long totalUsers;
    private long newUsersThisMonth;
    private long activeUsers;

    private long totalProducts;
    private long outOfStockProducts;
    private long lowStockProducts;   // stock <= 5

    // Charts
    private List<RevenueByDay> revenueChart;       // last 30 days
    private Map<String, Long> productsByCategory;   // category -> count

    // Tables
    private List<LowStockProduct> lowStockList;
    private List<RecentOrder> recentOrders;

    @Data
    @Builder
    public static class RevenueByDay {
        private String date;
        private long orders;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    public static class LowStockProduct {
        private Long id;
        private String name;
        private String slug;
        private int stock;
        private String categoryName;
    }

    @Data
    @Builder
    public static class RecentOrder {
        private Long id;
        private String customerName;
        private String customerEmail;
        private BigDecimal totalAmount;
        private String status;
        private String createdAt;
    }
}
