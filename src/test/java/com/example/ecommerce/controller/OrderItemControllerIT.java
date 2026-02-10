package com.example.ecommerce.controller;

import com.example.ecommerce.dto.order.OrderResponseDTO;
import com.example.ecommerce.dto.orderitem.OrderItemCreateDTO;
import com.example.ecommerce.dto.orderitem.OrderItemResponseDTO;
import com.example.ecommerce.dto.orderitem.OrderItemUpdateDTO;
import com.example.ecommerce.dto.product.ProductResponseDTO;
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
import static org.springframework.http.HttpStatus.*;

class OrderItemControllerIT extends IntegrationTestBase {

    private String baseUrl;

    private ProductResponseDTO defaultProduct;
    private OrderResponseDTO defaultOrder;

    @BeforeEach
    void setUp() {
        baseUrl = url("/api/order-items");
        defaultProduct = createTestProduct("Test Product", new BigDecimal("25.00"), 100);
        defaultOrder = createTestOrder(defaultProduct.code(), 1);
    }

    @Test
    @DisplayName("Successfully create an order item with valid data")
    void shouldCreateOrderItem_whenValidData() {
        // Given
        var dto = new OrderItemCreateDTO(defaultProduct.code(), 3, defaultOrder.code());

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(dto)
            .retrieve()
            .toEntity(OrderItemResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isNotNull();
        assertThat(response.getBody().productCode()).isEqualTo(defaultProduct.code());
        assertThat(response.getBody().productName()).isEqualTo("Test Product");
        assertThat(response.getBody().unitPrice()).isEqualByComparingTo(new BigDecimal("25.00"));

        assertThat(response.getBody().quantity()).isEqualTo(3);
        assertThat(response.getBody().createdAt()).isNotNull();
        assertThat(response.getBody().updatedAt()).isNotNull();
        assertThat(response.getBody().version()).isZero();
    }

    @Test
    @DisplayName("Creating an order item decrements stock and updates order total amount")
    void shouldDecrementStockAndUpdateTotal_whenOrderItemIsCreated() {
        // Given
        var dto = new OrderItemCreateDTO(defaultProduct.code(), 5, defaultOrder.code());

        // When
        restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(dto)
            .retrieve()
            .body(OrderItemResponseDTO.class);

        // Then
        var product = getProduct(defaultProduct.code());
        assertThat(product.stockQuantity()).isEqualTo(94);

        var order = getOrder(defaultOrder.code());
        assertThat(order.totalAmount()).isEqualByComparingTo(new BigDecimal("150.00"));

    }

    @Test
    @DisplayName("Return 400 when order item quantity exceeds available stock")
    void shouldReturnBadRequest_whenStockIsInsufficient() {
        // Given
        var limitedProduct = createTestProduct("Limited Product", new BigDecimal("10.00"), 5);
        var dto = new OrderItemCreateDTO(limitedProduct.code(), 10, defaultOrder.code());

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(dto)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Return 404 when creating order item with non-existent order")
    void shouldReturnNotFound_whenOrderDoesNotExist() {
        // Given
        var dto = new OrderItemCreateDTO(defaultProduct.code(), 2, UUID.randomUUID());

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(dto)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("Return 404 when creating order item with non-existent product")
    void shouldReturnNotFound_whenProductDoesNotExist() {
        // Given
        var dto = new OrderItemCreateDTO(UUID.randomUUID(), 2, defaultOrder.code());

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(dto)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("Return 400 when creating order item with null quantity")
    void shouldReturnBadRequest_whenQuantityIsNull() {
        // Given
        var jsonPayload = """
            {
                "productCode": "%s",
                "quantity": null,
                "orderCode": "%s"
            }
            """.formatted(defaultProduct.code(), defaultOrder.code());

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(jsonPayload)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("Return 400 when creating order item without productCode")
    void shouldReturnBadRequest_whenProductCodeIsNull() {
        // Given
        var jsonPayload = """
            {
                "productCode": null,
                "quantity": 2,
                "orderCode": "%s"
            }
            """.formatted(defaultOrder.code());

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(jsonPayload)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("Return 400 when creating order item with quantity zero")
    void shouldReturnBadRequest_whenQuantityIsZero() {
        // Given
        var jsonPayload = """
            {
                "productCode": "%s",
                "quantity": 0,
                "orderCode": "%s"
            }
            """.formatted(defaultProduct.code(), defaultOrder.code());

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(jsonPayload)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("Return 400 when creating order item with negative quantity")
    void shouldReturnBadRequest_whenQuantityIsNegative() {
        // Given
        var jsonPayload = """
            {
                "productCode": "%s",
                "quantity": -1,
                "orderCode": "%s"
            }
            """.formatted(defaultProduct.code(), defaultOrder.code());

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(jsonPayload)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("Return 404 when creating order item with deleted product")
    void shouldReturnNotFound_whenProductIsDeleted() {
        // Given
        var tempProduct = createTestProduct("Temporary Product", new BigDecimal("15.00"), 50);
        restClient.delete()
            .uri(url("/api/products/{code}"), tempProduct.code())
            .retrieve()
            .toBodilessEntity();

        // When
        var dto = new OrderItemCreateDTO(tempProduct.code(), 2, defaultOrder.code());
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(dto)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("Return 400 when creating order item with malformed JSON")
    void shouldReturnBadRequest_whenJsonIsMalformed() {
        // Given
        var malformedJson = "{ this is not valid json }";

        // When
        var response = restClient.post()
            .uri(baseUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(malformedJson)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    // ── READ ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Retrieve paginated list of order items with metadata")
    void shouldReturnPageMetadata_whenListingOrderItems() {
        // Given
        for (int i = 1; i <= 3; i++) {
            createTestOrderItem(defaultOrder.code(), defaultProduct.code(), 2);
        }

        // When
        var response = restClient.get()
            .uri(baseUrl + "?page=0&size=2")
            .retrieve()
            .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(OK);
        var body = response.getBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        var page = (Map<String, Object>) body.get("page");
        assertThat(page.get("totalElements")).isEqualTo(4);
        assertThat(page.get("size")).isEqualTo(2);
        assertThat(page.get("number")).isEqualTo(0);
        assertThat(page.get("totalPages")).isEqualTo(2);
    }

    @Test
    @DisplayName("Retrieve a specific order item by its code with all fields present")
    void shouldReturnOrderItem_whenCodeExists() {
        // Given
        var created = createTestOrderItem(defaultOrder.code(), defaultProduct.code(), 4);

        // When
        var response = restClient.get()
            .uri(baseUrl + "/{code}", created.code())
            .retrieve()
            .toEntity(OrderItemResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(created.code());
        assertThat(response.getBody().productCode()).isEqualTo(defaultProduct.code());
        assertThat(response.getBody().productName()).isEqualTo("Test Product");
        assertThat(response.getBody().unitPrice()).isEqualByComparingTo(new BigDecimal("25.00"));

        assertThat(response.getBody().quantity()).isEqualTo(4);
        assertThat(response.getBody().createdAt()).isNotNull();
        assertThat(response.getBody().updatedAt()).isNotNull();
        assertThat(response.getBody().version()).isNotNull();
    }

    @Test
    @DisplayName("Return 404 when requesting an order item with non-existent code")
    void shouldReturnNotFound_whenOrderItemCodeDoesNotExist() {
        // Given
        var nonExistentCode = UUID.randomUUID();

        // When
        var response = restClient.get()
            .uri(baseUrl + "/{code}", nonExistentCode)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
        assertThat(response.getBody().getDetail()).contains("OrderItem");
    }

    @Test
    @DisplayName("TC-OI-05: Get order item after product deletion — productCode null, snapshots preserved")
    void shouldPreserveSnapshots_whenProductIsDeleted() {
        // Given
        var product = createTestProduct("Deletable Product", new BigDecimal("42.00"), 50);
        var order = createTestOrder(product.code(), 1);
        var orderItem = createTestOrderItem(order.code(), product.code(), 3);

        // Delete the product
        restClient.delete()
            .uri(url("/api/products/{code}"), product.code())
            .retrieve()
            .toBodilessEntity();

        // When
        var response = restClient.get()
            .uri(baseUrl + "/{code}", orderItem.code())
            .retrieve()
            .toEntity(OrderItemResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().productCode()).isNull();
        assertThat(response.getBody().productName()).isEqualTo("Deletable Product");
        assertThat(response.getBody().unitPrice()).isEqualByComparingTo(new BigDecimal("42.00"));

        assertThat(response.getBody().quantity()).isEqualTo(3);
    }

    // ── UPDATE ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Increasing quantity decrements more stock from product")
    void shouldDecrementMoreStock_whenQuantityIncreases() {
        // Given
        var created = createTestOrderItem(defaultOrder.code(), defaultProduct.code(), 2);
        var stockAfterCreate = getProduct(defaultProduct.code()).stockQuantity();
        assertThat(stockAfterCreate).isEqualTo(97);

        // When
        var updateDTO = new OrderItemUpdateDTO(5, created.version());
        var response = restClient.put()
            .uri(baseUrl + "/{code}", created.code())
            .contentType(MediaType.APPLICATION_JSON)
            .body(updateDTO)
            .retrieve()
            .toEntity(OrderItemResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().quantity()).isEqualTo(5);
        assertThat(response.getBody().version()).isEqualTo(created.version() + 1);

        // Verify stock decremented by 3 more (97 - 3 = 94)
        var product = getProduct(defaultProduct.code());
        assertThat(product.stockQuantity()).isEqualTo(94);
    }

    @Test
    @DisplayName("Decreasing quantity restores some stock to product")
    void shouldRestoreStock_whenQuantityDecreases() {
        // Given
        var created = createTestOrderItem(defaultOrder.code(), defaultProduct.code(), 5);
        var stockAfterCreate = getProduct(defaultProduct.code()).stockQuantity();
        assertThat(stockAfterCreate).isEqualTo(94);

        // When
        var updateDTO = new OrderItemUpdateDTO(2, created.version());
        var response = restClient.put()
            .uri(baseUrl + "/{code}", created.code())
            .contentType(MediaType.APPLICATION_JSON)
            .body(updateDTO)
            .retrieve()
            .toEntity(OrderItemResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().quantity()).isEqualTo(2);
        assertThat(response.getBody().version()).isEqualTo(created.version() + 1);

        // Verify stock restored by 3 (94 + 3 = 97)
        var product = getProduct(defaultProduct.code());
        assertThat(product.stockQuantity()).isEqualTo(97);
    }

    @Test
    @DisplayName("Return 409 when updating with stale version")
    void shouldReturnConflict_whenOrderItemVersionIsStale() {
        // Given
        var created = createTestOrderItem(defaultOrder.code(), defaultProduct.code(), 2);

        // When
        var updateDTO = new OrderItemUpdateDTO(5, 999L);
        var response = restClient.put()
            .uri(baseUrl + "/{code}", created.code())
            .contentType(MediaType.APPLICATION_JSON)
            .body(updateDTO)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getTitle()).isEqualTo("Conflict");
        assertThat(response.getBody().getDetail()).contains("modified");
    }

    @Test
    @DisplayName("TC-OI-16: Return 404 when updating order item with non-existent code")
    void shouldReturnNotFound_whenUpdatingNonExistentOrderItem() {
        // Given
        var nonExistentCode = UUID.randomUUID();
        var updateDTO = new OrderItemUpdateDTO(5, 0L);

        // When
        var response = restClient.put()
            .uri(baseUrl + "/{code}", nonExistentCode)
            .contentType(MediaType.APPLICATION_JSON)
            .body(updateDTO)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
        assertThat(response.getBody().getDetail()).contains("OrderItem");
    }

    @Test
    @DisplayName("TC-OI-18: Return 400 when updating order item without version")
    void shouldReturnBadRequest_whenVersionIsNull() {
        // Given
        var created = createTestOrderItem(defaultOrder.code(), defaultProduct.code(), 2);
        var jsonPayload = """
            {
                "quantity": 5,
                "version": null
            }
            """;

        // When
        var response = restClient.put()
            .uri(baseUrl + "/{code}", created.code())
            .contentType(MediaType.APPLICATION_JSON)
            .body(jsonPayload)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("TC-OI-19: Return 400 when updating order item with quantity zero")
    void shouldReturnBadRequest_whenUpdatingWithZeroQuantity() {
        // Given
        var created = createTestOrderItem(defaultOrder.code(), defaultProduct.code(), 2);
        var jsonPayload = """
            {
                "quantity": 0,
                "version": %d
            }
            """.formatted(created.version());

        // When
        var response = restClient.put()
            .uri(baseUrl + "/{code}", created.code())
            .contentType(MediaType.APPLICATION_JSON)
            .body(jsonPayload)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deleting an order item restores stock and updates order total amount")
    void shouldRestoreStockAndUpdateTotal_whenOrderItemIsDeleted() {
        // Given
        var created = createTestOrderItem(defaultOrder.code(), defaultProduct.code(), 5);
        var stockAfterCreate = getProduct(defaultProduct.code()).stockQuantity();
        assertThat(stockAfterCreate).isEqualTo(94);

        var orderAfterCreate = getOrder(defaultOrder.code());
        // Order total includes original item (1 * 25.00) + new item (5 * 25.00) = 150.00
        assertThat(orderAfterCreate.totalAmount()).isEqualByComparingTo(new BigDecimal("150.00"));

        // When
        var response = restClient.delete()
            .uri(baseUrl + "/{code}", created.code())
            .retrieve()
            .toBodilessEntity();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT);

        // Verify stock restored (94 + 5 = 99)
        var product = getProduct(defaultProduct.code());
        assertThat(product.stockQuantity()).isEqualTo(99);

        // Verify order totalAmount updated (only original item remains: 1 * 25.00 = 25.00)
        var order = getOrder(defaultOrder.code());
        assertThat(order.totalAmount()).isEqualByComparingTo(new BigDecimal("25.00"));

    }

    @Test
    @DisplayName("Return 404 when deleting a non-existent order item")
    void shouldReturnNotFound_whenDeletingNonExistentOrderItem() {
        // Given
        var nonExistentCode = UUID.randomUUID();

        // When
        var response = restClient.delete()
            .uri(baseUrl + "/{code}", nonExistentCode)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {})
            .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
    }
}
