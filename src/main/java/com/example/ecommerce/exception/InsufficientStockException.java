package com.example.ecommerce.exception;

import lombok.Getter;

@Getter
public class InsufficientStockException extends RuntimeException {

    private final String productName;
    private final int availableStock;
    private final int requestedQuantity;

    public InsufficientStockException(String productName, int availableStock, int requestedQuantity) {
        super("Insufficient stock for product '%s': available=%d, requested=%d".formatted(productName, availableStock, requestedQuantity));
        this.productName = productName;
        this.availableStock = availableStock;
        this.requestedQuantity = requestedQuantity;
    }
}
