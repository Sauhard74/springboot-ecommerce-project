# E-Commerce Backend API

A minimal e-commerce backend system built with Spring Boot and MongoDB, following the PRD specifications.

## 📊 ER Diagram

The Entity Relationship Diagram is available in [ER_DIAGRAM.md](ER_DIAGRAM.md). The diagram shows all entities, their relationships, and database schema.

You can also view the ER diagram in Mermaid format in the PRD document (`prd.md`) or in `ER_DIAGRAM.md`.

## 🏗️ Architecture

- **E-Commerce API**: Main application running on port 8080
- **Mock Payment Service**: Separate service running on port 8081
- **MongoDB**: Database for storing all entities

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- MongoDB (running on localhost:27017)
- Postman (for testing)

## 🚀 Setup Instructions

### 1. Start MongoDB

Make sure MongoDB is running on `localhost:27017`:

```bash
# Using Docker
docker run -d -p 27017:27017 mongo:latest

# Or using local MongoDB installation
mongod
```

### 2. Start E-Commerce API

```bash
cd /path/to/springboot
mvn spring-boot:run
```

The API will start on `http://localhost:8080`

### 3. Start Mock Payment Service

Open a new terminal:

```bash
cd /path/to/springboot/mock-payment-service
mvn spring-boot:run
```

The mock payment service will start on `http://localhost:8081`

## 📁 Project Structure

```
com.example.ecommerce
│
├── controller
│   ├── ProductController.java
│   ├── CartController.java
│   ├── OrderController.java
│   └── PaymentController.java
│
├── service
│   ├── ProductService.java
│   ├── CartService.java
│   ├── OrderService.java
│   └── PaymentService.java
│
├── repository
│   ├── ProductRepository.java
│   ├── CartRepository.java
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   └── PaymentRepository.java
│
├── model
│   ├── User.java
│   ├── Product.java
│   ├── CartItem.java
│   ├── Order.java
│   ├── OrderItem.java
│   └── Payment.java
│
├── dto
│   ├── AddToCartRequest.java
│   ├── CreateOrderRequest.java
│   ├── PaymentRequest.java
│   └── PaymentWebhookRequest.java
│
├── webhook
│   └── PaymentWebhookController.java
│
├── client
│   └── PaymentServiceClient.java
│
├── config
│   └── RestTemplateConfig.java
│
└── EcommerceApplication.java
```

## 🔌 API Endpoints

### Product APIs

#### POST /api/products
Create a new product

**Request:**
```json
{
  "name": "Laptop",
  "description": "Gaming Laptop",
  "price": 50000.0,
  "stock": 10
}
```

#### GET /api/products
Get all products

#### GET /api/products/search?q=laptop
Search products by name or description (Bonus feature)

**Query Parameters:**
- `q` (optional): Search query string

### Cart APIs

#### POST /api/cart/add
Add item to cart

**Request:**
```json
{
  "userId": "user123",
  "productId": "prod123",
  "quantity": 2
}
```

#### GET /api/cart/{userId}
Get user's cart

#### DELETE /api/cart/{userId}/clear
Clear user's cart

### Order APIs

#### POST /api/orders
Create order from cart

**Request:**
```json
{
  "userId": "user123"
}
```

#### GET /api/orders/{orderId}
Get order details

#### GET /api/orders/user/{userId}
Get order history for a user (Bonus feature)

#### POST /api/orders/{orderId}/cancel
Cancel an order (Bonus feature)

**Response:**
```json
{
  "id": "order123",
  "userId": "user123",
  "totalAmount": 100000.0,
  "status": "CANCELLED",
  "message": "Order cancelled successfully"
}
```

**Note:** Only orders with status CREATED can be cancelled. Stock will be restored automatically.

### Payment APIs

#### POST /api/payments/create
Create payment for order

**Request:**
```json
{
  "orderId": "order123",
  "amount": 100000.0
}
```

**Response (Mock Payment):**
```json
{
  "paymentId": "pay_mock123",
  "orderId": "order123",
  "amount": 100000.0,
  "status": "PENDING"
}
```

**Response (Razorpay):**
```json
{
  "paymentId": "pay_internal_id",
  "razorpayOrderId": "order_xyz",
  "orderId": "order123",
  "amount": 100000.0,
  "status": "PENDING",
  "keyId": "rzp_test_xxxxx",
  "currency": "INR"
}
```

#### POST /api/webhooks/payment
Receive payment webhook (called by Mock Payment Service or Razorpay)

## 🔄 Complete Order Flow

1. **Create Products**: POST /api/products
2. **Add to Cart**: POST /api/cart/add
3. **View Cart**: GET /api/cart/{userId}
4. **Create Order**: POST /api/orders
5. **Initiate Payment**: POST /api/payments/create
6. **Wait for Webhook**: Mock payment service processes payment (3 seconds delay)
7. **Check Order Status**: GET /api/orders/{orderId}

