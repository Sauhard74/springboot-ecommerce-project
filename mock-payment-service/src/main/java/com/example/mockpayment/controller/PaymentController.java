package com.example.mockpayment.controller;

import com.example.mockpayment.dto.PaymentCreateRequest;
import com.example.mockpayment.dto.WebhookRequest;
import com.example.mockpayment.service.PaymentProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentProcessingService paymentProcessingService;
    
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody PaymentCreateRequest request) {
        String paymentId = UUID.randomUUID().toString();
        
        Map<String, Object> response = new HashMap<>();
        response.put("paymentId", paymentId);
        response.put("orderId", request.getOrderId());
        response.put("amount", request.getAmount());
        response.put("status", "PENDING");
        
        // Process payment asynchronously (will call webhook after 3 seconds)
        paymentProcessingService.processPaymentAsync(
                request.getOrderId(),
                paymentId,
                request.getAmount()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
