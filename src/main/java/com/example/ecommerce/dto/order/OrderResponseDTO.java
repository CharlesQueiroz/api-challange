package com.example.ecommerce.dto.order;

import com.example.ecommerce.domain.entity.OrderStatus;
import com.example.ecommerce.dto.common.HasCode;
import com.example.ecommerce.dto.orderitem.OrderItemResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Order resource representation")
public record OrderResponseDTO(
        @Schema(description = "Unique order identifier (UUID)", example = "660e8400-e29b-41d4-a716-446655440000")
        UUID code,

        @Schema(description = "Customer full name")
        String customerName,

        @Schema(description = "Customer email address")
        String customerEmail,

        @Schema(description = "Order status", example = "PENDING")
        OrderStatus status,

        @Schema(description = "Total order amount in EUR", example = "89.97")
        BigDecimal totalAmount,

        @Schema(description = "Order date")
        LocalDateTime orderDate,

        @Schema(description = "Order items")
        List<OrderItemResponseDTO> items,

        @Schema(description = "Creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Last update timestamp")
        LocalDateTime updatedAt,

        @Schema(description = "Optimistic locking version", example = "0")
        Long version
) implements HasCode {
}
