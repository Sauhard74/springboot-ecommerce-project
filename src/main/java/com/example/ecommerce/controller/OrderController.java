package com.example.ecommerce.controller;

import com.example.ecommerce.dto.CreateOrderRequest;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    private final PaymentService paymentService;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Map<String, Object> orderDetails = orderService.getOrderDetails(
                orderService.createOrder(request).getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(orderDetails);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId) {
        Map<String, Object> orderDetails = orderService.getOrderDetails(orderId);
        
        // Add payment information if available
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        if (payment != null) {
            Map<String, Object> paymentInfo = new HashMap<>();
            paymentInfo.put("id", payment.getId());
            paymentInfo.put("status", payment.getStatus().toString());
            paymentInfo.put("amount", payment.getAmount());
            orderDetails.put("payment", paymentInfo);
        } else {
            orderDetails.put("payment", new HashMap<>());
        }
        
        return ResponseEntity.ok(orderDetails);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getOrderHistory(@PathVariable String userId) {
        List<Map<String, Object>> orders = orderService.getOrderHistory(userId);
        return ResponseEntity.ok(orders);
    }
    
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable String orderId) {
        Order order = orderService.cancelOrder(orderId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", order.getId());
        response.put("userId", order.getUserId());
        response.put("totalAmount", order.getTotalAmount());
        response.put("status", order.getStatus().toString());
        response.put("message", "Order cancelled successfully");
        
        return ResponseEntity.ok(response);
    }
}
