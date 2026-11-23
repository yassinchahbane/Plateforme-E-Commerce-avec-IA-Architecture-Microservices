package com.ecommerce.order_service.event;

import com.ecommerce.order_service.config.OrderRabbitConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedRabbitPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                OrderRabbitConfiguration.EXCHANGE,
                OrderRabbitConfiguration.ORDER_CREATED_ROUTING_KEY,
                event);
        log.info("Published order-created event for order {}", event.orderNumber());
    }
}
