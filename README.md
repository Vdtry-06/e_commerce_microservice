# E-Commerce Microservices Platform

Một hệ thống thương mại điện tử được xây dựng với kiến trúc microservices sử dụng Spring Boot 3.3.7, Spring Cloud, Kafka, và các công nghệ hiện đại khác.

## 📋 Tổng Quan Hệ Thống

Hệ thống gồm 7 service chính, được quản lý bởi một Config Server tập trung và Service Discovery (Eureka):

| Service                  | Port | Mô Tả                                          |
| ------------------------ | ---- | ---------------------------------------------- |
| **Config Server**        | 8888 | Cung cấp cấu hình tập trung cho tất cả service |
| **Discovery (Eureka)**   | 8761 | Dịch vụ phát hiện và đăng ký service           |
| **Gateway**              | 8222 | API Gateway - điểm vào duy nhất cho client     |
| **Customer Service**     | 8090 | Quản lý thông tin khách hàng (MongoDB)         |
| **Product Service**      | 8050 | Quản lý sản phẩm (PostgreSQL + Flyway)         |
| **Order Service**        | 8070 | Quản lý đơn hàng (PostgreSQL)                  |
| **Payment Service**      | 8060 | Xử lý thanh toán (PostgreSQL)                  |
| **Notification Service** | 8040 | Gửi thông báo email (MongoDB + Kafka Consumer) |

## 🏗️ Kiến Trúc Hệ Thống

```
┌─────────────────────────────────────────────────────────────────┐
│                          CLIENT/Frontend                         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                    HTTP/REST │
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                      API Gateway (8222)                          │
│  - Route requests based on path predicates                       │
│  - Load balancing via Eureka                                     │
│  - Service discovery enabled                                     │
└──┬────┬────┬────┬──────────────────────────────────────────────┘
   │    │    │    │
   │    │    │    └─────────► Notification Service (8040)
   │    │    │                 ├── MongoDB (Notifications)
   │    │    │                 └── Kafka Consumer (Email)
   │    │    │
   │    │    └─────────► Payment Service (8060)
   │    │                 ├── PostgreSQL (Payments)
   │    │                 └── Kafka Producer
   │    │
   │    └─────────► Order Service (8070)
   │                ├── PostgreSQL (Orders)
   │                ├── FeignClient (Product, Payment, Customer)
   │                └── Kafka Producer
   │
   ├─────────► Product Service (8050)
   │            ├── PostgreSQL (Products)
   │            ├── Flyway Migrations
   │            └── REST API
   │
   └─────────► Customer Service (8090)
                ├── MongoDB (Customers)
                └── REST API

┌─────────────────────────────────────────────────────────────────┐
│                   Config Server (8888)                           │
│  - Centralized configuration management                          │
│  - Profiles: customer, product, order, payment, etc.             │
│  - Native search-locations: classpath:/configurations            │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   Discovery Server (8761)                        │
│  - Service registry and discovery                                │
│  - Health checks and load balancing                              │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                 Infrastructure Services                          │
├─────────────────────────────────────────────────────────────────┤
│ • PostgreSQL (5432) - Orders, Payments, Products DB              │
│ • MongoDB (27017) - Customers, Notifications DB                  │
│ • Kafka (9092) - Message broker for async communication          │
│ • Zookeeper (2181) - Kafka coordination                          │
│ • PgAdmin (5050) - PostgreSQL management UI                      │
│ • Mongo Express (8081) - MongoDB management UI                   │
│ • MailDev (1080, 1025) - Email testing UI                        │
└─────────────────────────────────────────────────────────────────┘
```

## 🔄 Luồng Hoạt Động Chính

### 1. **Luồng Tạo Đơn Hàng**

```
Client → Gateway → Order Service
  │                    ├─→ Customer Service (FeignClient): Xác thực khách hàng
  │                    ├─→ Product Service (RestTemplate): Kiểm tra & trừ hàng
  │                    ├─→ Save Order to DB (PostgreSQL)
  │                    ├─→ Payment Service (FeignClient): Tạo payment request
  │                    ├─→ Kafka Producer: Gửi OrderConfirmation
  │                    │   └─→ Notification Service (Kafka Consumer): Gửi email
  └────────── Response: Order ID
```

**Chi tiết các bước:**

1. Client gửi request tạo order kèm: customerId, products, amount, paymentMethod
2. Order Service gọi Customer Service để xác thực khách hàng
3. Order Service gọi Product Service để kiểm tra sản phẩm và trừ stock
4. Order được lưu vào PostgreSQL
5. Order Lines được tạo cho từng sản phẩm
6. Payment request được gửi tới Payment Service
7. OrderConfirmation được publish lên Kafka topic
8. Notification Service consume message và gửi email xác nhận

