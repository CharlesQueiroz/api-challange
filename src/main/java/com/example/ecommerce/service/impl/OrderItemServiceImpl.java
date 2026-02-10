package com.example.ecommerce.service.impl;

import com.example.ecommerce.domain.entity.OrderItem;
import com.example.ecommerce.dto.orderitem.OrderItemCreateDTO;
import com.example.ecommerce.dto.orderitem.OrderItemResponseDTO;
import com.example.ecommerce.dto.orderitem.OrderItemUpdateDTO;
import com.example.ecommerce.mapper.OrderItemMapper;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.service.OrderItemService;
import com.example.ecommerce.service.StockService;
import com.example.ecommerce.service.crud.MappedCrudService;
import com.example.ecommerce.service.support.CrudEntitySupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderItemServiceImpl extends MappedCrudService<OrderItem, OrderItemCreateDTO, OrderItemUpdateDTO, OrderItemResponseDTO> implements OrderItemService {

    public static final String ENTITY_NAME = "OrderItem";

    private final StockService stockService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderItemServiceImpl(
            OrderRepository orderRepository,
            OrderItemMapper orderItemMapper,
            ProductRepository productRepository,
            OrderItemRepository orderItemRepository,
            StockService stockService
    ) {
        super(orderItemRepository, orderItemMapper, OrderItem.class, ENTITY_NAME);
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.stockService = stockService;
    }

    @Override
    @Transactional
    public OrderItemResponseDTO create(OrderItemCreateDTO dto) {
        var order = CrudEntitySupport.requireByCode(orderRepository, "Order", dto.orderCode());
        var product = CrudEntitySupport.requireByCode(productRepository, "Product", dto.productCode());
        var orderItem = OrderItem.from(order, product, dto.quantity());

        stockService.adjust(product.getId(), dto.quantity());
        var saved = orderItemRepository.save(orderItem);

        order.addItem(saved);
        orderRepository.save(order);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public OrderItemResponseDTO update(UUID code, OrderItemUpdateDTO dto) {
        var orderItem = findEntityByCode(code);
        CrudEntitySupport.requireVersionMatch(orderItem, dto.version(), OrderItem.class);

        var delta = orderItem.quantityDeltaTo(dto.quantity());
        stockService.adjust(orderItem.getProduct() != null ? orderItem.getProduct().getId() : null, delta);

        mapper().updateEntityFromDTO(dto, orderItem);
        var saved = orderItemRepository.saveAndFlush(orderItem);

        var order = saved.getOrder();
        order.recalculateTotalAmount();
        orderRepository.save(order);

        return toResponse(saved);
    }

    @Override
    protected void beforeDelete(OrderItem entity) {
        stockService.adjust(entity.getProduct() != null ? entity.getProduct().getId() : null, -entity.getQuantity());
        entity.getOrder().removeItem(entity);
    }

    @Override
    protected void afterDelete(OrderItem entity) {
        orderRepository.save(entity.getOrder());
    }
}
