package com.example.ecommerce.service.impl;

import com.example.ecommerce.domain.entity.Order;
import com.example.ecommerce.domain.entity.OrderItem;
import com.example.ecommerce.domain.entity.OrderStatus;
import com.example.ecommerce.dto.order.OrderCreateDTO;
import com.example.ecommerce.dto.order.OrderLineDTO;
import com.example.ecommerce.dto.order.OrderResponseDTO;
import com.example.ecommerce.dto.order.OrderUpdateDTO;
import com.example.ecommerce.dto.orderitem.OrderItemResponseDTO;
import com.example.ecommerce.exception.EntityNotFoundException;
import com.example.ecommerce.mapper.OrderItemMapper;
import com.example.ecommerce.mapper.OrderMapper;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.StockService;
import com.example.ecommerce.service.support.CrudEntitySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.example.ecommerce.domain.entity.OrderStatus.CANCELLED;
import static com.example.ecommerce.domain.entity.OrderStatus.COMPLETED;
import static com.example.ecommerce.domain.entity.OrderStatus.PENDING;
import static com.example.ecommerce.domain.entity.OrderStatus.PROCESSING;
import java.math.BigDecimal;

import static java.time.LocalDateTime.now;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final StockService stockService;
    private final OrderItemMapper orderItemMapper;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public OrderResponseDTO create(OrderCreateDTO dto) {
        var productCodes = dto.items().stream().map(OrderLineDTO::productCode).toList();
        if (productCodes.size() != new HashSet<>(productCodes).size()) {
            throw new IllegalArgumentException("Duplicate product codes in order items are not allowed");
        }

        var order = orderMapper.toEntity(dto);
        order.setStatus(PENDING);
        order.setOrderDate(now());
        order.setTotalAmount(BigDecimal.ZERO);

        var items = dto.items()
                .stream()
                .map(lineDto -> {
                    var product = CrudEntitySupport.requireByCode(productRepository, "Product", lineDto.productCode());

                    stockService.adjust(product.getId(), lineDto.quantity());
                    return OrderItem.from(order, product, lineDto.quantity());
                }).toList();
        order.replaceItems(items);

        var saved = orderRepository.save(order);
        return orderMapper.toResponseDTO(saved);
    }

    @Override
    public Page<OrderResponseDTO> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(orderMapper::toResponseDTO);
    }

    @Override
    public OrderResponseDTO findByCode(UUID code) {
        return orderMapper.toResponseDTO(findOrderWithItemsByCode(code));
    }

    @Override
    @Transactional
    public OrderResponseDTO update(UUID code, OrderUpdateDTO dto) {
        var order = findOrderWithItemsByCode(code);
        CrudEntitySupport.requireVersionMatch(order, dto.version(), Order.class);

        var previousStatus = order.getStatus();
        validateStatusTransition(previousStatus, dto.status());
        orderMapper.updateEntityFromDTO(dto, order);

        if (dto.status() == CANCELLED && previousStatus != CANCELLED) {
            restoreStockForItems(order);
            order.setTotalAmount(BigDecimal.ZERO);
        }

        var saved = orderRepository.saveAndFlush(order);
        return orderMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public void delete(UUID code) {
        var order = findOrderWithItemsByCode(code);

        restoreStockForItems(order);

        orderRepository.delete(order);
    }

    @Override
    public Page<OrderItemResponseDTO> findOrderItemsByOrderCode(UUID orderCode, Pageable pageable) {
        CrudEntitySupport.requireByCode(orderRepository, "Order", orderCode);
        return orderItemRepository.findByOrderCode(orderCode, pageable)
                .map(orderItemMapper::toResponseDTO);
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        if (from == to) return;
        var allowed = switch (from) {
            case PENDING -> Set.of(PROCESSING, CANCELLED);
            case PROCESSING -> Set.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED -> Set.of();
        };
        if (!allowed.contains(to)) {
            throw new IllegalStateException("Cannot transition order from %s to %s".formatted(from, to));
        }
    }

    private void restoreStockForItems(Order order) {
        order.getItems().forEach(item -> {
            if (item.getProduct() != null) {
                stockService.adjust(item.getProduct().getId(), -item.getQuantity());
            }
        });
    }

    private Order findOrderWithItemsByCode(UUID code) {
        return orderRepository.findByCodeWithItems(code)
                .orElseThrow(() -> new EntityNotFoundException("Order", code));
    }
}
