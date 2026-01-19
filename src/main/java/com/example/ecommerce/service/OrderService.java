package com.example.ecommerce.service;

import com.example.ecommerce.dto.CreateOrderRequest;
import com.example.ecommerce.model.*;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final ProductService productService;
    
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // Get cart items
        List<CartItem> cartItems = cartService.getUserCart(request.getUserId());
        
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty. Cannot create order.");
        }
        
        // Calculate total and validate stock
        double totalAmount = 0.0;
        for (CartItem cartItem : cartItems) {
            Product product = productService.getProductById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + cartItem.getProductId()));
            
            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            
            totalAmount += product.getPrice() * cartItem.getQuantity();
        }
        
        // Create order
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setUserId(request.getUserId());
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(Instant.now());
        order = orderRepository.save(order);
        
        // Create order items and update stock
        for (CartItem cartItem : cartItems) {
            Product product = productService.getProductById(cartItem.getProductId()).orElseThrow();
            
            OrderItem orderItem = new OrderItem();
            orderItem.setId(UUID.randomUUID().toString());
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItemRepository.save(orderItem);
            
            // Update product stock
            productService.updateProductStock(cartItem.getProductId(), cartItem.getQuantity());
        }
        
        // Clear cart
        cartService.clearCart(request.getUserId());
        
        return order;
    }
    
    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
    
    @Transactional
    public void updateOrderStatus(String orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        order.setStatus(status);
        orderRepository.save(order);
    }
    
    @Transactional
    public Order cancelOrder(String orderId) {
        Order order = getOrderById(orderId);
        
        if (order.getStatus() == OrderStatus.PAID) {
            throw new RuntimeException("Cannot cancel a paid order");
        }
        
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled");
        }
        
        // Restore stock for all order items
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem orderItem : orderItems) {
            Product product = productService.getProductById(orderItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + orderItem.getProductId()));
            product.setStock(product.getStock() + orderItem.getQuantity());
            productService.createProduct(product); // Update product
        }
        
        // Update order status
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
    
    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }
    
    public List<Map<String, Object>> getOrderHistory(String userId) {
        List<Order> orders = getOrdersByUserId(userId);
        return orders.stream()
                .map(order -> {
                    List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
                    Map<String, Object> orderMap = new HashMap<>();
                    orderMap.put("id", order.getId());
                    orderMap.put("userId", order.getUserId());
                    orderMap.put("totalAmount", order.getTotalAmount());
                    orderMap.put("status", order.getStatus().toString());
                    orderMap.put("createdAt", order.getCreatedAt());
                    
                    List<Map<String, Object>> items = orderItems.stream()
                            .map(item -> {
                                Map<String, Object> itemMap = new HashMap<>();
                                itemMap.put("productId", item.getProductId());
                                itemMap.put("quantity", item.getQuantity());
                                itemMap.put("price", item.getPrice());
                                return itemMap;
                            })
                            .toList();
                    orderMap.put("items", items);
                    return orderMap;
                })
                .toList();
    }
    
    public Map<String, Object> getOrderDetails(String orderId) {
        Order order = getOrderById(orderId);
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", order.getId());
        response.put("userId", order.getUserId());
        response.put("totalAmount", order.getTotalAmount());
        response.put("status", order.getStatus());
        response.put("createdAt", order.getCreatedAt());
        
        // Add order items
        List<Map<String, Object>> items = orderItems.stream()
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("productId", item.getProductId());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("price", item.getPrice());
                    return itemMap;
                })
                .toList();
        response.put("items", items);
        
        return response;
    }
}
