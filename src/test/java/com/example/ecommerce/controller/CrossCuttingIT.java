package com.example.ecommerce.controller;

import com.example.ecommerce.dto.order.OrderLineDTO;
import com.example.ecommerce.dto.order.OrderResponseDTO;
import com.example.ecommerce.dto.order.OrderUpdateDTO;
import com.example.ecommerce.dto.product.ProductResponseDTO;
import com.example.ecommerce.dto.product.ProductUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static com.example.ecommerce.domain.entity.OrderStatus.CANCELLED;
import static com.example.ecommerce.domain.entity.OrderStatus.PROCESSING;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class CrossCuttingIT extends IntegrationTestBase {

    private String ordersUrl;
    private String productsUrl;

    @BeforeEach
    void setUp() {
        ordersUrl = url("/api/orders");
        productsUrl = url("/api/products");
    }

    @Test
    @DisplayName("Deleting a product preserves order item snapshots (productName and unitPrice)")
    void shouldPreserveOrderItemSnapshots_whenProductIsDeleted() {

        // Given
        var product = createTestProduct("Deluxe Widget", new BigDecimal("49.99"), 100);
        var order = createTestOrder("Jane Doe", "jane@example.com", List.of(new OrderLineDTO(product.code(), 2)));
        assertThat(order).isNotNull();
        assertThat(order.items()).hasSize(1);

        // When
        var deleteResponse = restClient.delete()
                .uri(productsUrl + "/{code}", product.code())
                .retrieve()
                .toBodilessEntity();
        assertThat(deleteResponse.getStatusCode()).isEqualTo(NO_CONTENT);

        // Then
        var orderResponse = restClient.get()
                .uri(ordersUrl + "/{code}", order.code())
                .retrieve()
                .toEntity(OrderResponseDTO.class);

        assertThat(orderResponse.getStatusCode()).isEqualTo(OK);
        assertThat(orderResponse.getBody()).isNotNull();
        assertThat(orderResponse.getBody().items()).hasSize(1);

        var item = orderResponse.getBody().items().getFirst();
        assertThat(item.productName()).isEqualTo("Deluxe Widget");
        assertThat(item.unitPrice()).isEqualByComparingTo(new BigDecimal("49.99"));
        assertThat(item.productCode()).isNull();
    }

    @Test
    @DisplayName("Creating an order decrements stock; cancelling restores it fully")
    void shouldFullyRestoreStock_whenOrderIsCancelledAfterCreation() {
        // Given
        var product = createTestProduct("Premium Gadget", new BigDecimal("79.99"), 100);

        // When
        var order = createTestOrder("John Doe", "john@example.com", List.of(new OrderLineDTO(product.code(), 10)));
        assertThat(order).isNotNull();

        // Then
        var afterOrder = getProduct(product.code());
        assertThat(afterOrder.stockQuantity()).isEqualTo(90);

        var cancelDTO = new OrderUpdateDTO(
                order.customerName(),
                order.customerEmail(),
                CANCELLED,
                order.version()
        );
        var cancelResponse = restClient.put()
                .uri(ordersUrl + "/{code}", order.code())
                .contentType(APPLICATION_JSON)
                .body(cancelDTO)
                .retrieve()
                .toEntity(OrderResponseDTO.class);

        // Then
        assertThat(cancelResponse.getStatusCode()).isEqualTo(OK);
        assertThat(cancelResponse.getBody()).isNotNull();
        assertThat(cancelResponse.getBody().status()).isEqualTo(CANCELLED);

        var afterCancel = getProduct(product.code());
        assertThat(afterCancel.stockQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("Second update with stale version returns 409 Conflict")
    void shouldReturnConflict_whenSecondUpdateUsesStaleVersion() {
        // Given
        var product = createTestProduct("Versioned Item", new BigDecimal("15.00"), 50);
        assertThat(product.version()).isZero();

        // When
        var firstUpdate = new ProductUpdateDTO(
                "Versioned Item - Updated",
                "Updated description",
                new BigDecimal("16.00"),
                55,
                0L
        );
        var firstResponse = restClient.put()
                .uri(productsUrl + "/{code}", product.code())
                .contentType(APPLICATION_JSON)
                .body(firstUpdate)
                .retrieve()
                .toEntity(ProductResponseDTO.class);

        assertThat(firstResponse.getStatusCode()).isEqualTo(OK);
        assertThat(firstResponse.getBody()).isNotNull();
        assertThat(firstResponse.getBody().version()).isEqualTo(1L);

        // When
        var staleUpdate = new ProductUpdateDTO(
                "Versioned Item - Stale",
                "Stale description",
                new BigDecimal("17.00"),
                60,
                0L
        );
        var staleResponse = restClient.put()
                .uri(productsUrl + "/{code}", product.code())
                .contentType(APPLICATION_JSON)
                .body(staleUpdate)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);

        // Then
        assertThat(staleResponse.getStatusCode()).isEqualTo(CONFLICT);
        assertThat(staleResponse.getBody()).isNotNull();
        assertThat(staleResponse.getBody().getStatus()).isEqualTo(409);
        assertThat(staleResponse.getBody().getTitle()).isEqualTo("Conflict");
        assertThat(staleResponse.getBody().getDetail()).contains("modified");
    }

    @Test
    @DisplayName("Concurrent order update: optimistic lock prevents conflicting updates")
    void shouldPreventConflict_whenConcurrentOrderUpdatesOccur() throws Exception {
        // Given
        var product = createTestProduct("Versioned Product", new BigDecimal("20.00"), 50);
        var order = createTestOrder("Version Test", "version@test.com", List.of(new OrderLineDTO(product.code(), 1)));
        assertThat(order.version()).isZero();

        var executor = newFixedThreadPool(2);
        var latch = new CountDownLatch(1);

        Callable<Integer> updateTask = () -> {
            var client = RestClient.create();
            latch.await();
            var dto = new OrderUpdateDTO("Updated Customer", "updated@test.com", PROCESSING, 0L);
            return client.put()
                    .uri(ordersUrl + "/{code}", order.code())
                    .contentType(APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                    })
                    .toEntity(String.class)
                    .getStatusCode().value();
        };

        // When
        var future1 = executor.submit(updateTask);
        var future2 = executor.submit(updateTask);
        latch.countDown();

        int status1 = future1.get();
        int status2 = future2.get();
        executor.shutdown();

        // Then
        var statuses = List.of(status1, status2);
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
    }

    @Test
    @DisplayName("Malformed UUID in path returns 400 Bad Request")
    void shouldReturnBadRequest_whenUuidIsMalformed() {
        // Given
        var malformedUrl = url("/api/products/not-a-uuid");

        // When
        var response = restClient.get()
                .uri(malformedUrl)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                })
                .toEntity(ProblemDetail.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
    }
}