### 2. **Luồng Xử Lý Thanh Toán**

```
Payment Service
  ├─→ Validate Payment Request
  ├─→ Save Payment to DB (PostgreSQL)
  ├─→ Kafka Producer: Gửi PaymentConfirmation
  │   └─→ Notification Service: Gửi email thanh toán
  └─→ Response: Payment ID
```

### 3. **Luồng Gửi Thông Báo**

```
Kafka Topics
  ├─→ order-topic → Notification Service → Email Templates
  │                 ├─→ ORDER_CONFIRMATION
  │                 └─→ PAYMENT_CONFIRMATION
  │
  └─→ payment-topic → Notification Service → Save to MongoDB
```

## 🛠️ Công Nghệ Sử Dụng

| Công Nghệ           | Phiên Bản | Mục Đích                |
| ------------------- | --------- | ----------------------- |
| Spring Boot         | 3.3.7     | Framework chính         |
| Spring Cloud        | 2023.0.5  | Microservices support   |
| Spring Data JPA     | 3.3.7     | ORM cho PostgreSQL      |
| Spring Data MongoDB | 3.3.7     | MongoDB driver          |
| Spring Cloud Config | 2023.0.5  | Config server           |
| Eureka              | 2023.0.5  | Service discovery       |
| OpenFeign           | 2023.0.5  | Declarative HTTP client |
| Spring Kafka        | 3.3.1     | Message broker          |
| Flyway              | 9.x       | Database migration      |
| Lombok              | 1.18.30   | Code generation         |
| PostgreSQL          | Latest    | Relational database     |
| MongoDB             | Latest    | NoSQL database          |
| Kafka               | Latest    | Event streaming         |
| Docker              | Latest    | Containerization        |

## 📦 Thành Phần Chi Tiết

### **Discovery Service (Eureka)**

**Cấu hình:** `discovery-service.yml`

- Port: 8761
- Eureka Server (không đăng ký chính nó)
- Tất cả service đều đăng ký với Eureka

**Endpoint:**

```
http://localhost:8761/eureka/
```

### **Config Server**

**Cấu hình:** `application.yml`

- Port: 8888
- Profiles: native
- Search locations: `classpath:/configurations`
- Cung cấp config cho tất cả service dựa trên `spring.application.name`

**Config files:**

- `application.yml` - Cấu hình Eureka chung
- `customer-service.yml` - MongoDB, ports
- `product-service.yml` - PostgreSQL, Flyway
- `order-service.yml` - PostgreSQL, URLs của services
- `payment-service.yml` - PostgreSQL, Kafka
- `notification-service.yml` - MongoDB, Kafka consumer
- `gateway-service.yml` - Gateway routes
- `discovery-service.yml` - Eureka config

### **Gateway Service**

**Vai trò:**

- API Gateway cho tất cả request từ client
- Load balancing via Eureka
- Route requests dựa vào path predicates

**Routes:**

```yaml
- /api/v1/customers/** → CUSTOMER-SERVICE (8090)
- /api/v1/products/** → PRODUCT-SERVICE (8050)
- /api/v1/orders/** → ORDER-SERVICE (8070)
- /api/v1/payments/** → PAYMENT-SERVICE (8060)
```

### **Customer Service**

**Database:** MongoDB

**Endpoints:**

```
POST   /api/v1/customer             - Tạo khách hàng
PUT    /api/v1/customer             - Cập nhật
GET    /api/v1/customer             - Danh sách
GET    /api/v1/customer/{id}        - Chi tiết
GET    /api/v1/customer/exists/{id} - Kiểm tra tồn tại
DELETE /api/v1/customer/{id}        - Xóa
```

**Collections:**

- `customer` - Lưu thông tin khách hàng (tên, email, địa chỉ)

### **Product Service**

**Database:** PostgreSQL

**Endpoints:**

```
POST   /api/v1/products              - Tạo sản phẩm
POST   /api/v1/products/purchase     - Mua hàng (trừ stock)
GET    /api/v1/products              - Danh sách
GET    /api/v1/products/{id}         - Chi tiết
```

**Tables:**

- `product` - Sản phẩm (id, name, description, price, available_quantity)
- `category` - Danh mục

**Migrations (Flyway):**

- `V1__init_database.sql` - Tạo tables
- `V2__insert_data.sql` - Dữ liệu mẫu

**Đặc điểm:** JPA Auditing (createdAt, updatedAt)

### **Order Service**

**Database:** PostgreSQL

**Endpoints:**

