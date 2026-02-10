package com.example.ecommerce.repository;

import com.example.ecommerce.domain.entity.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends CodeRepository<Order> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.code = :code")
    Optional<Order> findByCodeWithItems(@Param("code") UUID code);
}
