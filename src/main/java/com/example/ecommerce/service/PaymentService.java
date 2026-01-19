package com.example.ecommerce.service;

import com.example.ecommerce.client.PaymentServiceClient;
import com.example.ecommerce.dto.PaymentRequest;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.OrderStatus;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.model.PaymentStatus;
import com.example.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final PaymentServiceClient paymentServiceClient;
    private final RazorpayService razorpayService;
    
    @Value("${payment.provider:mock}")
    private String paymentProvider;
    
    @Transactional
    public Map<String, Object> createPayment(PaymentRequest request) {
        // Validate order exists and is in CREATED status
        Order order = orderService.getOrderById(request.getOrderId());
        
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new RuntimeException("Order is not in CREATED status. Current status: " + order.getStatus());
        }
        
        if (!order.getTotalAmount().equals(request.getAmount())) {
            throw new RuntimeException("Payment amount does not match order amount");
        }
        
        // Create payment record
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID().toString());
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(Instant.now());
        payment = paymentRepository.save(payment);
        
        // Build response based on payment provider
        Map<String, Object> response = new HashMap<>();
        
        if ("razorpay".equalsIgnoreCase(paymentProvider)) {
            // Create Razorpay order
            Map<String, Object> razorpayResponse = razorpayService.createRazorpayOrder(request);
            payment.setPaymentId((String) razorpayResponse.get("razorpayOrderId"));
            payment = paymentRepository.save(payment);
            
            response.put("paymentId", payment.getId());
            response.put("razorpayOrderId", razorpayResponse.get("razorpayOrderId"));
            response.put("orderId", payment.getOrderId());
            response.put("amount", payment.getAmount());
            response.put("status", payment.getStatus().toString());
            response.put("keyId", razorpayResponse.get("keyId"));
            response.put("currency", razorpayResponse.get("currency"));
        } else {
            // Call mock payment service
            Map<String, Object> paymentResponse = paymentServiceClient.createPayment(request);
            
            // Update payment with external payment ID
            if (paymentResponse != null && paymentResponse.containsKey("paymentId")) {
                payment.setPaymentId((String) paymentResponse.get("paymentId"));
                payment = paymentRepository.save(payment);
            }
            
            response.put("paymentId", payment.getPaymentId() != null ? payment.getPaymentId() : payment.getId());
            response.put("orderId", payment.getOrderId());
            response.put("amount", payment.getAmount());
            response.put("status", payment.getStatus().toString());
        }
        
        return response;
    }
    
    @Transactional
    public void handlePaymentWebhook(String orderId, String paymentId, String status) {
        // Update payment status
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
        
        PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
        payment.setStatus(paymentStatus);
        payment.setPaymentId(paymentId);
        paymentRepository.save(payment);
        
        // Update order status
        if (paymentStatus == PaymentStatus.SUCCESS) {
            orderService.updateOrderStatus(orderId, OrderStatus.PAID);
        } else if (paymentStatus == PaymentStatus.FAILED) {
            orderService.updateOrderStatus(orderId, OrderStatus.FAILED);
        }
    }
    
    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId).orElse(null);
    }
    
    @Transactional
    public void handleRazorpayWebhook(String razorpayOrderId, String razorpayPaymentId, String status) {
        // Find payment by razorpay order ID (stored as paymentId field)
        Payment payment = paymentRepository.findByPaymentId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for Razorpay order: " + razorpayOrderId));
        
        PaymentStatus paymentStatus = PaymentStatus.valueOf(status);
        payment.setStatus(paymentStatus);
        // Store both Razorpay order ID and payment ID (payment ID is the actual payment transaction ID)
        // Format: "order_xyz|pay_abc" or just use payment ID if we only need one
        payment.setPaymentId(razorpayPaymentId != null ? razorpayPaymentId : razorpayOrderId);
        paymentRepository.save(payment);
        
        // Update order status
        if (paymentStatus == PaymentStatus.SUCCESS) {
            orderService.updateOrderStatus(payment.getOrderId(), OrderStatus.PAID);
        } else if (paymentStatus == PaymentStatus.FAILED) {
            orderService.updateOrderStatus(payment.getOrderId(), OrderStatus.FAILED);
        }
    }
}
