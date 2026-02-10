package com.example.ecommerce.service.impl;

import com.example.ecommerce.exception.EntityNotFoundException;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockServiceImpl implements StockService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void adjust(Long productId, int delta) {
        if (productId == null || delta == 0) {
            return;
        }

        if (delta > 0) {
            decrease(productId, delta);
            return;
        }

        restore(productId, Math.abs(delta));
    }

    private void decrease(Long productId, int quantity) {
        var product = productRepository.findByIdForStockUpdate(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));

        product.decreaseStock(quantity);
        productRepository.save(product);
    }

    private void restore(Long productId, int quantity) {
        productRepository.findByIdForStockUpdate(productId)
                .ifPresentOrElse(
                        product -> {
                            product.increaseStock(quantity);
                            productRepository.save(product);
                        },
                        () -> log.warn("Product with id {} not found for stock restore of {} units", productId, quantity)
                );
    }
}
