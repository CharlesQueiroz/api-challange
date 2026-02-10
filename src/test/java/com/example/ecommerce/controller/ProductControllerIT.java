package com.example.ecommerce.controller;

import com.example.ecommerce.dto.product.ProductCreateDTO;
import com.example.ecommerce.dto.product.ProductResponseDTO;
import com.example.ecommerce.dto.product.ProductUpdateDTO;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ProductControllerIT extends IntegrationTestBase {

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = url("/api/products");
    }

    @Test
    @DisplayName("Successfully create a product with all valid fields")
    void shouldCreateProduct_whenValidData() {
        // Given
        var createDTO = new ProductCreateDTO(
            "Wireless Mouse",
            "Ergonomic wireless mouse with USB receiver",
            new BigDecimal("29.99"),
            150
        );

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDTO)
            .retrieve()
            .toEntity(ProductResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Wireless Mouse");
        assertThat(response.getBody().description()).isEqualTo("Ergonomic wireless mouse with USB receiver");
        assertThat(response.getBody().price()).isEqualByComparingTo(new BigDecimal("29.99"));

        assertThat(response.getBody().stockQuantity()).isEqualTo(150);
        assertThat(response.getBody().createdAt()).isNotNull();
        assertThat(response.getBody().updatedAt()).isNotNull();
        assertThat(response.getBody().version()).isZero();
    }

    @Test
    @DisplayName("Successfully create a product without optional description")
    void shouldCreateProduct_whenDescriptionIsNull() {
        // Given
        var createDTO = new ProductCreateDTO(
            "Keyboard",
            null,
            new BigDecimal("49.99"),
            75
        );

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDTO)
            .retrieve()
            .toEntity(ProductResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Keyboard");
        assertThat(response.getBody().description()).isNull();
        assertThat(response.getBody().price()).isEqualByComparingTo(new BigDecimal("49.99"));

        assertThat(response.getBody().stockQuantity()).isEqualTo(75);
    }

    @Test
    @DisplayName("Successfully create a product with zero stock quantity")
    void shouldCreateProduct_whenStockIsZero() {
        // Given
        var createDTO = new ProductCreateDTO(
            "Monitor",
            "Out of stock monitor",
            new BigDecimal("299.99"),
            0
        );

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDTO)
            .retrieve()
            .toEntity(ProductResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().stockQuantity()).isZero();
    }

    @Test
    @DisplayName("Reject duplicate product creation when name already exists (case-insensitive)")
    void shouldReturnConflict_whenProductNameIsDuplicate() {
        // Given
        var createDTO = new ProductCreateDTO(
            "Duplicate Name",
            "First product",
            new BigDecimal("10.00"),
            10
        );

        restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDTO)
            .retrieve()
            .toEntity(ProductResponseDTO.class);

        var duplicateDTO = new ProductCreateDTO(
            "  duplicate name  ",
            "Second product",
            new BigDecimal("20.00"),
            20
        );

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(duplicateDTO)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getTitle()).isEqualTo("Conflict");
        assertThat(response.getBody().getDetail()).contains("already exists");
    }

    @Test
    @DisplayName("Reject product creation when name is blank")
    void shouldReturnBadRequest_whenProductNameIsBlank() {
        // Given
        var createDTO = new ProductCreateDTO(
            "   ",
            "Description",
            new BigDecimal("19.99"),
            50
        );

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDTO)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
        assertThat(response.getBody().getDetail()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("Reject product creation when price is negative")
    void shouldReturnBadRequest_whenPriceIsNegative() {
        // Given
        var createDTO = new ProductCreateDTO(
            "Product",
            "Description",
            new BigDecimal("-10.00"),
            50
        );

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDTO)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
        assertThat(response.getBody().getDetail()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("Reject product creation when price is null")
    void shouldReturnBadRequest_whenPriceIsNull() {
        // Given
        var jsonPayload = """
            {
                "name": "Product",
                "description": "Description",
                "price": null,
                "stockQuantity": 50
            }
            """;

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(jsonPayload)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("Ignore unknown generated fields in payload and create product successfully")
    void shouldIgnoreGeneratedFields_whenPayloadContainsThem() {
        // Given
        var jsonPayload = """
            {
                "name": "Product With Unknown Fields",
                "description": "Description",
                "price": 19.99,
                "stockQuantity": 50,
                "code": "550e8400-e29b-41d4-a716-446655440000",
                "createdAt": "2026-02-10T00:00:00",
                "updatedAt": "2026-02-10T00:00:00",
                "version": 0
            }
            """;

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(jsonPayload)
            .retrieve()
            .toEntity(ProductResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Product With Unknown Fields");
        assertThat(response.getBody().description()).isEqualTo("Description");
        assertThat(response.getBody().price()).isEqualByComparingTo(new BigDecimal("19.99"));

        assertThat(response.getBody().stockQuantity()).isEqualTo(50);
        assertThat(response.getBody().code()).isNotNull();
        assertThat(response.getBody().createdAt()).isNotNull();
        assertThat(response.getBody().updatedAt()).isNotNull();
        assertThat(response.getBody().version()).isZero();
    }

    @Test
    @DisplayName("Retrieve paginated list of products with metadata")
    void shouldReturnPageMetadata_whenListingProducts() {
        // Given
        for (int i = 1; i <= 5; i++) {
            var dto = new ProductCreateDTO(
                "Product " + i,
                "Description " + i,
                new BigDecimal("10.00").multiply(BigDecimal.valueOf(i)),
                i * 10
            );
            restClient.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
        }

        // When
        var response = restClient.get()
            .uri(baseUrl + "?page=0&size=3")
            .retrieve()
            .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        var page = (Map<String, Object>) body.get("page");
        assertThat(page.get("totalElements")).isEqualTo(5);
        assertThat(page.get("size")).isEqualTo(3);
        assertThat(page.get("number")).isEqualTo(0);
        assertThat(page.get("totalPages")).isEqualTo(2);
    }

    @Test
    @DisplayName("Retrieve a specific product by its code with all fields present")
    void shouldReturnProduct_whenCodeExists() {
        // Given
        var createDTO = new ProductCreateDTO(
            "Webcam",
            "HD webcam",
            new BigDecimal("59.99"),
            25
        );
        var created = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDTO)
            .retrieve()
            .body(ProductResponseDTO.class);
        assertThat(created).isNotNull();

        // When
        var response = restClient.get()
            .uri(baseUrl + "/{code}", created.code())
            .retrieve()
            .toEntity(ProductResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(created.code());
        assertThat(response.getBody().name()).isEqualTo("Webcam");
        assertThat(response.getBody().description()).isEqualTo("HD webcam");
        assertThat(response.getBody().price()).isEqualByComparingTo(new BigDecimal("59.99"));

        assertThat(response.getBody().stockQuantity()).isEqualTo(25);
        assertThat(response.getBody().createdAt()).isNotNull();
        assertThat(response.getBody().updatedAt()).isNotNull();
        assertThat(response.getBody().version()).isNotNull();
    }

    @Test
    @DisplayName("Return 404 when requesting a product with non-existent code")
    void shouldReturnNotFound_whenProductCodeDoesNotExist() {
        // Given
        var nonExistentCode = UUID.randomUUID();

        // When
        var response = restClient.get()
            .uri(baseUrl + "/{code}", nonExistentCode)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
        assertThat(response.getBody().getDetail()).contains("Product");
    }

    @Test
    @DisplayName("Successfully update a product with matching version")
    void shouldUpdateProduct_whenVersionMatches() {
        // Given
        var createDTO = new ProductCreateDTO(
            "Old Product",
            "Old description",
            new BigDecimal("20.00"),
            100
        );
        var created = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDTO)
            .retrieve()
            .body(ProductResponseDTO.class);
        assertThat(created).isNotNull();

        // When
        var updateDTO = new ProductUpdateDTO(
            "Updated Product",
            "Updated description",
            new BigDecimal("25.00"),
            120,
            created.version()
        );
        var response = restClient.put()
            .uri(baseUrl + "/{code}", created.code())
            .contentType(MediaType.APPLICATION_JSON)
            .body(updateDTO)
            .retrieve()
            .toEntity(ProductResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(created.code());
        assertThat(response.getBody().name()).isEqualTo("Updated Product");
        assertThat(response.getBody().description()).isEqualTo("Updated description");
        assertThat(response.getBody().price()).isEqualByComparingTo(new BigDecimal("25.00"));

        assertThat(response.getBody().stockQuantity()).isEqualTo(120);
        assertThat(response.getBody().version()).isEqualTo(created.version() + 1);
    }

    @Test
    @DisplayName("Return 409 when updating with stale version")
    void shouldReturnConflict_whenProductVersionIsStale() {
        // Given
        var createDTO = new ProductCreateDTO(
            "Product",
            "Description",
            new BigDecimal("30.00"),
            50
        );
        var created = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDTO)
            .retrieve()
            .body(ProductResponseDTO.class);
        assertThat(created).isNotNull();

        // When
        var updateDTO = new ProductUpdateDTO(
            "Updated Product",
            "Updated description",
            new BigDecimal("35.00"),
            60,
            999L
        );
        var response = restClient.put()
            .uri(baseUrl + "/{code}", created.code())
            .contentType(MediaType.APPLICATION_JSON)
            .body(updateDTO)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getTitle()).isEqualTo("Conflict");
        assertThat(response.getBody().getDetail()).contains("modified");
    }

    @Test
    @DisplayName("Successfully delete an existing product")
    void shouldDeleteProduct_whenCodeExists() {
        // Given
        var createDTO = new ProductCreateDTO(
            "Product to Delete",
            "Will be deleted",
            new BigDecimal("15.00"),
            10
        );
        var created = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDTO)
            .retrieve()
            .body(ProductResponseDTO.class);
        assertThat(created).isNotNull();

        // When
        var response = restClient.delete()
            .uri(baseUrl + "/{code}", created.code())
            .retrieve()
            .toBodilessEntity();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify product is deleted
        var getResponse = restClient.get()
            .uri(baseUrl + "/{code}", created.code())
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Return 404 when deleting a non-existent product")
    void shouldReturnNotFound_whenDeletingNonExistentProduct() {
        // Given
        var nonExistentCode = UUID.randomUUID();

        // When
        var response = restClient.delete()
            .uri(baseUrl + "/{code}", nonExistentCode)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("Price with two decimal places is stored correctly")
    void shouldStorePrice_whenTwoDecimalPlaces() {
        var createDTO = new ProductCreateDTO("Precision Product", null, new BigDecimal("29.99"), 10);

        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDTO)
            .retrieve()
            .toEntity(ProductResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().price()).isEqualByComparingTo(new BigDecimal("29.99"));

    }

    @Test
    @DisplayName("Price with zero decimal places is stored correctly")
    void shouldStorePrice_whenZeroDecimalPlaces() {
        var createDTO = new ProductCreateDTO("Whole Price Product", null, new BigDecimal("30"), 10);

        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDTO)
            .retrieve()
            .toEntity(ProductResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().price()).isEqualByComparingTo(new BigDecimal("30.00"));

    }

    @Test
    @DisplayName("Price scale is preserved correctly after update")
    void shouldPreservePriceScale_whenUpdatingProduct() {
        var createDTO = new ProductCreateDTO("Scale Product", null, new BigDecimal("10.50"), 20);
        var created = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDTO)
            .retrieve()
            .body(ProductResponseDTO.class);
        assertThat(created).isNotNull();

        var updateDTO = new ProductUpdateDTO("Scale Product", null, new BigDecimal("99.95"), 20, created.version());
        var response = restClient.put()
            .uri(baseUrl + "/{code}", created.code())
            .contentType(MediaType.APPLICATION_JSON)
            .body(updateDTO)
            .retrieve()
            .toEntity(ProductResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().price()).isEqualByComparingTo(new BigDecimal("99.95"));

    }

    @Test
    @DisplayName("Return 400 when sending malformed JSON in request body")
    void shouldReturnBadRequest_whenJsonIsMalformed() {
        // Given
        var malformedJson = """
            {
                "name": "Product",
                "price": 10.00,
                "stockQuantity": 5
            """;

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(malformedJson)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
    }
}
