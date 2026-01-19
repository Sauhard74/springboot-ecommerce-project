package com.example.ecommerce.service;

import com.example.ecommerce.dto.PaymentRequest;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RazorpayService {
    
    @Value("${payment.razorpay.key_id}")
    private String keyId;
    
    @Value("${payment.razorpay.key_secret}")
    private String keySecret;
    
    @Value("${payment.razorpay.webhook.secret}")
    private String webhookSecret;
    
    public Map<String, Object> createRazorpayOrder(PaymentRequest request) {
        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            
            JSONObject orderRequest = new JSONObject();
            // Amount in paise (smallest currency unit)
            orderRequest.put("amount", (int)(request.getAmount() * 100));
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_receipt_" + request.getOrderId());
            orderRequest.put("notes", new JSONObject().put("orderId", request.getOrderId()));
            
            Order order = razorpay.orders.create(orderRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("razorpayOrderId", order.get("id"));
            response.put("amount", request.getAmount());
            response.put("currency", "INR");
            response.put("keyId", keyId);
            response.put("orderId", request.getOrderId());
            
            return response;
        } catch (RazorpayException e) {
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }
    
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            com.razorpay.Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