```
POST   /api/v1/orders      - Tạo đơn hàng
GET    /api/v1/orders      - Danh sách
GET    /api/v1/orders/{id} - Chi tiết
```

**Tables:**

- `orders` - Đơn hàng
- `order_lines` - Chi tiết sản phẩm trong đơn

**Integrations:**

- **FeignClient**: Customer, Product, Payment services
- **RestTemplate**: Product Service (alternative)
- **Kafka Producer**: Gửi OrderConfirmation

**Xử lý:**

1. Xác thực khách hàng qua Customer Service
2. Kiểm tra/trừ stock qua Product Service
3. Tạo Payment Request
4. Publish OrderConfirmation to Kafka

### **Payment Service**

**Database:** PostgreSQL

**Endpoints:**

```
POST   /api/v1/payments - Tạo thanh toán
```

**Tables:**

- `payment` - Lưu thông tin thanh toán

**Integrations:**

- **Kafka Producer**: Gửi PaymentConfirmation

### **Notification Service**

**Database:** MongoDB

**Tính năng:**

- Kafka Consumer: Lắng nghe `order-topic` và `payment-topic`
- Email Service: Gửi email qua maildev (localhost:1025)
- Email Templates: ORDER_CONFIRMATION, PAYMENT_CONFIRMATION

**Collections:**

- `notification` - Lưu lịch sử thông báo

**Kafka Topics:**

- `order-topic` - OrderConfirmation messages
- `payment-topic` - PaymentConfirmation messages

**Email Setup:**

- Host: localhost
- Port: 1025 (MailDev SMTP)
- User: vdtry06

## 📡 Giao Tiếp Giữa Services

### **Synchronous Communication (FeignClient)**

```java
@FeignClient(name = "customer-service", url = "http://localhost:8090/api/v1/customer")
public interface CustomerClient {
    Optional<CustomerResponse> findCustomerById(String customerId);
}
```

Ưu điểm:

- Real-time response
- Type-safe
- Dễ debug

### **Asynchronous Communication (Kafka)**

```java
// Order Service → Kafka Producer
kafkaTemplate.send("order-topic", orderConfirmation);

// Notification Service → Kafka Consumer
@KafkaListener(topics = "order-topic", groupId = "orderGroup")
public void consumeOrderConfirmation(OrderConfirmation message) {
    // Gửi email
}
```

Ưu điểm:

- Giảm coupling
- Tăng throughput
- Event-driven architecture

## 🗄️ Database Schemas

### **PostgreSQL**

```sql
-- Product Service
CREATE TABLE category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    description TEXT
);

CREATE TABLE product (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    description TEXT,
    price DECIMAL(10,2),
    available_quantity INTEGER,
    category_id INTEGER REFERENCES category(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Order Service
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    reference VARCHAR(100),
    total_amount DECIMAL(10,2),
    payment_method VARCHAR(50),
    customer_id VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE order_lines (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id),
    product_id INTEGER,
    quantity INTEGER
);

-- Payment Service
CREATE TABLE payment (
    id SERIAL PRIMARY KEY,
    reference VARCHAR(100),
    amount DECIMAL(10,2),
    payment_method VARCHAR(50),
    status VARCHAR(50),
    order_reference VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### **MongoDB**

```json
// Customer Collection
{
    "_id": ObjectId,
    "id": String,
    "firstName": String,
    "lastName": String,
    "email": String,
    "address": {
        "street": String,
        "houseNumber": String,
        "zipCode": String,
        "city": String
    },
    "createdAt": Timestamp,
    "updatedAt": Timestamp
}

// Notification Collection
{
    "_id": ObjectId,
    "notificationType": "ORDER_CONFIRMATION|PAYMENT_CONFIRMATION",
    "toCustomer": String,
    "orderReference": String,
    "subject": String,
    "confirmedAt": Timestamp
}
```

## ⚙️ Cấu Hình Environment

### **Docker Compose Services**

```yaml
Services:
  - PostgreSQL: vdtry06/vdtry06 @ localhost:5432
  - MongoDB: vdtry06/vdtry06 @ localhost:27017
  - Kafka: localhost:9092
  - Zookeeper: localhost:2181
  - PgAdmin: http://localhost:5050 (pgadmin4@pgadmin.org/admin)
  - Mongo Express: http://localhost:8081
  - MailDev: http://localhost:1080
```

### **Databases**

```
PostgreSQL:
  - order (Order Service)
  - payment (Payment Service)
  - product (Product Service)

MongoDB:
  - customer (Customer Service)
  - notification (Notification Service)
