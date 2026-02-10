package com.example.ecommerce.controller;

import com.example.ecommerce.dto.order.OrderCreateDTO;
import com.example.ecommerce.dto.order.OrderLineDTO;
import com.example.ecommerce.dto.order.OrderResponseDTO;
import com.example.ecommerce.dto.order.OrderUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.ecommerce.domain.entity.OrderStatus.CANCELLED;
import static com.example.ecommerce.domain.entity.OrderStatus.COMPLETED;
import static com.example.ecommerce.domain.entity.OrderStatus.PENDING;
import static com.example.ecommerce.domain.entity.OrderStatus.PROCESSING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

class OrderControllerIT extends IntegrationTestBase {

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = url("/api/orders");
    }

    @Test
    @DisplayName("Successfully create an order with one item, status PENDING, totalAmount calculated")
    void shouldCreateOrder_whenValidData() {
        // Given
        var product = createTestProduct("Wireless Mouse", new BigDecimal("29.99"), 100);

        var createDTO = new OrderCreateDTO("John Doe", "john@example.com",
                List.of(new OrderLineDTO(product.code(), 3)));

        // When
        var response = restClient.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createDTO)
                .retrieve()
                .toEntity(OrderResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isNotNull();
        assertThat(response.getBody().customerName()).isEqualTo("John Doe");
        assertThat(response.getBody().customerEmail()).isEqualTo("john@example.com");
        assertThat(response.getBody().status()).isEqualTo(PENDING);
        assertThat(response.getBody().totalAmount()).isEqualByComparingTo(new BigDecimal("89.97"));

        assertThat(response.getBody().orderDate()).isNotNull();
        assertThat(response.getBody().items()).hasSize(1);
        assertThat(response.getBody().items().getFirst().productCode()).isEqualTo(product.code());
        assertThat(response.getBody().items().getFirst().productName()).isEqualTo("Wireless Mouse");
        assertThat(response.getBody().items().getFirst().unitPrice()).isEqualByComparingTo(new BigDecimal("29.99"));

        assertThat(response.getBody().items().getFirst().quantity()).isEqualTo(3);
        assertThat(response.getBody().createdAt()).isNotNull();
        assertThat(response.getBody().updatedAt()).isNotNull();
        assertThat(response.getBody().version()).isZero();
    }

    @Test
    @DisplayName("Create order with multiple items calculates totalAmount as sum of (unitPrice * quantity)")
    void shouldCalculateTotal_whenOrderHasMultipleItems() {
        // Given
        var product1 = createTestProduct("Mouse", new BigDecimal("25.00"), 50);
        var product2 = createTestProduct("Keyboard", new BigDecimal("75.00"), 50);

        var createDTO = new OrderCreateDTO("Jane Doe", "jane@example.com",
                List.of(
                        new OrderLineDTO(product1.code(), 2),
                        new OrderLineDTO(product2.code(), 1)
                ));

        // When
        var response = restClient.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createDTO)
                .retrieve()
                .toEntity(OrderResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalAmount()).isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(response.getBody().items()).hasSize(2);
    }

    @Test
    @DisplayName("Creating an order decrements product stock by the ordered quantity")
    void shouldDecrementStock_whenOrderIsCreated() {
        // Given
        var product = createTestProduct("Monitor", new BigDecimal("299.99"), 50);

        // When
        createTestOrder(product.code(), 5);

        // Then
        var updatedProduct = getProduct(product.code());
        assertThat(updatedProduct.stockQuantity()).isEqualTo(45);
    }

    @Test
    @DisplayName("Return 400 when ordering more than available stock")
    void shouldReturnBadRequest_whenStockIsInsufficient() {
        // Given
        var product = createTestProduct("Rare Item", new BigDecimal("99.99"), 5);

        var createDTO = new OrderCreateDTO("John Doe", "john@example.com",
                List.of(new OrderLineDTO(product.code(), 10)));

        // When
        var response = restClient.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createDTO)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Return 400 when items list is empty")
    void shouldReturnBadRequest_whenItemsListIsEmpty() {
        // Given
        var createDTO = new OrderCreateDTO("John Doe", "john@example.com", List.of());

        // When
        var response = restClient.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createDTO)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
        assertThat(response.getBody().getDetail()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("Return 404 when creating order with non-existent product code")
    void shouldReturnNotFound_whenProductDoesNotExist() {
        // Given
        var createDTO = new OrderCreateDTO("John Doe", "john@example.com",
                List.of(new OrderLineDTO(UUID.randomUUID(), 1)));

        // When
        var response = restClient.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createDTO)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("Return 400 when customer name is blank")
    void shouldReturnBadRequest_whenCustomerNameIsBlank() {
        // Given
        var product = createTestProduct("Product", new BigDecimal("10.00"), 50);

        var createDTO = new OrderCreateDTO("   ", "john@example.com",
                List.of(new OrderLineDTO(product.code(), 1)));

        // When
        var response = restClient.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createDTO)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
        assertThat(response.getBody().getDetail()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("Return 400 when customer email is invalid")
    void shouldReturnBadRequest_whenEmailIsInvalid() {
        // Given
        var product = createTestProduct("Product", new BigDecimal("10.00"), 50);

        var createDTO = new OrderCreateDTO("John Doe", "not-an-email",
                List.of(new OrderLineDTO(product.code(), 1)));

        // When
        var response = restClient.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createDTO)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
        assertThat(response.getBody().getDetail()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("Order totalAmount calculation has correct BigDecimal precision")
    void shouldCalculateTotalWithPrecision_whenCreatingOrder() {
        // Given
        var product = createTestProduct("Precision Widget", new BigDecimal("33.33"), 100);

        var createDTO = new OrderCreateDTO("Precision Test", "prec@example.com",
                List.of(new OrderLineDTO(product.code(), 3)));

        // When
        var response = restClient.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createDTO)
                .retrieve()
                .toEntity(OrderResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalAmount()).isEqualByComparingTo(new BigDecimal("99.99"));

    }

    @Test
    @DisplayName("Retrieve paginated list of orders with page metadata")
    void shouldReturnPageMetadata_whenListingOrders() {
        // Given
        var product = createTestProduct("Widget", new BigDecimal("10.00"), 100);
        for (int i = 0; i < 3; i++) {
            createTestOrder(product.code(), 1);
        }

        // When
        var response = restClient.get()
                .uri(baseUrl + "?page=0&size=2")
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(OK);
        var body = response.getBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        var page = (Map<String, Object>) body.get("page");
        assertThat(page.get("totalElements")).isEqualTo(3);
        assertThat(page.get("size")).isEqualTo(2);
        assertThat(page.get("number")).isEqualTo(0);
        assertThat(page.get("totalPages")).isEqualTo(2);
    }

    @Test
    @DisplayName("Retrieve an existing order by code with all fields including items")
    void shouldReturnOrderWithItems_whenCodeExists() {
        // Given
        var product = createTestProduct("Webcam", new BigDecimal("59.99"), 25);
        var created = createTestOrder(product.code(), 2);

        // When
        var response = restClient.get()
                .uri(baseUrl + "/{code}", created.code())
                .retrieve()
                .toEntity(OrderResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(created.code());
        assertThat(response.getBody().customerName()).isEqualTo("Test Customer");
        assertThat(response.getBody().customerEmail()).isEqualTo("test@example.com");
        assertThat(response.getBody().status()).isEqualTo(PENDING);
        assertThat(response.getBody().totalAmount()).isEqualByComparingTo(new BigDecimal("119.98"));

        assertThat(response.getBody().orderDate()).isNotNull();
        assertThat(response.getBody().items()).hasSize(1);
        assertThat(response.getBody().items().getFirst().productCode()).isEqualTo(product.code());
        assertThat(response.getBody().items().getFirst().productName()).isEqualTo("Webcam");
        assertThat(response.getBody().items().getFirst().unitPrice()).isEqualByComparingTo(new BigDecimal("59.99"));

        assertThat(response.getBody().items().getFirst().quantity()).isEqualTo(2);
        assertThat(response.getBody().createdAt()).isNotNull();
        assertThat(response.getBody().updatedAt()).isNotNull();
        assertThat(response.getBody().version()).isNotNull();
    }

    @Test
    @DisplayName("Return 404 when requesting an order with non-existent code")
    void shouldReturnNotFound_whenOrderCodeDoesNotExist() {
        // Given
        var nonExistentCode = UUID.randomUUID();

        // When
        var response = restClient.get()
                .uri(baseUrl + "/{code}", nonExistentCode)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTitle()).isEqualTo("Not Found");
        assertThat(response.getBody().getDetail()).contains("Order");
    }

    @Test
    @DisplayName("Successfully update order customer info with matching version")
    void shouldUpdateOrder_whenVersionMatches() {
        // Given
        var product = createTestProduct("Product", new BigDecimal("20.00"), 100);
        var created = createTestOrder(product.code(), 1);

        // When
        var updateDTO = new OrderUpdateDTO("Updated Customer", "updated@example.com", PROCESSING, created.version());
        var response = restClient.put()
                .uri(baseUrl + "/{code}", created.code())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDTO)
                .retrieve()
                .toEntity(OrderResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(created.code());
        assertThat(response.getBody().customerName()).isEqualTo("Updated Customer");
        assertThat(response.getBody().customerEmail()).isEqualTo("updated@example.com");
        assertThat(response.getBody().status()).isEqualTo(PROCESSING);
        assertThat(response.getBody().version()).isEqualTo(created.version() + 1);
    }

    @Test
    @DisplayName("Cancelling an order restores product stock and sets totalAmount to zero")
    void shouldRestoreStock_whenOrderIsCancelled() {
        // Given
        var product = createTestProduct("Gadget", new BigDecimal("50.00"), 30);
        var created = createTestOrder(product.code(), 5);

        // Verify stock was decremented
        var afterOrder = getProduct(product.code());
        assertThat(afterOrder.stockQuantity()).isEqualTo(25);

        // When
        var updateDTO = new OrderUpdateDTO(
                created.customerName(),
                created.customerEmail(),
                CANCELLED,
                created.version()
        );
        var response = restClient.put()
                .uri(baseUrl + "/{code}", created.code())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDTO)
                .retrieve()
                .toEntity(OrderResponseDTO.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(CANCELLED);
        assertThat(response.getBody().totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        // Verify stock was restored
        var afterCancel = getProduct(product.code());
        assertThat(afterCancel.stockQuantity()).isEqualTo(30);
    }

    @Test
    @DisplayName("Return 409 when updating order with stale version")
    void shouldReturnConflict_whenOrderVersionIsStale() {
        // Given
        var product = createTestProduct("Product", new BigDecimal("15.00"), 50);
        var created = createTestOrder(product.code(), 1);

        // When
        var updateDTO = new OrderUpdateDTO(
                "Updated Customer",
                "updated@example.com",
                PROCESSING,
                999L
        );
        var response = restClient.put()
                .uri(baseUrl + "/{code}", created.code())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDTO)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getTitle()).isEqualTo("Conflict");
        assertThat(response.getBody().getDetail()).contains("modified");
    }

    @Test
    @DisplayName("Return 400 when updating order with invalid status transition")
    void shouldReturnBadRequest_whenStatusTransitionIsInvalid() {
        // Given
        var product = createTestProduct("Product", new BigDecimal("15.00"), 50);
        var created = createTestOrder(product.code(), 1);

        // PENDING -> COMPLETED is invalid in business rules
        var updateDTO = new OrderUpdateDTO(
                created.customerName(),
                created.customerEmail(),
                COMPLETED,
                created.version()
        );

        // When
        var response = restClient.put()
                .uri(baseUrl + "/{code}", created.code())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDTO)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getTitle()).isEqualTo("Bad Request");
        assertThat(response.getBody().getDetail()).contains("Cannot transition order");
    }

    @Test
    @DisplayName("Return 400 when updating a cancelled order status")
    void shouldReturnBadRequest_whenTransitioningFromCancelled() {
        // Given
        var product = createTestProduct("Product", new BigDecimal("15.00"), 50);
        var created = createTestOrder(product.code(), 1);
        var cancelDTO = new OrderUpdateDTO(
                created.customerName(), created.customerEmail(),
                CANCELLED, created.version()
        );
        var cancelled = restClient.put()
                .uri(baseUrl + "/{code}", created.code())
                .contentType(MediaType.APPLICATION_JSON)
                .body(cancelDTO)
                .retrieve()
                .body(OrderResponseDTO.class);
        assertThat(cancelled).isNotNull();

        // When
        var updateDTO = new OrderUpdateDTO(
                cancelled.customerName(), cancelled.customerEmail(),
                PROCESSING, cancelled.version()
        );
        var response = restClient.put()
                .uri(baseUrl + "/{code}", created.code())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDTO)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).contains("Cannot transition order");
    }

    @Test
    @DisplayName("Return 400 when updating a completed order status")
    void shouldReturnBadRequest_whenTransitioningFromCompleted() {
        // Given
        var product = createTestProduct("Product", new BigDecimal("15.00"), 50);
        var created = createTestOrder(product.code(), 1);

        // PENDING → PROCESSING
        var processingDTO = new OrderUpdateDTO(
                created.customerName(), created.customerEmail(),
                PROCESSING, created.version()
        );
        var processing = restClient.put()
                .uri(baseUrl + "/{code}", created.code())
                .contentType(MediaType.APPLICATION_JSON)
                .body(processingDTO)
                .retrieve()
                .body(OrderResponseDTO.class);
        assertThat(processing).isNotNull();

        // PROCESSING → COMPLETED
        var completedDTO = new OrderUpdateDTO(
                processing.customerName(), processing.customerEmail(),
                COMPLETED, processing.version()
        );
        var completed = restClient.put()
                .uri(baseUrl + "/{code}", created.code())
                .contentType(MediaType.APPLICATION_JSON)
                .body(completedDTO)
                .retrieve()
                .body(OrderResponseDTO.class);
        assertThat(completed).isNotNull();

        // When
        var updateDTO = new OrderUpdateDTO(completed.customerName(), completed.customerEmail(), CANCELLED, completed.version());
        var response = restClient.put()
                .uri(baseUrl + "/{code}", created.code())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDTO)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).contains("Cannot transition order");
    }

    @Test
    @DisplayName("Return 400 when reversing order from PROCESSING to PENDING")
    void shouldReturnBadRequest_whenReversingToPending() {
        // Given
        var product = createTestProduct("Product", new BigDecimal("15.00"), 50);
        var created = createTestOrder(product.code(), 1);
        var processingDTO = new OrderUpdateDTO(
                created.customerName(), created.customerEmail(),
                PROCESSING, created.version()
        );
        var processing = restClient.put()
                .uri(baseUrl + "/{code}", created.code())
                .contentType(MediaType.APPLICATION_JSON)
                .body(processingDTO)
                .retrieve()
                .body(OrderResponseDTO.class);
        assertThat(processing).isNotNull();

        // When
        var updateDTO = new OrderUpdateDTO(
                processing.customerName(), processing.customerEmail(),
                PENDING, processing.version()
        );
        var response = restClient.put()
                .uri(baseUrl + "/{code}", created.code())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateDTO)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).contains("Cannot transition order");
    }

    @Test
    @DisplayName("Successfully delete an order and restore product stock")
    void shouldDeleteOrderAndRestoreStock_whenCodeExists() {
        // Given
        var product = createTestProduct("Headphones", new BigDecimal("79.99"), 40);
        var created = createTestOrder(product.code(), 3);

        // Verify stock was decremented
        var afterOrder = getProduct(product.code());
        assertThat(afterOrder.stockQuantity()).isEqualTo(37);

        // When
        var response = restClient.delete()
                .uri(baseUrl + "/{code}", created.code())
                .retrieve()
                .toBodilessEntity();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT);

        // Verify stock was restored
        var afterDelete = getProduct(product.code());
        assertThat(afterDelete.stockQuantity()).isEqualTo(40);

        // Verify order is deleted
        var getResponse = restClient.get()
                .uri(baseUrl + "/{code}", created.code())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    @DisplayName("Retrieve paginated order items as a sub-resource of an order")
    void shouldReturnPaginatedItems_whenListingOrderSubResource() {
        // Given
        var product1 = createTestProduct("Mouse", new BigDecimal("25.00"), 50);
        var product2 = createTestProduct("Keyboard", new BigDecimal("75.00"), 50);

        var createDTO = new OrderCreateDTO("John Doe", "john@example.com",
                List.of(
                        new OrderLineDTO(product1.code(), 1),
                        new OrderLineDTO(product2.code(), 2)
                ));
        var created = restClient.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createDTO)
                .retrieve()
                .body(OrderResponseDTO.class);
        assertThat(created).isNotNull();

        // When
        var response = restClient.get()
                .uri(baseUrl + "/{code}/order-items?page=0&size=10", created.code())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {
                });

        // Then
        assertThat(response.getStatusCode()).isEqualTo(OK);
        var body = response.getBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        var page = (Map<String, Object>) body.get("page");
        assertThat(page.get("totalElements")).isEqualTo(2);
        assertThat(page.get("size")).isEqualTo(10);
        assertThat(page.get("number")).isEqualTo(0);
        assertThat(page.get("totalPages")).isEqualTo(1);
    }
}
