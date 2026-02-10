package com.example.ecommerce.controller;

import com.example.ecommerce.dto.orderitem.OrderItemCreateDTO;
import com.example.ecommerce.dto.orderitem.OrderItemResponseDTO;
import com.example.ecommerce.dto.orderitem.OrderItemUpdateDTO;
import com.example.ecommerce.service.OrderItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-items")
@Tag(name = "Order Items", description = "CRUD operations for order item management")
public class OrderItemController extends AbstractCrudController<OrderItemCreateDTO, OrderItemUpdateDTO, OrderItemResponseDTO> {

    private final OrderItemService orderItemService;

    @Override
    protected OrderItemService service() {
        return orderItemService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new order item",
            description = "Creates a new order item for an existing order."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Order item created successfully",
            content = @Content(schema = @Schema(implementation = OrderItemResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request body, validation errors, or insufficient stock",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Product or order not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public ResponseEntity<OrderItemResponseDTO> create(@Valid @RequestBody OrderItemCreateDTO dto) {
        return createResource(dto);
    }

    @GetMapping
    @Operation(
            summary = "Get all order items",
            description = "Returns a paginated list of all order items."
    )
    @ApiResponse(responseCode = "200", description = "Order items retrieved successfully")
    public Page<OrderItemResponseDTO> findAll(@Parameter(description = "Pagination and sorting parameters") Pageable pageable) {
        return findAllResources(pageable);
    }

    @GetMapping("/{code}")
    @Operation(
            summary = "Get an order item by code",
            description = "Returns a single order item identified by its UUID code."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order item found",
            content = @Content(schema = @Schema(implementation = OrderItemResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Order item not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public OrderItemResponseDTO findByCode(@Parameter(description = "Order item UUID code") @PathVariable UUID code) {
        return findResourceByCode(code);
    }

    @PutMapping("/{code}")
    @Operation(
            summary = "Update an order item",
            description = "Updates the quantity of an order item."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order item updated successfully",
            content = @Content(schema = @Schema(implementation = OrderItemResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request body — validation errors",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Order item not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Version conflict — resource was modified by another request",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public OrderItemResponseDTO update(
            @Parameter(description = "Order item UUID code")
            @PathVariable UUID code,
            @Valid @RequestBody OrderItemUpdateDTO dto) {
        return updateResource(code, dto);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(NO_CONTENT)
    @Operation(
            summary = "Delete an order item by code",
            description = "Deletes an order item. Stock is restored and the parent order total is recalculated."
    )
    @ApiResponse(responseCode = "204", description = "Order item deleted successfully")
    @ApiResponse(
            responseCode = "404",
            description = "Order item not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public void delete(@Parameter(description = "Order item UUID code") @PathVariable UUID code) {
        deleteResource(code);
    }
}
