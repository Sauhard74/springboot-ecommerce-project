package com.example.ecommerce.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Document(collection = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    private String id;
    
    private String orderId;
    
    private Double amount;
    
    private PaymentStatus status; // PENDING, SUCCESS, FAILED
    
    private String paymentId; // External payment ID
    
    private Instant createdAt;
}
