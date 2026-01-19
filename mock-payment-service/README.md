# Mock Payment Service

A mock payment service that simulates payment processing for the E-Commerce backend.

## 🚀 Quick Start

```bash
cd mock-payment-service
mvn spring-boot:run
```

Service will start on `http://localhost:8081`

## 🔌 API Endpoints

### POST /payments/create

Creates a payment and processes it asynchronously. After 3 seconds, it calls the webhook endpoint.

**Request:**
```json
{
  "orderId": "order123",
  "amount": 100000.0
}
```

**Response:**
```json
{
  "paymentId": "pay_mock123",
  "orderId": "order123",
  "amount": 100000.0,
  "status": "PENDING"
}
```

## 🔄 Payment Flow

1. Client calls `/payments/create` with order details
2. Service returns payment ID with PENDING status
3. Service waits 3 seconds (simulating payment processing)
4. Service calls webhook at `http://localhost:8080/api/webhooks/payment` with SUCCESS status

## ⚙️ Configuration

Update webhook URL in `application.yaml`:

```yaml
payment:
  webhook:
    url: http://localhost:8080/api/webhooks/payment
```
