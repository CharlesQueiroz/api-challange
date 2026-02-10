package com.example.ecommerce.service;

import com.example.ecommerce.dto.order.OrderCreateDTO;
import com.example.ecommerce.dto.order.OrderResponseDTO;
import com.example.ecommerce.dto.order.OrderUpdateDTO;
import com.example.ecommerce.dto.orderitem.OrderItemResponseDTO;
import com.example.ecommerce.service.crud.CrudService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService extends CrudService<OrderCreateDTO, OrderUpdateDTO, OrderResponseDTO, UUID> {

    Page<OrderItemResponseDTO> findOrderItemsByOrderCode(UUID orderCode, Pageable pageable);
}
