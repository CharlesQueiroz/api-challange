package com.example.ecommerce.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request body for creating a new product")
public record ProductCreateDTO(
        @Schema(description = "Product name", example = "Wireless Mouse", maxLength = 255)
        @NotBlank @Size(max = 255) String name,

        @Schema(description = "Product description", example = "wireless mouse", maxLength = 2000)
        @Size(max = 2000) String description,

        @Schema(description = "Product price in EUR", example = "29.99")
        @NotNull @Positive BigDecimal price,

        @Schema(description = "Available stock quantity", example = "150", minimum = "0")
        @NotNull @PositiveOrZero Integer stockQuantity
) {
}
