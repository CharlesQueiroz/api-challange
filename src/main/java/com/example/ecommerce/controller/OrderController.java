package com.example.ecommerce.controller;

import com.example.ecommerce.dto.order.OrderCreateDTO;
import com.example.ecommerce.dto.order.OrderResponseDTO;
import com.example.ecommerce.dto.order.OrderUpdateDTO;
import com.example.ecommerce.dto.orderitem.OrderItemResponseDTO;
import com.example.ecommerce.service.OrderService;
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
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "CRUD operations for order management")
public class OrderController extends AbstractCrudController<OrderCreateDTO, OrderUpdateDTO, OrderResponseDTO> {

    private final OrderService orderService;

    @Override
    protected OrderService service() {
        return orderService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new order",
            description = "Creates a new order with items. Status defaults to PENDING. Stock is decremented for each item."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Order created successfully",
            content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request body, validation errors, or insufficient stock",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public ResponseEntity<OrderResponseDTO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return createResource(dto);
    }

    @GetMapping
    @Operation(
            summary = "Get all orders",
            description = "Returns a paginated list of all orders with inline items."
    )
    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    public Page<OrderResponseDTO> findAll(@Parameter(description = "Pagination and sorting parameters") Pageable pageable) {
        return findAllResources(pageable);
    }

    @GetMapping("/{code}")
    @Operation(
            summary = "Get an order by code",
            description = "Returns a single order with its items identified by its UUID code."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order found",
            content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Order not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public OrderResponseDTO findByCode(@Parameter(description = "Order UUID code") @PathVariable UUID code) {
        return findResourceByCode(code);
    }

    @PutMapping("/{code}")
    @Operation(
            summary = "Update an existing order",
            description = "Updates order-level fields. If status changes to CANCELLED, stock is restored for all items."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Order updated successfully",
            content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request body or invalid status transition",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Order not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Version conflict — resource was modified by another request",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public OrderResponseDTO update(@Parameter(description = "Order UUID code") @PathVariable UUID code, @Valid @RequestBody OrderUpdateDTO dto) {
        return updateResource(code, dto);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(NO_CONTENT)
    @Operation(
            summary = "Delete an order by code",
            description = "Deletes an order and all its items. Stock is restored for all items."
    )
    @ApiResponse(responseCode = "204", description = "Order deleted successfully")
    @ApiResponse(
            responseCode = "404",
            description = "Order not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public void delete(@Parameter(description = "Order UUID code") @PathVariable UUID code) {
        deleteResource(code);
    }

    @GetMapping("/{code}/order-items")
    @Operation(
            summary = "Get order items for a specific order",
            description = "Returns a paginated list of order items belonging to the specified order."
    )
    @ApiResponse(responseCode = "200", description = "Order items retrieved successfully")
    @ApiResponse(
            responseCode = "404",
            description = "Order not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public Page<OrderItemResponseDTO> findOrderItems(
            @Parameter(description = "Order UUID code")
            @PathVariable UUID code,
            @Parameter(description = "Pagination and sorting parameters")
            Pageable pageable) {
        return orderService.findOrderItemsByOrderCode(code, pageable);
    }
}
