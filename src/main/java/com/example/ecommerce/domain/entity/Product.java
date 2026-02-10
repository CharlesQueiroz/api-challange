package com.example.ecommerce.domain.entity;

import com.example.ecommerce.exception.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product")
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    public void decreaseStock(int quantity) {
        requirePositiveQuantity(quantity);

        var available = availableStock();
        var newStock = available - quantity;
        if (newStock < 0) {
            throw new InsufficientStockException(name, available, quantity);
        }

        stockQuantity = newStock;
    }

    public void increaseStock(int quantity) {
        requirePositiveQuantity(quantity);
        stockQuantity = availableStock() + quantity;
    }

    private int availableStock() {
        return stockQuantity == null ? 0 : stockQuantity;
    }

    private void requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }
}
