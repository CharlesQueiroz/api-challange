package com.example.ecommerce.dto.product;

import com.example.ecommerce.dto.common.HasCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Product resource representation")
public record ProductResponseDTO(
        @Schema(description = "Unique product identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID code,

        @Schema(description = "Product name", example = "Wireless Mouse")
        String name,

        @Schema(description = "Product description", example = "Wireless mouse with USB")
        String description,

        @Schema(description = "Product price in EUR", example = "29.99")
        BigDecimal price,

        @Schema(description = "Available stock quantity", example = "150")
        Integer stockQuantity,

        @Schema(description = "Creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Last update timestamp")
        LocalDateTime updatedAt,

        @Schema(description = "Optimistic locking version", example = "1")
        Long version
) implements HasCode {
}
