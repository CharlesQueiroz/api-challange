package com.example.ecommerce.dto.orderitem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

@Schema(description = "Request body for creating an order item")
public record OrderItemCreateDTO(
        @Schema(description = "Product UUID code")
        @NotNull UUID productCode,

        @Schema(description = "Quantity to order", example = "3", minimum = "1")
        @NotNull @Positive Integer quantity,

        @Schema(description = "Parent order UUID code")
        @NotNull UUID orderCode
) {
}
