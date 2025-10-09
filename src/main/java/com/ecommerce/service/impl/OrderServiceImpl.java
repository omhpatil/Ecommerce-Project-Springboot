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

//    @Override
//    public OrderDTO createOrder(OrderDTO orderDTO) {
//        orderDTO.setCreatedAt(LocalDateTime.now());
//        Order order = modelMapper.map(orderDTO, Order.class);
//        order = orderRepository.save(order);
//
//        // Send Kafka event
//        OrderEvent event = new OrderEvent(
//                order.getId(),
//                order.getUserId(),
//                order.getProductId(),
//                order.getQuantity(),
//                order.getTotalPrice()
//        );
//        orderEventProducer.sendOrderEvent(event);
//
//        return modelMapper.map(order, OrderDTO.class);
//    }

    @Override
    public OrderDTO createOrder(OrderDTO orderDTO) {
        // 1️⃣ Fetch product from DB
        Product product = productRepository.findById(orderDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + orderDTO.getProductId()));

        // 2️⃣ Check stock availability
        if (product.getQuantity() < orderDTO.getQuantity()) {
            throw new RuntimeException("Insufficient stock for product: " + product.getName());
        }

        // 3️⃣ Update product stock
        product.setQuantity(product.getQuantity() - orderDTO.getQuantity());
        productRepository.save(product); // save updated stock

        // 4️⃣ Set order creation time
        orderDTO.setCreatedAt(LocalDateTime.now());

        // 5️⃣ Save order
        Order order = modelMapper.map(orderDTO, Order.class);
        order = orderRepository.save(order);

        // 6️⃣ Send Kafka event
        OrderEvent event = new OrderEvent(
                order.getId(),
                order.getUserId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalPrice()
        );
        orderEventProducer.sendOrderEvent(event);

        return modelMapper.map(order, OrderDTO.class);
    }


    @Override
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return modelMapper.map(order, OrderDTO.class);
    }

    @Override
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> modelMapper.map(order, OrderDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        orderRepository.delete(order);
    }
}
