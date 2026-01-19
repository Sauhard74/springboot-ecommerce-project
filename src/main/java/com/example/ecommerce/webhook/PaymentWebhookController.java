package com.example.ecommerce.webhook;

import com.example.ecommerce.dto.PaymentWebhookRequest;
import com.example.ecommerce.dto.RazorpayWebhookRequest;
import com.example.ecommerce.service.PaymentService;
import com.example.ecommerce.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {
    
    private final PaymentService paymentService;
    private final RazorpayService razorpayService;
    
    @Value("${payment.provider:mock}")
    private String paymentProvider;
    
    @PostMapping("/payment")
    public ResponseEntity<Map<String, String>> handlePaymentWebhook(
            @RequestBody(required = false) Map<String, Object> requestBody,
            HttpServletRequest httpRequest) {
        try {
            // Handle Razorpay webhook
            if ("razorpay".equalsIgnoreCase(paymentProvider)) {
                // Try to parse as Razorpay webhook
                try {
                    RazorpayWebhookRequest razorpayRequest = parseRazorpayWebhook(requestBody);
                    return handleRazorpayWebhook(razorpayRequest, httpRequest);
                } catch (Exception e) {
                    // Fall through to mock webhook handling
                }
            }
            
            // Handle Mock payment webhook
            PaymentWebhookRequest request = parseMockWebhook(requestBody);
            if (request != null) {
                paymentService.handlePaymentWebhook(
                        request.getOrderId(),
                        request.getPaymentId(),
                        request.getStatus()
                );
                
                Map<String, String> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Webhook processed successfully");
                
                return ResponseEntity.ok(response);
            }
            
            throw new RuntimeException("Invalid webhook request");
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    private PaymentWebhookRequest parseMockWebhook(Map<String, Object> requestBody) {
        if (requestBody == null) return null;
        
        PaymentWebhookRequest request = new PaymentWebhookRequest();
        if (requestBody.containsKey("orderId")) {
            request.setOrderId((String) requestBody.get("orderId"));
        }
        if (requestBody.containsKey("paymentId")) {
            request.setPaymentId((String) requestBody.get("paymentId"));
        }
        if (requestBody.containsKey("status")) {
            request.setStatus((String) requestBody.get("status"));
        }
        if (requestBody.containsKey("amount")) {
            Object amount = requestBody.get("amount");
            if (amount instanceof Number) {
                request.setAmount(((Number) amount).doubleValue());
            }
        }
        return request;
    }
    
    private RazorpayWebhookRequest parseRazorpayWebhook(Map<String, Object> requestBody) {
        if (requestBody == null) return null;
        
        RazorpayWebhookRequest request = new RazorpayWebhookRequest();
        if (requestBody.containsKey("event")) {
            request.setEvent((String) requestBody.get("event"));
        }
        
        if (requestBody.containsKey("payload")) {
            Map<String, Object> payloadMap = (Map<String, Object>) requestBody.get("payload");
            RazorpayWebhookRequest.RazorpayPayload payload = new RazorpayWebhookRequest.RazorpayPayload();
            
            if (payloadMap.containsKey("payment")) {
                Map<String, Object> paymentMap = (Map<String, Object>) payloadMap.get("payment");
                RazorpayWebhookRequest.RazorpayPayload.RazorpayPayment payment = 
                    new RazorpayWebhookRequest.RazorpayPayload.RazorpayPayment();
                
                if (paymentMap.containsKey("id")) {
                    payment.setId((String) paymentMap.get("id"));
                }
                if (paymentMap.containsKey("order_id")) {
                    payment.setOrder_id((String) paymentMap.get("order_id"));
                }
                if (paymentMap.containsKey("status")) {
                    payment.setStatus((String) paymentMap.get("status"));
                }
                if (paymentMap.containsKey("amount")) {
                    Object amount = paymentMap.get("amount");
                    if (amount instanceof Number) {
                        payment.setAmount(((Number) amount).intValue());
                    }
                }
                payload.setPayment(payment);
            }
            request.setPayload(payload);
        }
        
        return request;
    }
    
    private ResponseEntity<Map<String, String>> handleRazorpayWebhook(
            RazorpayWebhookRequest razorpayRequest, HttpServletRequest httpRequest) {
        try {
            // Verify webhook signature
            String signature = httpRequest.getHeader("X-Razorpay-Signature");
            String payload = getRequestBody(httpRequest);
            
            if (signature != null && !razorpayService.verifyWebhookSignature(payload, signature)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Invalid webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            // Process payment.captured event
            if ("payment.captured".equals(razorpayRequest.getEvent()) &&
                razorpayRequest.getPayload() != null &&
                razorpayRequest.getPayload().getPayment() != null) {
                
                RazorpayWebhookRequest.RazorpayPayload.RazorpayPayment payment = 
                    razorpayRequest.getPayload().getPayment();
                
                // Use Razorpay order_id to find our payment record
                String razorpayOrderId = payment.getOrder_id();
                String paymentId = payment.getId();
                String status = "captured".equals(payment.getStatus()) ? "SUCCESS" : "FAILED";
                
                // Find payment by razorpay order ID (stored as paymentId in Payment entity)
                paymentService.handleRazorpayWebhook(razorpayOrderId, paymentId, status);
                
                Map<String, String> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Razorpay webhook processed successfully");
                
                return ResponseEntity.ok(response);
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Webhook received (not a payment event)");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    private String getRequestBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
