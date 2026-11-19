package com.ecommerce.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDetailResponse {
    private Long productId;
    private String productName;
    private String productDescription;
    private String productCategory;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
