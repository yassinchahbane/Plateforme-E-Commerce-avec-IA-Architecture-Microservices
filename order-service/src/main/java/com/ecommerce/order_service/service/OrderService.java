package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderItemRequest;
import com.ecommerce.order_service.dto.OrderItemResponse;
import com.ecommerce.order_service.dto.OrderRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.model.Order;
import com.ecommerce.order_service.model.OrderItem;
import com.ecommerce.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        log.info("Creating order for user: {}", orderRequest.getUserId());

        // Validate products and get product details
        List<Long> productIds = orderRequest.getOrderItems().stream()
                .map(OrderItemRequest::getProductId)
                .collect(Collectors.toList());

        // Check if products exist and get their details
        var productDetails = productServiceClient.getProductsByIds(productIds);
        
        if (productDetails.size() != productIds.size()) {
            throw new RuntimeException("Some products not found");
        }

        // Create order
        Order order = Order.builder()
                .userId(orderRequest.getUserId())
                .orderNumber(generateOrderNumber())
                .status(Order.OrderStatus.PENDING)
                .build();

        // Create order items
        List<OrderItem> orderItems = orderRequest.getOrderItems().stream()
                .map(itemRequest -> {
                    var product = productDetails.stream()
                            .filter(p -> p.getId().equals(itemRequest.getProductId()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Product not found: " + itemRequest.getProductId()));

                    return OrderItem.builder()
                            .order(order)
                            .productId(itemRequest.getProductId())
                            .productName(product.getName())
                            .quantity(itemRequest.getQuantity())
                            .unitPrice(product.getPrice())
                            .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())))
                            .build();
                })
                .collect(Collectors.toList());

        // Calculate total amount
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);
        
        log.info("Order created successfully with order number: {}", savedOrder.getOrderNumber());
        return convertToResponse(savedOrder);
    }

    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return convertToResponse(order);
    }

    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found with order number: " + orderNumber));
        return convertToResponse(order);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(String orderId, Order.OrderStatus status) {
        Order order = orderRepository.findByOrderNumber(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        
        log.info("Order status updated to {} for order: {}", status, updatedOrder.getOrderNumber());
        return convertToResponse(updatedOrder);
    }

    private String generateOrderNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = LocalDateTime.now().format(formatter);
        Random random = new Random();
        int randomPart = 1000 + random.nextInt(9000);
        return "ORD-" + datePart + "-" + randomPart;
    }

    private OrderResponse convertToResponse(Order order) {
        List<OrderItemResponse> orderItemResponses = order.getOrderItems().stream()
                .map(this::convertOrderItemToResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .orderItems(orderItemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse convertOrderItemToResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId())
                .productName(orderItem.getProductName())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .totalPrice(orderItem.getTotalPrice())
                .build();
    }
}
