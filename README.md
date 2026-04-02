# E-Commerce Backend — Spring Boot

A production-ready REST API backend for an e-commerce platform covering user authentication,
product catalog, cart management, order processing, and Stripe payment integration.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (jjwt 0.11) |
| Persistence | Spring Data JPA + Hibernate |
| Database | H2 (dev/test) → MySQL/PostgreSQL (prod) |
| Payments | Stripe Java SDK |
| Validation | Jakarta Bean Validation |
| Build | Maven |
| Java | 17+ |

---

## Project Structure

```
src/main/java/com/ecommerce/
├── config/
│   ├── SecurityConfig.java        # JWT filter chain, CORS, role-based rules
│   └── DataSeeder.java            # Demo users + products on startup
├── controller/
│   ├── AuthController.java        # /api/auth/register, /api/auth/login
│   ├── ProductController.java     # /api/products  (CRUD + search)
│   ├── CartController.java        # /api/cart      (add/update/remove items)
│   ├── OrderController.java       # /api/orders    (create + view orders)
│   └── PaymentController.java     # /api/payments  (Stripe intent + webhook)
├── dto/
│   ├── request/                   # Validated inbound payloads
│   └── response/                  # Outbound API shapes
├── entity/                        # JPA entities: User, Product, Cart, CartItem, Order, OrderItem
├── exception/                     # Custom exceptions + GlobalExceptionHandler
├── repository/                    # Spring Data JPA interfaces
├── security/                      # JwtUtils, JwtAuthFilter, UserDetailsServiceImpl
└── service/
    ├── (interfaces)
    └── impl/                      # AuthServiceImpl, ProductServiceImpl,
                                   # CartServiceImpl, OrderServiceImpl, PaymentServiceImpl
```

---

## Quick Start

### 1. Clone & Configure

```bash
git clone <repo-url>
cd ecommerce-backend
```

Edit `src/main/resources/application.properties`:

```properties
# Replace with your real Stripe test keys
stripe.api.key=sk_test_YOUR_KEY_HERE
stripe.webhook.secret=whsec_YOUR_WEBHOOK_SECRET_HERE
```

### 2. Run

```bash
./mvnw spring-boot:run
```

The server starts on **http://localhost:8080**.

H2 console available at: **http://localhost:8080/h2-console**
(JDBC URL: `jdbc:h2:mem:ecommercedb`, user: `sa`, password: empty)

### 3. Seeded Demo Accounts

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@ecommerce.com | admin123 |
| USER | john@example.com | password123 |

---

## API Reference

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | None | Register new user |
| POST | `/api/auth/login` | None | Login, get JWT token |

**Register:**
```json
POST /api/auth/register
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "mypassword"
}
```

**Login:**
```json
POST /api/auth/login
{
  "email": "jane@example.com",
  "password": "mypassword"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 1,
  "name": "Jane Doe",
  "email": "jane@example.com",
  "role": "ROLE_USER"
}
```

All subsequent requests must include: `Authorization: Bearer <token>`

---

### Products

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/products` | None | List products (paginated) |
| GET | `/api/products?keyword=laptop` | None | Search products |
| GET | `/api/products/{id}` | None | Get product by ID |
| POST | `/api/products` | ADMIN | Create product |
| PUT | `/api/products/{id}` | ADMIN | Update product |
| DELETE | `/api/products/{id}` | ADMIN | Delete product |

Query params: `page=0&size=10&sortBy=price&sortDir=asc`

**Create Product (ADMIN):**
```json
POST /api/products
{
  "name": "Gaming Mouse",
  "description": "Optical, 16000 DPI, RGB",
  "price": 59.99,
  "stockQuantity": 75,
  "imageUrl": "https://example.com/mouse.jpg"
}
```

---

### Cart

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/cart` | USER | View cart |
| POST | `/api/cart/items` | USER | Add item |
| PUT | `/api/cart/items/{id}?quantity=3` | USER | Update quantity (0 = remove) |
| DELETE | `/api/cart/items/{id}` | USER | Remove item |
| DELETE | `/api/cart` | USER | Clear cart |

**Add to Cart:**
```json
POST /api/cart/items
{
  "productId": 1,
  "quantity": 2
}
```

**Cart Response:**
```json
{
  "id": 1,
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "Wireless Headphones",
      "unitPrice": 89.99,
      "quantity": 2,
      "subtotal": 179.98
    }
  ],
  "totalPrice": 179.98,
  "totalItems": 2
}
```

