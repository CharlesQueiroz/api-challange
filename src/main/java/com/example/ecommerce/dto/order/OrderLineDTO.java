package com.example.ecommerce.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

@Schema(description = "Inline order item for order creation")
public record OrderLineDTO(
        @Schema(description = "Product UUID code", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull UUID productCode,

        @Schema(description = "Quantity to order", example = "2", minimum = "1")
        @NotNull @Positive Integer quantity
) {
}
