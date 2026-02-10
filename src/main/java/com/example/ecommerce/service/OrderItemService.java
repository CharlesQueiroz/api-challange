package com.example.ecommerce.service;

import com.example.ecommerce.dto.orderitem.OrderItemCreateDTO;
import com.example.ecommerce.dto.orderitem.OrderItemResponseDTO;
import com.example.ecommerce.dto.orderitem.OrderItemUpdateDTO;
import com.example.ecommerce.service.crud.CrudService;

import java.util.UUID;

public interface OrderItemService extends CrudService<OrderItemCreateDTO, OrderItemUpdateDTO, OrderItemResponseDTO, UUID> {
}
