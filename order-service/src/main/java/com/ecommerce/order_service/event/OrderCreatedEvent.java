package com.ecommerce.order_service.event;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        Long orderId,
        String orderNumber,
        Long userId,
        BigDecimal totalAmount) {
}
