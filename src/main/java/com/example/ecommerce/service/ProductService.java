package com.example.ecommerce.service;

import com.example.ecommerce.dto.product.ProductCreateDTO;
import com.example.ecommerce.dto.product.ProductResponseDTO;
import com.example.ecommerce.dto.product.ProductUpdateDTO;
import com.example.ecommerce.service.crud.CrudService;

import java.util.UUID;

public interface ProductService extends CrudService<ProductCreateDTO, ProductUpdateDTO, ProductResponseDTO, UUID> {
}
