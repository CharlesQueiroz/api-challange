package com.example.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.ecommerce.domain.entity.Order;
import com.example.ecommerce.domain.entity.OrderItem;
import com.example.ecommerce.domain.entity.OrderStatus;
import com.example.ecommerce.domain.entity.Product;
import com.example.ecommerce.dto.orderitem.OrderItemCreateDTO;
import com.example.ecommerce.dto.orderitem.OrderItemResponseDTO;
import com.example.ecommerce.dto.orderitem.OrderItemUpdateDTO;
import com.example.ecommerce.exception.EntityNotFoundException;
import com.example.ecommerce.mapper.OrderItemMapper;
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
class OrderItemServiceImplTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private StockService stockService;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    private static final UUID ORDER_CODE = UUID.randomUUID();
    private static final UUID PRODUCT_CODE = UUID.randomUUID();
    private static final UUID ORDER_ITEM_CODE = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.now();

    private Product product;
    private Order order;
    private OrderItem orderItem;
    private OrderItemResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .code(PRODUCT_CODE)
                .version(0L)
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .price(new BigDecimal("29.99"))
                .stockQuantity(150)
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
                .totalAmount(new BigDecimal("59.98"))
                .orderDate(NOW)
                .items(new ArrayList<>())
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        orderItem = OrderItem.builder()
                .id(1L)
                .code(ORDER_ITEM_CODE)
                .version(0L)
                .order(order)
                .product(product)
                .productName("Wireless Mouse")
                .unitPrice(new BigDecimal("29.99"))
                .quantity(2)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        order.getItems().add(orderItem);

        responseDTO = new OrderItemResponseDTO(
                ORDER_ITEM_CODE,
                PRODUCT_CODE,
                "Wireless Mouse",
                new BigDecimal("29.99"),
                2,
                NOW,
                NOW,
                0L
        );
    }

    @Test
    @DisplayName("should create order item with product snapshot and recalculate total")
    void shouldCreateOrderItem_whenDtoIsValid() {
        var createDTO = new OrderItemCreateDTO(PRODUCT_CODE, 3, ORDER_CODE);
        var newItem = OrderItem.builder()
                .id(2L)
                .code(UUID.randomUUID())
                .version(0L)
                .order(order)
                .product(product)
                .productName("Wireless Mouse")
                .unitPrice(new BigDecimal("29.99"))
                .quantity(3)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        given(orderRepository.findByCode(ORDER_CODE)).willReturn(Optional.of(order));
        given(productRepository.findByCode(PRODUCT_CODE)).willReturn(Optional.of(product));
        given(orderItemRepository.save(any(OrderItem.class))).willReturn(newItem);
        given(orderItemMapper.toResponseDTO(newItem)).willReturn(responseDTO);
        given(orderRepository.save(order)).willReturn(order);

        var result = orderItemService.create(createDTO);

        assertThat(result).isEqualTo(responseDTO);
        then(orderItemRepository).should().save(any(OrderItem.class));
        then(orderRepository).should().save(order);
    }

    @Test
    @DisplayName("should adjust stock on create")
    void shouldAdjustStock_whenOrderItemIsCreated() {
        var createDTO = new OrderItemCreateDTO(PRODUCT_CODE, 3, ORDER_CODE);
        var newItem = OrderItem.builder()
                .id(2L)
                .code(UUID.randomUUID())
                .version(0L)
                .order(order)
                .product(product)
                .productName("Wireless Mouse")
                .unitPrice(new BigDecimal("29.99"))
                .quantity(3)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        given(orderRepository.findByCode(ORDER_CODE)).willReturn(Optional.of(order));
        given(productRepository.findByCode(PRODUCT_CODE)).willReturn(Optional.of(product));
        given(orderItemRepository.save(any(OrderItem.class))).willReturn(newItem);
        given(orderItemMapper.toResponseDTO(newItem)).willReturn(responseDTO);
        given(orderRepository.save(order)).willReturn(order);

        orderItemService.create(createDTO);

        then(stockService).should().adjust(1L, 3);
    }

    @Test
    @DisplayName("should return paged results")
    void shouldReturnPagedResults_whenFindingAll() {
        Pageable pageable = PageRequest.of(0, 10);
        var itemPage = new PageImpl<>(List.of(orderItem), pageable, 1);

        given(orderItemRepository.findAll(pageable)).willReturn(itemPage);
        given(orderItemMapper.toResponseDTO(orderItem)).willReturn(responseDTO);

        var result = orderItemService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst()).isEqualTo(responseDTO);
        assertThat(result.getTotalElements()).isEqualTo(1);
        then(orderItemRepository).should().findAll(pageable);
    }

    @Test
    @DisplayName("should return DTO when order item exists")
    void shouldReturnDto_whenCodeExists() {
        given(orderItemRepository.findByCode(ORDER_ITEM_CODE)).willReturn(Optional.of(orderItem));
        given(orderItemMapper.toResponseDTO(orderItem)).willReturn(responseDTO);

        var result = orderItemService.findByCode(ORDER_ITEM_CODE);

        assertThat(result).isEqualTo(responseDTO);
        then(orderItemRepository).should().findByCode(ORDER_ITEM_CODE);
    }

    @Test
    @DisplayName("should throw EntityNotFoundException when order item does not exist")
    void shouldThrowNotFound_whenCodeDoesNotExist() {
        given(orderItemRepository.findByCode(ORDER_ITEM_CODE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.findByCode(ORDER_ITEM_CODE))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("OrderItem")
                .hasMessageContaining(ORDER_ITEM_CODE.toString());

        then(orderItemMapper).should(never()).toResponseDTO(any());
    }

    @Test
    @DisplayName("should decrease stock when quantity increases")
    void shouldDecreaseStock_whenQuantityIncreases() {
        var updateDTO = new OrderItemUpdateDTO(5, 0L);
        var updatedItem = OrderItem.builder()
                .id(1L)
                .code(ORDER_ITEM_CODE)
                .version(1L)
                .order(order)
                .product(product)
                .productName("Wireless Mouse")
                .unitPrice(new BigDecimal("29.99"))
                .quantity(5)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        var updatedResponseDTO = new OrderItemResponseDTO(
                ORDER_ITEM_CODE, PRODUCT_CODE, "Wireless Mouse",
                new BigDecimal("29.99"), 5, NOW, NOW, 1L
        );

        given(orderItemRepository.findByCode(ORDER_ITEM_CODE)).willReturn(Optional.of(orderItem));
        given(orderItemRepository.saveAndFlush(orderItem)).willReturn(updatedItem);
        given(orderItemMapper.toResponseDTO(updatedItem)).willReturn(updatedResponseDTO);
        given(orderRepository.save(order)).willReturn(order);

        var result = orderItemService.update(ORDER_ITEM_CODE, updateDTO);

        assertThat(result).isEqualTo(updatedResponseDTO);
        then(stockService).should().adjust(1L, 3);
        then(orderItemMapper).should().updateEntityFromDTO(updateDTO, orderItem);
    }

    @Test
    @DisplayName("should restore stock when quantity decreases")
    void shouldRestoreStock_whenQuantityDecreases() {
        var updateDTO = new OrderItemUpdateDTO(1, 0L);
        var updatedItem = OrderItem.builder()
                .id(1L)
                .code(ORDER_ITEM_CODE)
                .version(1L)
                .order(order)
                .product(product)
                .productName("Wireless Mouse")
                .unitPrice(new BigDecimal("29.99"))
                .quantity(1)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        var updatedResponseDTO = new OrderItemResponseDTO(
                ORDER_ITEM_CODE, PRODUCT_CODE, "Wireless Mouse",
                new BigDecimal("29.99"), 1, NOW, NOW, 1L
        );

        given(orderItemRepository.findByCode(ORDER_ITEM_CODE)).willReturn(Optional.of(orderItem));
        given(orderItemRepository.saveAndFlush(orderItem)).willReturn(updatedItem);
        given(orderItemMapper.toResponseDTO(updatedItem)).willReturn(updatedResponseDTO);
        given(orderRepository.save(order)).willReturn(order);

        var result = orderItemService.update(ORDER_ITEM_CODE, updateDTO);

        assertThat(result).isEqualTo(updatedResponseDTO);
        then(stockService).should().adjust(1L, -1);
    }

    @Test
    @DisplayName("should throw OptimisticLockingFailureException when version is stale")
    void shouldThrowOptimisticLock_whenVersionIsStale() {
        var updateDTO = new OrderItemUpdateDTO(5, 999L);

        given(orderItemRepository.findByCode(ORDER_ITEM_CODE)).willReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> orderItemService.update(ORDER_ITEM_CODE, updateDTO))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        then(orderItemMapper).should(never()).updateEntityFromDTO(any(), any());
        then(orderItemRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("should restore stock, recalculate total, and delete item")
    void shouldRestoreStockAndRecalculate_whenItemIsDeleted() {
        given(orderItemRepository.findByCode(ORDER_ITEM_CODE)).willReturn(Optional.of(orderItem));

        orderItemService.delete(ORDER_ITEM_CODE);

        then(stockService).should().adjust(1L, -2);

        then(orderItemRepository).should().delete(orderItem);
        then(orderRepository).should().save(order);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should throw EntityNotFoundException when order item does not exist")
    void shouldThrowNotFound_whenDeletingNonExistentItem() {
        given(orderItemRepository.findByCode(ORDER_ITEM_CODE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderItemService.delete(ORDER_ITEM_CODE))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("OrderItem")
                .hasMessageContaining(ORDER_ITEM_CODE.toString());

        then(orderItemRepository).should(never()).delete(any());
        then(stockService).should(never()).adjust(any(), anyInt());
    }
}
