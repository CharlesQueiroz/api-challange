package com.example.ecommerce.dto.product;

import com.example.ecommerce.dto.common.VersionedUpdateDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request body for updating an existing product")
public record ProductUpdateDTO(
        @Schema(description = "Product name", example = "Wireless Mouse Pro")
        @NotBlank @Size(max = 255) String name,

        @Schema(description = "Product description", example = "Updated ergonomic wireless mouse")
        @Size(max = 2000) String description,

        @Schema(description = "Product price in EUR", example = "59.90")
        @NotNull @Positive BigDecimal price,

        @Schema(description = "Available stock quantity", example = "200")
        @NotNull @PositiveOrZero Integer stockQuantity,

        @Schema(description = "Optimistic locking version (must match current version)", example = "0")
        @NotNull Long version
) implements VersionedUpdateDTO {
}
