package com.example.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.ecommerce.domain.entity.Product;
import com.example.ecommerce.exception.EntityNotFoundException;
import com.example.ecommerce.exception.InsufficientStockException;
import com.example.ecommerce.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StockServiceImpl stockService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .code(UUID.randomUUID())
                .version(0L)
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .price(new BigDecimal("29.99"))
                .stockQuantity(100)
                .build();
    }

    @Test
    @DisplayName("adjust should decrease stock when delta is positive")
    void shouldDecreaseStock_whenDeltaIsPositive() {
        given(productRepository.findByIdForStockUpdate(1L)).willReturn(Optional.of(product));

        stockService.adjust(1L, 30);

        assertThat(product.getStockQuantity()).isEqualTo(70);
        then(productRepository).should().save(product);
    }

    @Test
    @DisplayName("adjust should restore stock when delta is negative")
    void shouldRestoreStock_whenDeltaIsNegative() {
        given(productRepository.findByIdForStockUpdate(1L)).willReturn(Optional.of(product));

        stockService.adjust(1L, -25);

        assertThat(product.getStockQuantity()).isEqualTo(125);
        then(productRepository).should().save(product);
    }

    @Test
    @DisplayName("adjust should throw InsufficientStockException when stock is not enough")
    void shouldThrowException_whenStockIsInsufficient() {
        product.setStockQuantity(5);
        given(productRepository.findByIdForStockUpdate(1L)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> stockService.adjust(1L, 10))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("available=5")
                .hasMessageContaining("requested=10");

        then(productRepository).should(never()).save(any(Product.class));
    }

    @Test
    @DisplayName("adjust should throw EntityNotFoundException when product is missing on decrement")
    void shouldThrowException_whenProductIsMissingOnDecrement() {
        given(productRepository.findByIdForStockUpdate(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.adjust(99L, 1))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining("99");

        then(productRepository).should(never()).save(any(Product.class));
    }

    @Test
    @DisplayName("adjust should ignore missing product on restore")
    void shouldDoNothing_whenProductIsMissingOnRestore() {
        given(productRepository.findByIdForStockUpdate(99L)).willReturn(Optional.empty());

        stockService.adjust(99L, -10);

        then(productRepository).should(never()).save(any(Product.class));
    }

    @Test
    @DisplayName("adjust should ignore null product id or zero delta")
    void shouldIgnore_whenProductIdIsNullOrDeltaIsZero() {
        stockService.adjust(null, 10);
        stockService.adjust(1L, 0);

        then(productRepository).should(never()).findByIdForStockUpdate(any(Long.class));
        then(productRepository).should(never()).save(any(Product.class));
    }
}
