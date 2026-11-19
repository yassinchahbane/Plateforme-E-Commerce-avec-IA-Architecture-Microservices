package com.ecommerce.order_service.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencySchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateExistingOrders() {
        jdbcTemplate.update(
                "UPDATE orders SET idempotency_key = 'legacy-' || id WHERE idempotency_key IS NULL");
        jdbcTemplate.execute(
                "ALTER TABLE orders ALTER COLUMN idempotency_key SET NOT NULL");
    }
}