```

## 🚀 Cách Chạy Hệ Thống

### **Bước 1: Khởi động Infrastructure**

```bash
cd c:\Users\ASUS\e_commerce_microservice
docker-compose up -d
```

Chờ tất cả services khởi động (khoảng 30-60 giây):

```bash
# Kiểm tra trạng thái
docker ps

# Nên thấy các container:
# - ms_pg_sql (PostgreSQL)
# - ms_mongo_db (MongoDB)
# - zookeeper
# - ms_kafka (Kafka)
# - ms-mail-dev (MailDev)
# - ms_pgadmin
# - ms_mongo_express
```

**Kiểm tra Kafka đã sẵn sàng:**

```bash
docker logs ms_kafka | grep -i "started"
# Nên thấy: [..] INFO [KafkaServer id=1] started (kafka.server.KafkaServer)
```

### **Bước 2: Chạy Services (theo thứ tự CHẶT CHẼ)**

> ⚠️ **QUAN TRỌNG**: Phải chạy theo thứ tự này, hãy chờ mỗi service khởi động xong (cỡ 10-15 giây) trước khi chạy service tiếp theo

**Terminal 1 - Config Server (PHẢI CHẠY TRƯỚC TIÊN):**

```bash
cd c:\Users\ASUS\e_commerce_microservice\services\config-server
./mvnw spring-boot:run
# Đợi tới khi thấy: "Started ConfigServerApplication"
```

**Terminal 2 - Discovery Service (Eureka):**

```bash
cd c:\Users\ASUS\e_commerce_microservice\services\discovery
./mvnw spring-boot:run
# Đợi tới khi thấy: "Started DiscoveryApplication"
# Và: "Started Eureka Server"
```

**Terminal 3 - Customer Service:**

```bash
cd c:\Users\ASUS\e_commerce_microservice\services\customer
./mvnw spring-boot:run
# Đợi tới khi thấy: "Started CustomerApplication"
```

**Terminal 4 - Product Service:**

```bash
cd c:\Users\ASUS\e_commerce_microservice\services\product
./mvnw spring-boot:run
# Đợi tới khi thấy: "Started ProductApplication"
```

**Terminal 5 - Order Service:**

```bash
cd c:\Users\ASUS\e_commerce_microservice\services\order
./mvnw spring-boot:run
# Đợi tới khi thấy: "Started OrderApplication"
# (Sẽ có warnings về Kafka, đó là bình thường)
```

**Terminal 6 - Payment Service:**

```bash
cd c:\Users\ASUS\e_commerce_microservice\services\payment
./mvnw spring-boot:run
# Đợi tới khi thấy: "Started PaymentApplication"
```

**Terminal 7 - Notification Service:**

```bash
cd c:\Users\ASUS\e_commerce_microservice\services\notification
./mvnw spring-boot:run
# Đợi tới khi thấy: "Started NotificationApplication"
```

**Terminal 8 - Gateway Service (CHẠY CUỐI CÙNG):**

```bash
cd c:\Users\ASUS\e_commerce_microservice\services\gateway
./mvnw spring-boot:run
# Đợi tới khi thấy: "Started GatewayApplication"
```

### **Bước 3: Kiểm tra Services**

```
Eureka Dashboard:    http://localhost:8761
  (Nên thấy 7 services đã đăng ký)

API Gateway:         http://localhost:8222
  (Điểm vào chính)

Config Server:       http://localhost:8888

PgAdmin:             http://localhost:5050
  (Email: pgadmin4@pgadmin.org, Password: admin)

Mongo Express:       http://localhost:8081

MailDev:             http://localhost:1080
```

## ⚠️ Xử lý Lỗi Phổ Biến

### **404 Not Found khi gọi API**

**Nguyên nhân:** Services chưa hoàn toàn khởi động hoặc chưa đăng ký với Eureka

**Giải pháp:**

1. **Kiểm tra Eureka Dashboard:**

   ```
   http://localhost:8761
   ```

   - Nên thấy 7 instances đã đăng ký (CUSTOMER-SERVICE, PRODUCT-SERVICE, v.v.)
   - Nếu chưa, chờ thêm 30 giây để services đăng ký

2. **Kiểm tra logs của Gateway:**

   ```
   # Tìm dòng: "Started GatewayApplication"
   # Và: "Registering application GATEWAY-SERVICE"
   ```

3. **Nếu vẫn 404, hãy:**
   - Tắt tất cả services (Ctrl+C)
   - Khởi động lại theo thứ tự từ đầu
   - Đợi 60 giây trước khi test API

### **Kafka Connection Errors**

**Nguyên nhân:** Kafka container đã thoát (Exited)

**Giải pháp:**

```bash
# Kiểm tra trạng thái Kafka
docker ps | grep kafka

