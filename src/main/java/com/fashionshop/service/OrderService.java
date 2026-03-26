package com.fashionshop.service;

import com.fashionshop.dto.request.CreateOrderRequest;
import com.fashionshop.dto.request.UpdateOrderStatusRequest;
import com.fashionshop.dto.response.OrderResponse;
import com.fashionshop.entity.*;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.ResourceNotFoundException;
import com.fashionshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    private static final Set<String> ALLOWED_SORT = Set.of("createdAt", "updatedAt", "totalAmount", "status");

    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest req) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Validate stock for all items first
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                throw new BadRequestException(
                        "Insufficient stock for product: " + product.getName()
                                + ". Available: " + product.getStock());
            }
        }

        // Calculate total
        BigDecimal total = cartItems.stream()
                .map(i -> i.getProduct().getEffectivePrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Validate coupon if provided
        Coupon coupon = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (req.getCouponCode() != null && !req.getCouponCode().isBlank()) {
            coupon = couponRepository.findByCode(req.getCouponCode().toUpperCase())
                    .filter(c -> c.isValid(total))
                    .orElseThrow(() -> new BadRequestException("Invalid or expired coupon"));
            discountAmount = coupon.calculateDiscount(total);
        }

        // Build order
        Order order = Order.builder()
                .user(user)
                .totalAmount(total)
                .discountAmount(discountAmount)
                .shippingAddress(req.getShippingAddress())
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "COD")
                .coupon(coupon)
                .notes(req.getNotes())
                .build();

        // Build order items + decrement stock
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(item.getQuantity())
                    .price(product.getEffectivePrice())
                    .size(item.getSize())
                    .color(item.getColor())
                    .build();
            order.getItems().add(orderItem);
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }

        // Increment coupon usage
        if (coupon != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }

        Order saved = orderRepository.save(order);

        // Clear cart
        cartItemRepository.deleteByUserId(userId);

        return OrderResponse.from(saved);
    }

    public Page<OrderResponse> getOrders(Long userId, String status, int page, int size, String sort) {
        String sortField = ALLOWED_SORT.contains(sort) ? sort : "createdAt";
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortField));

        Page<Order> orders;
        if (status != null) {
            Order.Status orderStatus = Order.Status.valueOf(status.toUpperCase());
            orders = orderRepository.findByUserIdAndStatus(userId, orderStatus, pageRequest);
        } else {
            orders = orderRepository.findByUserId(userId, pageRequest);
        }
        return orders.map(OrderResponse::from);
    }

    public Page<OrderResponse> getAllOrders(String status, int page, int size, String sort) {
        String sortField = ALLOWED_SORT.contains(sort) ? sort : "createdAt";
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortField));

        if (status != null) {
            Order.Status orderStatus = Order.Status.valueOf(status.toUpperCase());
            return orderRepository.findByStatus(orderStatus, pageRequest).map(OrderResponse::from);
        }
        return orderRepository.findAll(pageRequest).map(OrderResponse::from);
    }

    public OrderResponse getOrderById(Long userId, Long orderId, boolean isAdmin) {
        Order order;
        if (isAdmin) {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        } else {
            order = orderRepository.findByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        }
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        Order.Status newStatus = Order.Status.valueOf(req.getStatus().toUpperCase());
        validateStatusTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != Order.Status.PENDING && order.getStatus() != Order.Status.CONFIRMED) {
            throw new BadRequestException("Cannot cancel order with status: " + order.getStatus());
        }

        // Restore stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        // Decrement coupon usage if applicable
        if (order.getCoupon() != null) {
            Coupon coupon = order.getCoupon();
            coupon.setUsedCount(Math.max(0, coupon.getUsedCount() - 1));
            couponRepository.save(coupon);
        }

        order.setStatus(Order.Status.CANCELLED);
        return OrderResponse.from(orderRepository.save(order));
    }

    private void validateStatusTransition(Order.Status current, Order.Status next) {
        boolean valid = switch (current) {
            case PENDING -> next == Order.Status.CONFIRMED || next == Order.Status.CANCELLED;
            case CONFIRMED -> next == Order.Status.SHIPPING || next == Order.Status.CANCELLED;
            case SHIPPING -> next == Order.Status.DELIVERED;
            default -> false;
        };
        if (!valid) {
            throw new BadRequestException("Cannot transition from " + current + " to " + next);
        }
    }
}