## 🧪 Testing with Postman

### Import Postman Collection

A complete Postman collection is available in `E-Commerce-API.postman_collection.json`. To use it:

1. Open Postman
2. Click **Import** button
3. Select the file `E-Commerce-API.postman_collection.json`
4. The collection will be imported with all endpoints and sample requests
5. Update variables (userId, productId, orderId, etc.) as needed

The collection includes:
- All Product APIs (including search)
- All Cart APIs
- All Order APIs (including history and cancellation)
- Payment APIs
- Webhook endpoints
- Complete flow examples
- Environment variables for easy testing

### Sample Test Flow

1. **Create Products**
   ```
   POST http://localhost:8080/api/products
   Body: {
     "name": "Laptop",
     "description": "Gaming Laptop",
     "price": 50000.0,
     "stock": 10
   }
   ```

2. **Add to Cart**
   ```
   POST http://localhost:8080/api/cart/add
   Body: {
     "userId": "user123",
     "productId": "<product-id-from-step-1>",
     "quantity": 2
   }
   ```

3. **View Cart**
   ```
   GET http://localhost:8080/api/cart/user123
   ```

4. **Create Order**
   ```
   POST http://localhost:8080/api/orders
   Body: {
     "userId": "user123"
   }
   ```

5. **Initiate Payment**
   ```
   POST http://localhost:8080/api/payments/create
   Body: {
     "orderId": "<order-id-from-step-4>",
     "amount": 100000.0
   }
   ```

6. **Check Order Status** (after ~3 seconds)
   ```
   GET http://localhost:8080/api/orders/<order-id>
   ```

## 📊 Database

MongoDB collections:
- `users`
- `products`
- `cart_items`
- `orders`
- `order_items`
- `payments`

Database name: `ecommerce_db`

## 🛠️ Technologies Used

- Spring Boot 3.2.0
- Spring Data MongoDB
- Lombok
- Maven
- MongoDB

## 📝 Configuration

All configuration is in `src/main/resources/application.yaml`:

```yaml
server:
  port: 8080

spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: ecommerce_db

payment:
  provider: mock # Options: mock, razorpay
  mock:
    service:
      url: http://localhost:8081
      webhook:
        url: http://localhost:8080/api/webhooks/payment
  razorpay:
    key_id: ${RAZORPAY_KEY_ID:rzp_test_xxxxxxxxxxxxx}
    key_secret: ${RAZORPAY_KEY_SECRET:your_razorpay_secret}
    webhook:
      secret: ${RAZORPAY_WEBHOOK_SECRET:your_webhook_secret}
```

### Switching Payment Providers

**Mock Payment (Default):**
- Set `payment.provider: mock`
- Ensure Mock Payment Service is running on port 8081

**Razorpay (Bonus):**
- Set `payment.provider: razorpay`
- Set environment variables:
  - `RAZORPAY_KEY_ID`: Your Razorpay Key ID
  - `RAZORPAY_KEY_SECRET`: Your Razorpay Key Secret
  - `RAZORPAY_WEBHOOK_SECRET`: Your Razorpay Webhook Secret
- Configure webhook URL in Razorpay dashboard: `http://your-domain/api/webhooks/payment`

## ⚠️ Important Notes

- Ensure MongoDB is running before starting the application
- Start Mock Payment Service before testing payment flow
- Order status will update automatically after webhook callback (3 seconds delay)
- Stock is automatically decremented when order is created

## 🎯 Features Implemented

### Core Features
✅ Product CRUD operations
✅ Product search by name/description (Bonus)
✅ Cart management (add, view, clear)
✅ Order creation from cart
✅ Order history by user (Bonus)
✅ Order cancellation with stock restoration (Bonus)
✅ Stock validation and management
✅ Payment integration (Mock Service & Razorpay)
✅ Webhook handling
✅ Order status updates
✅ Automatic cart clearing after order creation

### Technical Improvements
✅ Clean architecture with DTOs (ProductRequest)
✅ Status enums (OrderStatus, PaymentStatus) for type safety
✅ Razorpay integration (Bonus +10 points)
✅ Comprehensive error handling
✅ Input validation

## 📎 Additional Files

- **E-Commerce-API.postman_collection.json**: Complete Postman collection with all endpoints
- **ER_DIAGRAM.md**: Entity Relationship Diagram documentation
- **COMPLETE_FEATURES.md**: Detailed feature implementation summary
- **prd.md**: Original Product Requirements Document

### Bonus Challenges Completed
✅ Order History: `GET /api/orders/user/{userId}`
✅ Order Cancellation: `POST /api/orders/{orderId}/cancel`
✅ Product Search: `GET /api/products/search?q=laptop`
✅ Razorpay Integration: Full implementation with webhook support