# Nếu "Exited", khởi động lại:
docker-compose up kafka -d

# Kiểm tra Kafka đã khởi động:
docker logs ms_kafka | grep "started"
```

### **Port Already in Use**

**Lỗi:** `Address already in use: bind`

**Giải pháp:**

```bash
# Tìm process dùng port (ví dụ port 8222):
netstat -ano | findstr :8222

# Kill process:
taskkill /PID <PID> /F

# Hoặc đơn giản, tắt tất cả và start lại
```

### **Database Connection Refused**

**Lỗi:** `Connection refused: localhost:5432`

**Giải pháp:**

```bash
# Kiểm tra PostgreSQL container chạy:
docker ps | grep postgres

# Nếu không chạy:
docker-compose up postgresql -d

# Tương tự cho MongoDB:
docker ps | grep mongo
docker-compose up mongodb -d
```

## 📊 API Usage Examples

### **Tạo Khách Hàng**

```bash
curl -X POST http://localhost:8222/api/v1/customer \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "address": {
      "street": "Main St",
      "houseNumber": "123",
      "zipCode": "12345",
      "city": "New York"
    }
  }'

# Response sẽ như:
# "64f1c2e3d5e4f5g6h7i8j9k0"  (customer ID)
```

### **Tạo Sản Phẩm**

```bash
curl -X POST http://localhost:8222/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Dell",
    "description": "High-performance laptop",
    "price": 1000.00,
    "availableQuantity": 50,
    "categoryId": 1
  }'

# Response:
# 1  (product ID)
```

### **Tạo Đơn Hàng**

```bash
curl -X POST http://localhost:8222/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "64f1c2e3d5e4f5g6h7i8j9k0",
    "reference": "ORD-2024-001",
    "amount": 1000.00,
    "paymentMethod": "CREDIT_CARD",
    "products": [
      {
        "productId": 1,
        "quantity": 1
      }
    ]
  }'

# Response:
# 1  (order ID)
```

### **Kiểm tra Email đã gửi**

```
http://localhost:1080
# Xem các email đã gửi (ORDER_CONFIRMATION, PAYMENT_CONFIRMATION)
```

## 🔧 Troubleshooting Checklist

| Vấn đề                  | Kiểm tra                        | Giải pháp                    |
| ----------------------- | ------------------------------- | ---------------------------- |
| 404 API                 | http://localhost:8761           | Đợi services đăng ký         |
| Kafka Error             | `docker logs ms_kafka`          | `docker-compose up kafka -d` |
| Port in use             | `netstat -ano \| findstr :PORT` | `taskkill /PID <ID> /F`      |
| DB Connection           | `docker ps`                     | `docker-compose up` -d`      |
| Service không khởi động | Xem logs dòng "Started"         | Check config files           |
| Timeout errors          | Logs của services               | Đợi thêm 30 giây             |

## 📝 Lưu Ý Quan Trọng

1. **Thứ tự khởi động:** Config Server → Eureka → Services → Gateway
2. **Kafka Warning:** Order/Payment/Notification service sẽ có warnings về Kafka khi khởi động là BÌNH THƯỜNG
3. **Eureka Registration:** Services mất 10-15 giây để đăng ký
4. **Gateway Routes:** Cần tất cả services đã registered mới route được
5. **Kafka Topics:** Tự động tạo (AUTO_CREATE_TOPICS_ENABLE=true)
6. **Database:** PostgreSQL dùng cho Product/Order/Payment, MongoDB dùng cho Customer/Notification

## 📝 Lưu Ý Quan Trọng

1. **Thứ tự khởi động:** Config Server → Eureka → Các service khác → Gateway
2. **Database Credentials:** username: `vdtry06`, password: `vdtry06`
3. **Kafka:** Cần Zookeeper chạy trước Kafka
4. **Product Service:** Dùng Flyway cho migration tự động
5. **Notifications:** Chỉ gửi thành công khi Kafka consumer đang chạy

## 📚 Tài Liệu Tham Khảo

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Config](https://cloud.spring.io/spring-cloud-config/)
- [Eureka Service Discovery](https://cloud.spring.io/spring-cloud-netflix/)
- [OpenFeign](https://cloud.spring.io/spring-cloud-openfeign/)
- [Spring Kafka](https://spring.io/projects/spring-kafka)
- [Flyway Database Migrations](https://flywaydb.org/)

## 📞 Support

Nếu gặp vấn đề:

1. Kiểm tra tất cả services đang chạy
2. Xem logs của service gặp lỗi
3. Kiểm tra database connection
4. Xác nhận Eureka Server đang chạy
5. Kiểm tra port không bị chiếm dụng
