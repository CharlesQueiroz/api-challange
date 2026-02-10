package com.example.ecommerce.repository;

import com.example.ecommerce.domain.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderItemRepository extends CodeRepository<OrderItem> {

    Page<OrderItem> findByOrderCode(UUID orderCode, Pageable pageable);
}
