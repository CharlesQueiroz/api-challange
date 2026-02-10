package com.example.ecommerce.dto.orderitem;

import com.example.ecommerce.dto.common.VersionedUpdateDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request body for updating an order item")
public record OrderItemUpdateDTO(
        @Schema(description = "Updated quantity", example = "5", minimum = "1")
        @NotNull @Positive Integer quantity,

        @Schema(description = "Optimistic locking version (must match current version)", example = "0")
        @NotNull Long version
) implements VersionedUpdateDTO {
}
