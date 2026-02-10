package com.example.ecommerce.controller;

import com.example.ecommerce.TestcontainersConfiguration;
import com.example.ecommerce.dto.order.OrderCreateDTO;
import com.example.ecommerce.dto.order.OrderLineDTO;
import com.example.ecommerce.dto.order.OrderResponseDTO;
import com.example.ecommerce.dto.orderitem.OrderItemCreateDTO;
import com.example.ecommerce.dto.orderitem.OrderItemResponseDTO;
import com.example.ecommerce.dto.product.ProductCreateDTO;
import com.example.ecommerce.dto.product.ProductResponseDTO;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
abstract class IntegrationTestBase {

    @LocalServerPort
    protected int port;

    @Autowired
    protected OrderItemRepository orderItemRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected ProductRepository productRepository;

    protected RestClient restClient;

    @BeforeEach
    void setUpBase() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        restClient = RestClient.create();
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected ProductResponseDTO createTestProduct(String name, BigDecimal price, int stock) {
        var dto = new ProductCreateDTO(name, null, price, stock);
        return restClient.post()
            .uri(url("/api/products"))
            .contentType(APPLICATION_JSON)
            .body(dto)
            .retrieve()
            .body(ProductResponseDTO.class);
    }

    protected OrderResponseDTO createTestOrder(UUID productCode, int quantity) {
        return createTestOrder("Test Customer", "test@example.com", List.of(new OrderLineDTO(productCode, quantity)));
    }

    protected OrderResponseDTO createTestOrder(String customerName, String email, List<OrderLineDTO> items) {
        var dto = new OrderCreateDTO(customerName, email, items);
        return restClient.post()
            .uri(url("/api/orders"))
            .contentType(APPLICATION_JSON)
            .body(dto)
            .retrieve()
            .body(OrderResponseDTO.class);
    }

    protected OrderItemResponseDTO createTestOrderItem(UUID orderCode, UUID productCode, int quantity) {
        var dto = new OrderItemCreateDTO(productCode, quantity, orderCode);
        return restClient.post()
            .uri(url("/api/order-items"))
            .contentType(APPLICATION_JSON)
            .body(dto)
            .retrieve()
            .body(OrderItemResponseDTO.class);
    }

    protected ProductResponseDTO getProduct(UUID code) {
        return restClient.get()
            .uri(url("/api/products/{code}"), code)
            .retrieve()
            .body(ProductResponseDTO.class);
    }

    protected OrderResponseDTO getOrder(UUID code) {
        return restClient.get()
            .uri(url("/api/orders/{code}"), code)
            .retrieve()
            .body(OrderResponseDTO.class);
    }
}
