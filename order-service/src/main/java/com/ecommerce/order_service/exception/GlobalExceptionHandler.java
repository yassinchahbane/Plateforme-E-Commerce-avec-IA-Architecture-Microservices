package com.ecommerce.order_service.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<String> rabbitMqUnavailable(AmqpException ex) {
        log.error("RabbitMQ operation failed", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("RabbitMQ is unavailable; the message could not be published");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> duplicate(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Operation already processed");
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<String> localNotFound(RessourceIntrouvableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> refuse(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<String> notFound(FeignException.NotFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("External resource not found");
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<String> feign(FeignException ex) {
        if (ex.status() == -1) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Communication failure with a service");
        }
        if (ex.status() == 503) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Service or instance unavailable");
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("HTTP error from remote service: " + ex.status());
    }
}
