package com.example.ecommerce.dto.order;

import com.example.ecommerce.domain.entity.OrderStatus;
import com.example.ecommerce.dto.common.VersionedUpdateDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating an existing order")
public record OrderUpdateDTO(
        @Schema(description = "Customer full name", example = "Charles Queiroz Updated")
        @NotBlank @Size(max = 255) String customerName,

        @Schema(description = "Customer email address")
        @NotBlank @Email String customerEmail,

        @Schema(description = "Order status", example = "PROCESSING")
        @NotNull OrderStatus status,

        @Schema(description = "Optimistic locking version (must match current version)", example = "0")
        @NotNull Long version
) implements VersionedUpdateDTO {
}
