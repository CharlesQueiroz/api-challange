package com.example.ecommerce.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.EnumType.*;
import static java.math.BigDecimal.*;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        if (item == null) {
            return;
        }

        attachItem(item);
        recalculateTotalAmount();
    }

    public void replaceItems(Collection<OrderItem> newItems) {
        items.clear();
        if (newItems != null) {
            newItems.forEach(this::attachItem);
        }
        recalculateTotalAmount();
    }

    public void removeItem(OrderItem item) {
        if (item == null) {
            return;
        }

        items.remove(item);
        recalculateTotalAmount();
    }

    public void recalculateTotalAmount() {
        totalAmount = items.stream()
                .map(OrderItem::lineTotal)
                .reduce(ZERO, BigDecimal::add);
    }

    private void attachItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }
}
