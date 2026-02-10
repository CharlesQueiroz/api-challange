package com.example.ecommerce.controller;

import com.example.ecommerce.dto.product.ProductCreateDTO;
import com.example.ecommerce.dto.product.ProductResponseDTO;
import com.example.ecommerce.dto.product.ProductUpdateDTO;
import com.example.ecommerce.service.ProductService;
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
@RequestMapping("/api/products")
@Tag(name = "Products", description = "CRUD operations for product management")
public class ProductController extends AbstractCrudController<ProductCreateDTO, ProductUpdateDTO, ProductResponseDTO> {

    private final ProductService productService;

    @Override
    protected ProductService service() {
        return productService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new product",
            description = "Creates a new product with the provided details."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Product created successfully",
            content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request body — validation errors",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Duplicate product name",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public ResponseEntity<ProductResponseDTO> create(@Valid @RequestBody ProductCreateDTO dto) {
        return createResource(dto);
    }

    @GetMapping
    @Operation(
            summary = "Get all products",
            description = "Returns a paginated list of all products. Supports sorting by any field."
    )
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    public Page<ProductResponseDTO> findAll(@Parameter(description = "Pagination and sorting parameters") Pageable pageable) {
        return findAllResources(pageable);
    }

    @GetMapping("/{code}")
    @Operation(
            summary = "Get a product by code",
            description = "Returns a single product identified by its UUID code."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product found",
            content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public ProductResponseDTO findByCode(
            @Parameter(description = "Product UUID code", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID code) {
        return findResourceByCode(code);
    }

    @PutMapping("/{code}")
    @Operation(
            summary = "Update an existing product",
            description = "Updates a product identified by its UUID code. Requires the current version for optimistic locking."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product updated successfully",
            content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request body — validation errors",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Version conflict — resource was modified by another request",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public ProductResponseDTO update(
            @Parameter(description = "Product UUID code")
            @PathVariable UUID code,
            @Valid @RequestBody ProductUpdateDTO dto) {
        return updateResource(code, dto);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(NO_CONTENT)
    @Operation(
            summary = "Delete a product by code",
            description = "Deletes a product. Existing order items referencing this product retain their snapshot data; the product reference is set to null."
    )
    @ApiResponse(responseCode = "204", description = "Product deleted successfully")
    @ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public void delete(@Parameter(description = "Product UUID code") @PathVariable UUID code) {
        deleteResource(code);
    }
}
