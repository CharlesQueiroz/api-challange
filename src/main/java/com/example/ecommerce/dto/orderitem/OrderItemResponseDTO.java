package com.example.ecommerce.dto.orderitem;

import com.example.ecommerce.dto.common.HasCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Order item resource representation")
public record OrderItemResponseDTO(
        @Schema(description = "Unique order item identifier (UUID)")
        UUID code,

        @Schema(description = "Product UUID code (null if product was deleted)", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID productCode,

        @Schema(description = "Snapshot of product name at time of order", example = "Wireless Mouse")
        String productName,

        @Schema(description = "Snapshot of unit price at time of order in EUR", example = "29.99")
        BigDecimal unitPrice,

        @Schema(description = "Quantity ordered", example = "2")
        Integer quantity,

        @Schema(description = "Creation timestamp", example = "2026-02-09T14:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Last update timestamp", example = "2026-02-09T15:45:00")
        LocalDateTime updatedAt,

        @Schema(description = "Optimistic locking version", example = "0")
        Long version
) implements HasCode {
}
