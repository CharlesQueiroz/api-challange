package com.example.ecommerce.service.impl;

import com.example.ecommerce.domain.entity.Product;
import com.example.ecommerce.dto.product.ProductCreateDTO;
import com.example.ecommerce.dto.product.ProductResponseDTO;
import com.example.ecommerce.dto.product.ProductUpdateDTO;
import com.example.ecommerce.exception.DuplicateResourceException;
import com.example.ecommerce.mapper.ProductMapper;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.service.ProductService;
import com.example.ecommerce.service.crud.MappedCrudService;
import com.example.ecommerce.service.support.CrudEntitySupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl extends MappedCrudService<Product, ProductCreateDTO, ProductUpdateDTO, ProductResponseDTO> implements ProductService {

    private static final String ENTITY_NAME = "Product";

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper) {
        super(productRepository, productMapper, Product.class, "Product");
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public ProductResponseDTO create(ProductCreateDTO createDto) {
        var normalizedName = normalizeName(createDto.name());
        assertProductNameIsUniqueForCreate(normalizedName);

        var entity = mapper().toEntity(createDto);
        entity.setName(normalizedName);
        var saved = productRepository.save(entity);

        return mapper().toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ProductResponseDTO update(UUID code, ProductUpdateDTO updateDto) {
        var product = findEntityByCode(code);
        CrudEntitySupport.requireVersionMatch(product, updateDto.version(), Product.class);

        var normalizedName = normalizeName(updateDto.name());
        assertProductNameIsUniqueForUpdate(normalizedName, product.getId());

        mapper().updateEntityFromDTO(updateDto, product);
        product.setName(normalizedName);
        var saved = productRepository.saveAndFlush(product);

        return mapper().toResponseDTO(saved);
    }

    private void assertProductNameIsUniqueForCreate(String normalizedName) {
        if (productRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new DuplicateResourceException(ENTITY_NAME, "name", normalizedName);
        }
    }

    private void assertProductNameIsUniqueForUpdate(String normalizedName, Long productId) {
        if (productRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, productId)) {
            throw new DuplicateResourceException(ENTITY_NAME, "name", normalizedName);
        }
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }
}