---

### Orders

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/orders` | USER | Create order from cart |
| GET | `/api/orders` | USER | My orders (paginated) |
| GET | `/api/orders/{id}` | USER | Get specific order |
| GET | `/api/orders/admin/all` | ADMIN | All orders |

**Create Order:**
```json
POST /api/orders
{
  "shippingAddress": "42 Elm Street, Mumbai 400001"
}
```

Creating an order:
1. Validates stock for every cart item
2. Deducts stock quantities
3. Records unit prices at time of purchase (price snapshot)
4. Clears the cart
5. Sets `paymentStatus = PENDING`

---

### Payments (Stripe)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/payments/create-intent/{orderId}` | USER | Create Stripe PaymentIntent |
| POST | `/api/payments/webhook` | None (Stripe sig) | Handle Stripe webhook events |

**Payment Flow:**

```
1. POST /api/orders                          → orderId returned
2. POST /api/payments/create-intent/{id}     → clientSecret returned
3. Frontend calls stripe.confirmPayment()    → user enters card
4. Stripe sends webhook → /api/payments/webhook
5. Order paymentStatus updated: PAID / FAILED
```

**PaymentIntent Response:**
```json
{
  "clientSecret": "pi_xxx_secret_yyy",
  "paymentIntentId": "pi_xxx",
  "orderId": 1,
  "amountInCents": 17998,
  "currency": "usd"
}
```

**Webhook Setup (Stripe Dashboard):**
- URL: `https://your-domain.com/api/payments/webhook`
- Events: `payment_intent.succeeded`, `payment_intent.payment_failed`

---

## Entity Relationships

```
User (1) ──── (1) Cart ──── (N) CartItem ──── (1) Product
 │
 └── (N) Order ──── (N) OrderItem ──── (1) Product
```

- **User → Cart**: OneToOne
- **Cart → CartItem**: OneToMany
- **User → Order**: OneToMany
- **Order → OrderItem**: OneToMany
- **CartItem/OrderItem → Product**: ManyToOne

---

## Security Architecture

```
Request
  └── JwtAuthFilter (extracts + validates Bearer token)
        └── UsernamePasswordAuthenticationToken → SecurityContext
              └── @PreAuthorize / HttpSecurity rules
                    ├── Public:  GET /api/products/**, /api/auth/**, /api/payments/webhook
                    ├── USER:    /api/cart/**, POST /api/orders, GET /api/orders
                    └── ADMIN:   POST/PUT/DELETE /api/products, GET /api/orders/admin/all
```

Passwords are hashed with **BCrypt** (cost factor 10). JWTs are signed with HS256. Token expiry: 24 hours.

---

## Error Handling

All errors return a consistent envelope:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 99",
  "timestamp": "2026-04-01T10:30:00"
}
```

Validation errors return field-level messages:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": {
    "email": "must be a well-formed email address",
    "password": "size must be between 6 and 40"
  }
}
```

HTTP status codes used: `200 OK`, `201 Created`, `204 No Content`, `400 Bad Request`,
`401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`, `500 Internal Server Error`.

---

## Running Tests

```bash
./mvnw test
```

Test coverage includes:
- `AuthServiceTest` — register/login happy + sad paths
- `ProductServiceTest` — full CRUD, not-found, pagination
- `CartServiceTest` — add/update/remove items, stock validation, ownership checks
- `OrderServiceTest` — order creation, stock deduction, empty cart, wrong-user access
- `AuthControllerTest` — HTTP layer slice tests (MockMvc)
- `EcommerceApplicationTests` — full Spring context smoke test

---

## Production Checklist

- [ ] Switch `application.properties` to MySQL/PostgreSQL datasource
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` (use Flyway/Liquibase for migrations)
- [ ] Move secrets (`jwt.secret`, `stripe.api.key`) to environment variables or Vault
- [ ] Enable HTTPS / TLS termination
- [ ] Register Stripe webhook endpoint in Stripe Dashboard
- [ ] Configure CORS origins in `SecurityConfig`
- [ ] Add rate limiting (Bucket4j or API Gateway)
- [ ] Set up structured logging (Logback + ELK or CloudWatch)

---

## Postman Collection

Import `postman_collection.json` (included in repo root) for a ready-to-use set of requests
covering all endpoints with example bodies and environment variables for `{{baseUrl}}` and `{{token}}`.
