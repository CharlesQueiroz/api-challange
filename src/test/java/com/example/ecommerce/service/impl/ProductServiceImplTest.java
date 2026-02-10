package com.example.ecommerce.service.impl;

import static java.time.LocalDateTime.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.ecommerce.domain.entity.Product;
import com.example.ecommerce.dto.product.ProductCreateDTO;
import com.example.ecommerce.dto.product.ProductResponseDTO;
import com.example.ecommerce.dto.product.ProductUpdateDTO;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.exception.EntityNotFoundException;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private static final UUID PRODUCT_CODE = UUID.randomUUID();
    private static final LocalDateTime NOW = now();

    private Product product;
    private ProductResponseDTO responseDTO;

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

        responseDTO = new ProductResponseDTO(
                PRODUCT_CODE,
                "Wireless Mouse",
                "Ergonomic wireless mouse",
                new BigDecimal("29.99"),
                150,
                NOW,
                NOW,
                0L
        );
    }

    @Test
    @DisplayName("should create product and return response DTO")
    void shouldCreateProduct_whenDtoIsValid() {
        var createDTO = new ProductCreateDTO(
                "Wireless Mouse",
                "Ergonomic wireless mouse",
                new BigDecimal("29.99"),
                150
        );

        given(productRepository.existsByNameIgnoreCase("Wireless Mouse")).willReturn(false);
        given(productMapper.toEntity(createDTO)).willReturn(product);
        given(productRepository.save(product)).willReturn(product);
        given(productMapper.toResponseDTO(product)).willReturn(responseDTO);

        var result = productService.create(createDTO);

        assertThat(result).isEqualTo(responseDTO);
        then(productMapper).should().toEntity(createDTO);
        then(productRepository).should().save(product);
        then(productMapper).should().toResponseDTO(product);
    }

    @Test
    @DisplayName("should reject product creation when name already exists")
    void shouldThrowDuplicateResource_whenNameAlreadyExists() {
        var createDTO = new ProductCreateDTO(
                "Wireless Mouse",
                "Ergonomic wireless mouse",
                new BigDecimal("29.99"),
                150
        );

        given(productRepository.existsByNameIgnoreCase("Wireless Mouse")).willReturn(true);

        assertThatThrownBy(() -> productService.create(createDTO))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining("name")
                .hasMessageContaining("Wireless Mouse");

        then(productMapper).should(never()).toEntity(any());
        then(productRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("should return paged results")
    void shouldReturnPagedResults_whenFindingAll() {
        var pageable = PageRequest.of(0, 10);
        var productPage = new PageImpl<>(List.of(product), pageable, 1);

        given(productRepository.findAll(pageable)).willReturn(productPage);
        given(productMapper.toResponseDTO(product)).willReturn(responseDTO);

        var result = productService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst()).isEqualTo(responseDTO);
        assertThat(result.getTotalElements()).isEqualTo(1);
        then(productRepository).should().findAll(pageable);
    }

    @Test
    @DisplayName("should return DTO when product exists")
    void shouldReturnDto_whenCodeExists() {
        given(productRepository.findByCode(PRODUCT_CODE)).willReturn(Optional.of(product));
        given(productMapper.toResponseDTO(product)).willReturn(responseDTO);

        var result = productService.findByCode(PRODUCT_CODE);

        assertThat(result).isEqualTo(responseDTO);
        then(productRepository).should().findByCode(PRODUCT_CODE);
    }

    @Test
    @DisplayName("should throw EntityNotFoundException when product does not exist")
    void shouldThrowNotFound_whenCodeDoesNotExist() {
        given(productRepository.findByCode(PRODUCT_CODE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findByCode(PRODUCT_CODE))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining(PRODUCT_CODE.toString());

        then(productMapper).should(never()).toResponseDTO(any());
    }

    @Test
    @DisplayName("should update product when version matches")
    void shouldUpdateProduct_whenVersionMatches() {
        var updateDTO = new ProductUpdateDTO(
                "Wireless Mouse Pro",
                "Updated ergonomic mouse",
                new BigDecimal("39.99"),
                200,
                0L
        );
        var updatedProduct = Product.builder()
                .id(1L)
                .code(PRODUCT_CODE)
                .version(1L)
                .name("Wireless Mouse Pro")
                .description("Updated ergonomic mouse")
                .price(new BigDecimal("39.99"))
                .stockQuantity(200)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        var updatedResponseDTO = new ProductResponseDTO(
                PRODUCT_CODE,
                "Wireless Mouse Pro",
                "Updated ergonomic mouse",
                new BigDecimal("39.99"),
                200,
                NOW,
                NOW,
                1L
        );

        given(productRepository.findByCode(PRODUCT_CODE)).willReturn(Optional.of(product));
        given(productRepository.existsByNameIgnoreCaseAndIdNot("Wireless Mouse Pro", 1L)).willReturn(false);
        given(productRepository.saveAndFlush(product)).willReturn(updatedProduct);
        given(productMapper.toResponseDTO(updatedProduct)).willReturn(updatedResponseDTO);

        var result = productService.update(PRODUCT_CODE, updateDTO);

        assertThat(result).isEqualTo(updatedResponseDTO);
        assertThat(result.version()).isEqualTo(1L);
        then(productMapper).should().updateEntityFromDTO(updateDTO, product);
        then(productRepository).should().saveAndFlush(product);
    }

    @Test
    @DisplayName("should reject product update when name already exists on another product")
    void shouldThrowDuplicateResource_whenUpdatingToExistingName() {
        var updateDTO = new ProductUpdateDTO(
                "Wireless Mouse Pro",
                "Updated ergonomic mouse",
                new BigDecimal("39.99"),
                200,
                0L
        );

        given(productRepository.findByCode(PRODUCT_CODE)).willReturn(Optional.of(product));
        given(productRepository.existsByNameIgnoreCaseAndIdNot("Wireless Mouse Pro", 1L)).willReturn(true);

        assertThatThrownBy(() -> productService.update(PRODUCT_CODE, updateDTO))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining("name")
                .hasMessageContaining("Wireless Mouse Pro");

        then(productMapper).should(never()).updateEntityFromDTO(any(), any());
        then(productRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("should throw OptimisticLockingFailureException when version is stale")
    void shouldThrowOptimisticLock_whenVersionIsStale() {
        var updateDTO = new ProductUpdateDTO(
                "Wireless Mouse Pro",
                "Updated ergonomic mouse",
                new BigDecimal("39.99"),
                200,
                999L
        );

        given(productRepository.findByCode(PRODUCT_CODE)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.update(PRODUCT_CODE, updateDTO))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        then(productMapper).should(never()).updateEntityFromDTO(any(), any());
        then(productRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("should throw EntityNotFoundException when product does not exist")
    void shouldThrowNotFound_whenUpdatingNonExistentProduct() {
        var updateDTO = new ProductUpdateDTO(
                "Wireless Mouse Pro",
                "Updated ergonomic mouse",
                new BigDecimal("39.99"),
                200,
                0L
        );

        given(productRepository.findByCode(PRODUCT_CODE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(PRODUCT_CODE, updateDTO))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining(PRODUCT_CODE.toString());

        then(productMapper).should(never()).updateEntityFromDTO(any(), any());
    }

    @Test
    @DisplayName("should delete product when it exists")
    void shouldDeleteProduct_whenCodeExists() {
        given(productRepository.findByCode(PRODUCT_CODE)).willReturn(Optional.of(product));

        productService.delete(PRODUCT_CODE);

        then(productRepository).should().delete(product);
    }

    @Test
    @DisplayName("should throw EntityNotFoundException when product does not exist")
    void shouldThrowNotFound_whenDeletingNonExistentProduct() {
        given(productRepository.findByCode(PRODUCT_CODE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(PRODUCT_CODE))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining(PRODUCT_CODE.toString());

        then(productRepository).should(never()).delete(any());
    }
}
