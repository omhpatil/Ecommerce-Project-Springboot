package com.ecommerce.service.impl;

import com.ecommerce.dto.OrderDTO;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Product;
import com.ecommerce.event.OrderEvent;
import com.ecommerce.event.OrderEventProducer;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final OrderEventProducer orderEventProducer;

    @Override
    public OrderDTO createOrder(OrderDTO orderDTO) {
        orderDTO.setCreatedAt(LocalDateTime.now());

        // Fetch product entity
        Product product = productRepository.findById(orderDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + orderDTO.getProductId()));

        // Map DTO to Order entity
        Order order = new Order();
        order.setUserId(orderDTO.getUserId());
        order.setProduct(product); // set the product entity
        order.setQuantity(orderDTO.getQuantity());
        order.setTotalPrice(orderDTO.getTotalPrice());
        order.setCreatedAt(orderDTO.getCreatedAt());

        order = orderRepository.save(order);

        // Send Kafka event
        OrderEvent event = new OrderEvent(
                order.getId(),
                order.getUserId(),
                product.getId(),
                order.getQuantity(),
                order.getTotalPrice()
        );
        orderEventProducer.sendOrderEvent(event);

        // Map back to DTO including product name
        OrderDTO responseDTO = mapToDTO(order);
        return responseDTO;
    }

    @Override
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToDTO(order);
    }

    @Override
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        orderRepository.delete(order);
    }

    // Helper method to map Order -> OrderDTO
    private OrderDTO mapToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setProductId(order.getProduct().getId());
        dto.setProductName(order.getProduct().getName()); // include product name
        dto.setQuantity(order.getQuantity());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }
}
