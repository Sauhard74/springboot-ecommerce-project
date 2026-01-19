package com.example.mockpayment.service;

import com.example.mockpayment.dto.WebhookRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class PaymentProcessingService {
    
    private final RestTemplate restTemplate;
    
    @Value("${payment.webhook.url:http://localhost:8080/api/webhooks/payment}")
    private String webhookUrl;
    
    @Async
    public void processPaymentAsync(String orderId, String paymentId, Double amount) {
        try {
            // Wait 3 seconds (simulating payment processing)
            Thread.sleep(3000);
            
            // Simulate successful payment (you can add logic for random failure here)
            String status = "SUCCESS"; // Can be made random: Math.random() > 0.1 ? "SUCCESS" : "FAILED"
            
            // Call webhook
            WebhookRequest webhookRequest = new WebhookRequest();
            webhookRequest.setOrderId(orderId);
            webhookRequest.setPaymentId(paymentId);
            webhookRequest.setStatus(status);
            webhookRequest.setAmount(amount);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<WebhookRequest> entity = new HttpEntity<>(webhookRequest, headers);
            
            restTemplate.postForObject(webhookUrl, entity, Object.class);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Log error (payment processing failed)
            e.printStackTrace();
        }
    }
}
