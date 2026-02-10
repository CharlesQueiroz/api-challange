package com.example.ecommerce.mapper;

import com.example.ecommerce.domain.entity.OrderItem;
import com.example.ecommerce.dto.orderitem.OrderItemCreateDTO;
import com.example.ecommerce.dto.orderitem.OrderItemResponseDTO;
import com.example.ecommerce.dto.orderitem.OrderItemUpdateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OrderItemMapper extends CrudMapper<OrderItem, OrderItemCreateDTO, OrderItemUpdateDTO, OrderItemResponseDTO> {

    @Mapping(target = "productCode", source = "product.code")
    OrderItemResponseDTO toResponseDTO(OrderItem orderItem);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    OrderItem toEntity(OrderItemCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDTO(OrderItemUpdateDTO dto, @MappingTarget OrderItem orderItem);
}
