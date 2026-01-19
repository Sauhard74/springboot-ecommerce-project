package com.example.ecommerce.client;

import com.example.ecommerce.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${payment.mock.service.url:http://localhost:8081}")
    private String mockPaymentServiceUrl;
    
    public Map<String, Object> createPayment(PaymentRequest request) {
        String url = mockPaymentServiceUrl + "/payments/create";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("orderId", request.getOrderId());
        requestBody.put("amount", request.getAmount());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        return restTemplate.postForObject(url, entity, Map.class);
    }
}
