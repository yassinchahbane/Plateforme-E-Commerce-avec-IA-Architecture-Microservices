package com.ecommerce.backend.dto;


import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

// DTO = what the API accepts/returns. Never expose the @Entity directly.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {

    private Long id;           // null on create, set on read/update

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;

    @Min(0)
    private Integer stock;

    private String category;
}