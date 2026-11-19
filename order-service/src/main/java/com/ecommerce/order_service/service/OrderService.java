package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderItemRequest;
import com.ecommerce.order_service.dto.OrderItemResponse;
import com.ecommerce.order_service.dto.OrderRequest;
import com.ecommerce.order_service.dto.OrderResponse;
import com.ecommerce.order_service.dto.OrderDetailResponse;
import com.ecommerce.order_service.dto.OrderItemDetailResponse;
import com.ecommerce.order_service.feignclient.ProductDTO;
import com.ecommerce.order_service.feignclient.ProductRequest;
import com.ecommerce.order_service.feignclient.UserDto;
import com.ecommerce.order_service.feignclient.UserRequest;
import com.ecommerce.order_service.exception.RessourceIntrouvableException;
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
    private final ProductRequest productRequest;
    private final UserRequest userRequest;

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest, String idempotencyKey) {
        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 255) {
            throw new IllegalArgumentException("Invalid idempotency key");
        }

        if (orderRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new IllegalArgumentException("Operation already processed");
        }

        log.info("Creating order for user: {}", orderRequest.getUserId());

        UserDto user = userRequest.getUserById(orderRequest.getUserId());

        // Create order
        Order order = Order.builder()
                .userId(orderRequest.getUserId())
                .orderNumber(generateOrderNumber())
                .idempotencyKey(idempotencyKey)
                .status(Order.OrderStatus.PENDING)
                .build();

        // Create order items
        List<OrderItem> orderItems = orderRequest.getOrderItems().stream()
                .map(itemRequest -> {
                    ProductDTO product = productRequest.getProductById(itemRequest.getProductId());

                    if (product.getStock() == null
                            || product.getStock() < itemRequest.getQuantity()) {
                        throw new IllegalArgumentException(
                                "Insufficient stock for product " + itemRequest.getProductId());
                    }

                    BigDecimal unitPrice = product.getPrice();
                    BigDecimal totalPrice = unitPrice.multiply(
                            BigDecimal.valueOf(itemRequest.getQuantity()));

                    return OrderItem.builder()
                            .order(order)
                            .productId(itemRequest.getProductId())
                            .productName(product.getName())
                            .quantity(itemRequest.getQuantity())
                            .unitPrice(unitPrice)
                            .totalPrice(totalPrice)
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

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetailById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RessourceIntrouvableException("Order not found"));

        UserDto user = userRequest.getUserById(order.getUserId());

        List<OrderItemDetailResponse> detailedItems = order.getOrderItems().stream()
                .map(item -> {
                    ProductDTO product = productRequest.getProductById(item.getProductId());

                    return OrderItemDetailResponse.builder()
                            .productId(item.getProductId())
                            .productName(product.getName())
                            .productDescription(product.getDescription())
                            .productCategory(product.getCategory())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .totalPrice(item.getTotalPrice())
                            .build();
                })
                .toList();

        String userName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        if (userName.isBlank()) {
            userName = user.getUsername();
        }

        return OrderDetailResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .orderItems(detailedItems)
                .userName(userName)
                .userEmail(user.getEmail())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
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
