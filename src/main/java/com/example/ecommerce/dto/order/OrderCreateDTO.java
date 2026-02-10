package com.example.ecommerce.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Request body for creating a new order")
public record OrderCreateDTO(
        @Schema(description = "Customer full name", example = "Charles Queiroz", maxLength = 255)
        @NotBlank @Size(max = 255) String customerName,

        @Schema(description = "Customer email address")
        @NotBlank @Email String customerEmail,

        @Schema(description = "Order items to create (at least one required)")
        @NotEmpty @Valid List<OrderLineDTO> items
) {
}
