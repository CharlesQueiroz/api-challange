package com.example.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.example.ecommerce.domain.entity.Order;
import com.example.ecommerce.domain.entity.OrderItem;
import com.example.ecommerce.domain.entity.OrderStatus;
import com.example.ecommerce.domain.entity.Product;
import com.example.ecommerce.dto.order.OrderCreateDTO;
import com.example.ecommerce.dto.order.OrderLineDTO;
import com.example.ecommerce.dto.order.OrderResponseDTO;
import com.example.ecommerce.dto.order.OrderUpdateDTO;
import com.example.ecommerce.dto.orderitem.OrderItemResponseDTO;
import com.example.ecommerce.exception.EntityNotFoundException;
import com.example.ecommerce.mapper.OrderItemMapper;
import com.example.ecommerce.mapper.OrderMapper;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.service.StockService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private StockService stockService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private static final UUID ORDER_CODE = UUID.randomUUID();
    private static final UUID PRODUCT_CODE_1 = UUID.randomUUID();
    private static final UUID PRODUCT_CODE_2 = UUID.randomUUID();
    private static final UUID ORDER_ITEM_CODE_1 = UUID.randomUUID();
    private static final UUID ORDER_ITEM_CODE_2 = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.now();

    private Product product1;
    private Product product2;
    private Order order;
    private OrderItem orderItem1;
    private OrderItem orderItem2;
    private OrderResponseDTO orderResponseDTO;

    @BeforeEach
    void setUp() {
        product1 = Product.builder()
                .id(1L)
                .code(PRODUCT_CODE_1)
                .version(0L)
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .price(new BigDecimal("29.99"))
                .stockQuantity(150)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        product2 = Product.builder()
                .id(2L)
                .code(PRODUCT_CODE_2)
                .version(0L)
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(new BigDecimal("79.99"))
                .stockQuantity(50)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        order = Order.builder()
                .id(1L)
                .code(ORDER_CODE)
                .version(0L)
                .status(OrderStatus.PENDING)
                .customerName("John Doe")
                .customerEmail("john.doe@example.com")
                .totalAmount(BigDecimal.ZERO)
                .orderDate(NOW)
                .items(new ArrayList<>())
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        orderItem1 = OrderItem.builder()
                .id(1L)
                .code(ORDER_ITEM_CODE_1)
                .version(0L)
                .order(order)
                .product(product1)
                .productName("Wireless Mouse")
                .unitPrice(new BigDecimal("29.99"))
                .quantity(2)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        orderItem2 = OrderItem.builder()
                .id(2L)
                .code(ORDER_ITEM_CODE_2)
                .version(0L)
                .order(order)
                .product(product2)
                .productName("Keyboard")
                .unitPrice(new BigDecimal("79.99"))
                .quantity(1)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        var itemResponseDTO1 = new OrderItemResponseDTO(
                ORDER_ITEM_CODE_1, PRODUCT_CODE_1, "Wireless Mouse",
                new BigDecimal("29.99"), 2, NOW, NOW, 0L
        );
        var itemResponseDTO2 = new OrderItemResponseDTO(
                ORDER_ITEM_CODE_2, PRODUCT_CODE_2, "Keyboard",
                new BigDecimal("79.99"), 1, NOW, NOW, 0L
        );

        orderResponseDTO = new OrderResponseDTO(
                ORDER_CODE,
                "John Doe",
                "john.doe@example.com",
                OrderStatus.PENDING,
                new BigDecimal("139.97"),
                NOW,
                List.of(itemResponseDTO1, itemResponseDTO2),
                NOW,
                NOW,
                0L
        );
    }

    @Test
    @DisplayName("should create order with PENDING status and calculated totalAmount")
    void shouldCreateOrder_whenDataIsValid() {
        var lineDTO1 = new OrderLineDTO(PRODUCT_CODE_1, 2);
        var lineDTO2 = new OrderLineDTO(PRODUCT_CODE_2, 1);
        var createDTO = new OrderCreateDTO("John Doe", "john.doe@example.com", List.of(lineDTO1, lineDTO2));

        var orderShell = Order.builder()
                .customerName("John Doe")
                .customerEmail("john.doe@example.com")
                .build();

        given(orderMapper.toEntity(createDTO)).willReturn(orderShell);
        given(productRepository.findByCode(PRODUCT_CODE_1)).willReturn(Optional.of(product1));
        given(productRepository.findByCode(PRODUCT_CODE_2)).willReturn(Optional.of(product2));
        given(orderRepository.save(any(Order.class))).willReturn(order);
        given(orderMapper.toResponseDTO(order)).willReturn(orderResponseDTO);

        var result = orderService.create(createDTO);

        assertThat(result).isEqualTo(orderResponseDTO);
        assertThat(orderShell.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(orderShell.getTotalAmount()).isEqualByComparingTo(new BigDecimal("139.97"));
        assertThat(orderShell.getItems()).hasSize(2);
        then(orderRepository).should().save(any(Order.class));
    }

    @Test
    @DisplayName("should decrease stock for each order item")
    void shouldDecreaseStock_whenOrderIsCreated() {
        var lineDTO1 = new OrderLineDTO(PRODUCT_CODE_1, 2);
        var lineDTO2 = new OrderLineDTO(PRODUCT_CODE_2, 1);
        var createDTO = new OrderCreateDTO("John Doe", "john.doe@example.com", List.of(lineDTO1, lineDTO2));

        var orderShell = Order.builder()
                .customerName("John Doe")
                .customerEmail("john.doe@example.com")
                .build();

        given(orderMapper.toEntity(createDTO)).willReturn(orderShell);
        given(productRepository.findByCode(PRODUCT_CODE_1)).willReturn(Optional.of(product1));
        given(productRepository.findByCode(PRODUCT_CODE_2)).willReturn(Optional.of(product2));
        given(orderRepository.save(any(Order.class))).willReturn(order);
        given(orderMapper.toResponseDTO(order)).willReturn(orderResponseDTO);

        orderService.create(createDTO);

        then(stockService).should(times(1)).adjust(1L, 2);
        then(stockService).should(times(1)).adjust(2L, 1);
    }

    @Test
    @DisplayName("should return paged results")
    void shouldReturnPagedResults_whenFindingAll() {
        Pageable pageable = PageRequest.of(0, 10);
        var orderPage = new PageImpl<>(List.of(order), pageable, 1);

        given(orderRepository.findAll(pageable)).willReturn(orderPage);
        given(orderMapper.toResponseDTO(order)).willReturn(orderResponseDTO);

        var result = orderService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst()).isEqualTo(orderResponseDTO);
        assertThat(result.getTotalElements()).isEqualTo(1);
        then(orderRepository).should().findAll(pageable);
    }

    @Test
    @DisplayName("should return DTO when order exists")
    void shouldReturnDto_whenCodeExists() {
        given(orderRepository.findByCodeWithItems(ORDER_CODE)).willReturn(Optional.of(order));
        given(orderMapper.toResponseDTO(order)).willReturn(orderResponseDTO);

        var result = orderService.findByCode(ORDER_CODE);

        assertThat(result).isEqualTo(orderResponseDTO);
        then(orderRepository).should().findByCodeWithItems(ORDER_CODE);
    }

    @Test
    @DisplayName("should throw EntityNotFoundException when order does not exist")
    void shouldThrowNotFound_whenCodeDoesNotExist() {
        given(orderRepository.findByCodeWithItems(ORDER_CODE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findByCode(ORDER_CODE))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order")
                .hasMessageContaining(ORDER_CODE.toString());

        then(orderMapper).should(never()).toResponseDTO(any());
    }

    @Test
    @DisplayName("should update order when version matches")
    void shouldUpdateOrder_whenVersionMatches() {
        var updateDTO = new OrderUpdateDTO(
                "John Doe Updated",
                "john.updated@example.com",
                OrderStatus.PROCESSING,
                0L
        );
        var updatedResponseDTO = new OrderResponseDTO(
                ORDER_CODE,
                "John Doe Updated",
                "john.updated@example.com",
                OrderStatus.PROCESSING,
                new BigDecimal("139.97"),
                NOW,
                List.of(),
                NOW,
                NOW,
                1L
        );

        given(orderRepository.findByCodeWithItems(ORDER_CODE)).willReturn(Optional.of(order));
        given(orderRepository.saveAndFlush(order)).willReturn(order);
        given(orderMapper.toResponseDTO(order)).willReturn(updatedResponseDTO);

        var result = orderService.update(ORDER_CODE, updateDTO);

        assertThat(result).isEqualTo(updatedResponseDTO);
        then(orderMapper).should().updateEntityFromDTO(updateDTO, order);
        then(orderRepository).should().saveAndFlush(order);
    }

    @Test
    @DisplayName("should restore stock when order is cancelled")
    void shouldRestoreStock_whenOrderIsCancelled() {
        order.setItems(new ArrayList<>(List.of(orderItem1, orderItem2)));

        var updateDTO = new OrderUpdateDTO(
                "John Doe",
                "john.doe@example.com",
                OrderStatus.CANCELLED,
                0L
        );
        var cancelledResponseDTO = new OrderResponseDTO(
                ORDER_CODE,
                "John Doe",
                "john.doe@example.com",
                OrderStatus.CANCELLED,
                BigDecimal.ZERO,
                NOW,
                List.of(),
                NOW,
                NOW,
                1L
        );

        given(orderRepository.findByCodeWithItems(ORDER_CODE)).willReturn(Optional.of(order));
        given(orderRepository.saveAndFlush(order)).willReturn(order);
        given(orderMapper.toResponseDTO(order)).willReturn(cancelledResponseDTO);

        orderService.update(ORDER_CODE, updateDTO);

        then(stockService).should(times(1)).adjust(1L, -2);
        then(stockService).should(times(1)).adjust(2L, -1);

        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should throw OptimisticLockingFailureException when version is stale")
    void shouldThrowOptimisticLock_whenVersionIsStale() {
        var updateDTO = new OrderUpdateDTO(
                "John Doe Updated",
                "john.updated@example.com",
                OrderStatus.PROCESSING,
                999L
        );

        given(orderRepository.findByCodeWithItems(ORDER_CODE)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.update(ORDER_CODE, updateDTO))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        then(orderMapper).should(never()).updateEntityFromDTO(any(), any());
        then(orderRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("should throw EntityNotFoundException when order does not exist")
    void shouldThrowNotFound_whenUpdatingNonExistentOrder() {
        var updateDTO = new OrderUpdateDTO(
                "John Doe Updated",
                "john.updated@example.com",
                OrderStatus.PROCESSING,
                0L
        );

        given(orderRepository.findByCodeWithItems(ORDER_CODE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.update(ORDER_CODE, updateDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order")
                .hasMessageContaining(ORDER_CODE.toString());
    }

    @Test
    @DisplayName("should restore stock for each item and delete order")
    void shouldRestoreStockAndDelete_whenOrderExists() {
        order.setItems(new ArrayList<>(List.of(orderItem1, orderItem2)));

        given(orderRepository.findByCodeWithItems(ORDER_CODE)).willReturn(Optional.of(order));

        orderService.delete(ORDER_CODE);

        then(stockService).should(times(1)).adjust(1L, -2);
        then(stockService).should(times(1)).adjust(2L, -1);

        then(orderRepository).should().delete(order);
    }

    @Test
    @DisplayName("should throw EntityNotFoundException when order does not exist")
    void shouldThrowNotFound_whenDeletingNonExistentOrder() {
        given(orderRepository.findByCodeWithItems(ORDER_CODE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.delete(ORDER_CODE))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order")
                .hasMessageContaining(ORDER_CODE.toString());

        then(orderRepository).should(never()).delete(any(Order.class));
    }

    @Test
    @DisplayName("should return paged order items for an existing order")
    void shouldReturnPagedItems_whenFindingOrderItems() {
        Pageable pageable = PageRequest.of(0, 10);
        var itemPage = new PageImpl<>(List.of(orderItem1), pageable, 1);
        var itemResponseDTO = new OrderItemResponseDTO(
                ORDER_ITEM_CODE_1, PRODUCT_CODE_1, "Wireless Mouse",
                new BigDecimal("29.99"), 2, NOW, NOW, 0L
        );

        given(orderRepository.findByCode(ORDER_CODE)).willReturn(Optional.of(order));
        given(orderItemRepository.findByOrderCode(ORDER_CODE, pageable)).willReturn(itemPage);
        given(orderItemMapper.toResponseDTO(orderItem1)).willReturn(itemResponseDTO);

        var result = orderService.findOrderItemsByOrderCode(ORDER_CODE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst()).isEqualTo(itemResponseDTO);
        then(orderRepository).should().findByCode(ORDER_CODE);
        then(orderItemRepository).should().findByOrderCode(ORDER_CODE, pageable);
    }
}
