# Delivery Charges API Documentation — Including Order Integration

**Base URL:** `http://localhost:8080/api`  
**Content-Type:** `application/json`

> **Note:** The application context path is `/api`, so all controller paths are relative to that.  
> Full URL pattern: `http://localhost:8080/api/delivery-charges/...`

---

## Overview

The Delivery Charges API lets administrators define tiered delivery fee rules based on the order subtotal.
The checkout / cart summary screen can call the **Calculate** endpoint to get the live shipping fee for a customer's basket.

### How Rules Work

- Each rule defines a **min** and optional **max** order amount bracket.
- When a customer's order subtotal falls within a bracket, the associated `deliveryCharge` applies.
- Rules are evaluated in ascending `priority` order — the **first matching rule** wins.
- A `deliveryCharge` of `0.00` means **free delivery**.
- `maxOrderAmount: null` means **no upper limit** (open-ended upper bracket).

### Typical Setup Example

| Priority | Rule Name                   | Min (₹) | Max (₹) | Charge (₹) |
|----------|-----------------------------|---------|---------|------------|
| 1        | Free Delivery Above ₹500    | 500     | null    | 0          |
| 2        | Standard Delivery Below ₹500 | 0       | 499.99  | 50         |

---

## Endpoints Summary

| # | Method   | Endpoint                              | Description                         |
|---|----------|---------------------------------------|-------------------------------------|
| 1 | `POST`   | `/api/delivery-charges`               | Create a new delivery charge rule   |
| 2 | `PUT`    | `/api/delivery-charges/{id}`          | Update an existing rule             |
| 3 | `DELETE` | `/api/delivery-charges/{id}`          | Deactivate (soft-delete) a rule     |
| 4 | `GET`    | `/api/delivery-charges/{id}`          | Get a single rule by ID             |
| 5 | `GET`    | `/api/delivery-charges?status=A`      | List all rules (optional filter)    |
| 6 | `GET`    | `/api/delivery-charges/calculate?orderAmount=499` | Calculate charge for cart amount |

---

## 1. Create Delivery Charge Rule

**Endpoint:** `POST /api/delivery-charges`  
**Description:** Creates a new delivery charge rule. `status` defaults to `A` (Active) if not provided.

### Request Payload

| Field            | Type       | Required | Description                                                  |
|------------------|------------|----------|--------------------------------------------------------------|
| `ruleName`       | String     | **Yes**  | Human-readable label for the rule                            |
| `deliveryCharge` | BigDecimal | **Yes**  | Delivery fee in ₹. Use `0` for free delivery                 |
| `minOrderAmount` | BigDecimal | No       | Minimum cart value for rule to apply. Defaults to `0`        |
| `maxOrderAmount` | BigDecimal | No       | Maximum cart value (inclusive). `null` = no upper limit      |
| `priority`       | Integer    | No       | Evaluation order (lower = checked first). Defaults to `100`  |
| `description`    | String     | No       | Optional notes about the rule                                |
| `status`         | String     | No       | `A` = Active, `I` = Inactive. Defaults to `A`                |
| `createdBy`      | String     | No       | Admin user identifier                                        |

### Request — Free Delivery Rule (above ₹500)

```json
{
  "ruleName": "Free Delivery Above ₹500",
  "minOrderAmount": 500.00,
  "maxOrderAmount": null,
  "deliveryCharge": 0.00,
  "priority": 1,
  "description": "Orders with subtotal ₹500 or more qualify for free delivery",
  "status": "A",
  "createdBy": "admin"
}
```

### Request — Standard Delivery Rule (below ₹500)

```json
{
  "ruleName": "Standard Delivery Below ₹500",
  "minOrderAmount": 0.00,
  "maxOrderAmount": 499.99,
  "deliveryCharge": 50.00,
  "priority": 2,
  "description": "Standard delivery charge for orders under ₹500",
  "status": "A",
  "createdBy": "admin"
}
```

### Response — Success (200 OK)

```json
{
  "id": 1,
  "ruleName": "Free Delivery Above ₹500",
  "minOrderAmount": 500.00,
  "maxOrderAmount": null,
  "deliveryCharge": 0.00,
  "isFreeDelivery": true,
  "priority": 1,
  "status": "A",
  "description": "Orders with subtotal ₹500 or more qualify for free delivery",
  "createdBy": "admin",
  "updatedBy": null,
  "createdAt": "2026-05-20T18:30:00",
  "updatedAt": "2026-05-20T18:30:00"
}
```

### Response — Validation Failure (400 Bad Request)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "ruleName is required"
}
```

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "deliveryCharge is required"
}
```

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "deliveryCharge must be >= 0"
}
```

### Response — Server Error (500)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An error occurred while creating the delivery charge rule"
}
```

---

## 2. Update Delivery Charge Rule

**Endpoint:** `PUT /api/delivery-charges/{id}`  
**Description:** Partially updates an existing rule. Only the fields provided (non-null) in the request body are updated.

### Path Parameter

| Parameter | Type | Required | Description              |
|-----------|------|----------|--------------------------|
| `id`      | Long | **Yes**  | ID of the rule to update |

### Request Payload (all fields optional)

| Field            | Type       | Description                                             |
|------------------|------------|---------------------------------------------------------|
| `ruleName`       | String     | New rule label                                          |
| `minOrderAmount` | BigDecimal | Updated minimum cart value                              |
| `maxOrderAmount` | BigDecimal | Updated maximum cart value (`null` = no upper limit)    |
| `deliveryCharge` | BigDecimal | Updated delivery fee                                    |
| `priority`       | Integer    | Updated priority                                        |
| `description`    | String     | Updated description                                     |
| `status`         | String     | `A` = Active, `I` = Inactive                            |
| `updatedBy`      | String     | Admin user identifier                                   |

### Request — Update delivery charge amount

```json
{
  "deliveryCharge": 40.00,
  "description": "Revised standard delivery charge",
  "updatedBy": "admin"
}
```

### Request — Raise the free-delivery threshold to ₹600

```json
{
  "ruleName": "Free Delivery Above ₹600",
  "minOrderAmount": 600.00,
  "updatedBy": "admin"
}
```

### Request — Deactivate a rule via update

```json
{
  "status": "I",
  "updatedBy": "admin"
}
```

### Response — Success (200 OK)

```json
{
  "id": 2,
  "ruleName": "Standard Delivery Below ₹500",
  "minOrderAmount": 0.00,
  "maxOrderAmount": 499.99,
  "deliveryCharge": 40.00,
  "isFreeDelivery": false,
  "priority": 2,
  "status": "A",
  "description": "Revised standard delivery charge",
  "createdBy": "admin",
  "updatedBy": "admin",
  "createdAt": "2026-05-20T18:30:00",
  "updatedAt": "2026-05-20T19:00:00"
}
```

### Response — Not Found (404)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Delivery charge rule not found with id=99"
}
```

### Response — Validation Failure (400 Bad Request)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "deliveryCharge must be >= 0"
}
```

---

## 3. Deactivate (Soft-Delete) Delivery Charge Rule

**Endpoint:** `DELETE /api/delivery-charges/{id}`  
**Description:** Soft-deletes a rule by setting its `status` to `I` (Inactive). The record is retained in the database. No request body needed.

### Path Parameter

| Parameter | Type | Required | Description                |
|-----------|------|----------|----------------------------|
| `id`      | Long | **Yes**  | ID of the rule to deactivate |

### Request

```
DELETE /api/delivery-charges/2
```

### Response — Success (200 OK)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Delivery charge rule deactivated successfully"
}
```

### Response — Not Found (404)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Delivery charge rule not found with id=99"
}
```

---

## 4. Get Delivery Charge Rule by ID

**Endpoint:** `GET /api/delivery-charges/{id}`  
**Description:** Returns a single delivery charge rule by its numeric ID.

### Path Parameter

| Parameter | Type | Required | Description         |
|-----------|------|----------|---------------------|
| `id`      | Long | **Yes**  | ID of the rule      |

### Request

```
GET /api/delivery-charges/1
```

### Response — Success (200 OK)

```json
{
  "id": 1,
  "ruleName": "Free Delivery Above ₹500",
  "minOrderAmount": 500.00,
  "maxOrderAmount": null,
  "deliveryCharge": 0.00,
  "isFreeDelivery": true,
  "priority": 1,
  "status": "A",
  "description": "Orders with subtotal ₹500 or more qualify for free delivery",
  "createdBy": "admin",
  "updatedBy": null,
  "createdAt": "2026-05-20T18:30:00",
  "updatedAt": "2026-05-20T18:30:00"
}
```

### Response — Not Found (404)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Delivery charge rule not found with id=99"
}
```

---

## 5. List All Delivery Charge Rules

**Endpoint:** `GET /api/delivery-charges`  
**Description:** Returns all delivery charge rules sorted by `priority` ascending. Optionally filter by `status`.

### Query Parameters

| Parameter | Type   | Required | Description                                          |
|-----------|--------|----------|------------------------------------------------------|
| `status`  | String | No       | Filter by `A` (Active only) or `I` (Inactive only). Omit to return all. |

### Request — All rules

```
GET /api/delivery-charges
```

### Request — Active rules only

```
GET /api/delivery-charges?status=A
```

### Request — Inactive rules only

```
GET /api/delivery-charges?status=I
```

### Response — Success (200 OK)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Delivery charge rules fetched successfully",
  "deliveryCharges": [
    {
      "id": 1,
      "ruleName": "Free Delivery Above ₹500",
      "minOrderAmount": 500.00,
      "maxOrderAmount": null,
      "deliveryCharge": 0.00,
      "isFreeDelivery": true,
      "priority": 1,
      "status": "A",
      "description": "Orders with subtotal ₹500 or more qualify for free delivery",
      "createdBy": "admin",
      "updatedBy": null,
      "createdAt": "2026-05-20T18:30:00",
      "updatedAt": "2026-05-20T18:30:00"
    },
    {
      "id": 2,
      "ruleName": "Standard Delivery Below ₹500",
      "minOrderAmount": 0.00,
      "maxOrderAmount": 499.99,
      "deliveryCharge": 50.00,
      "isFreeDelivery": false,
      "priority": 2,
      "status": "A",
      "description": "Standard delivery charge for orders under ₹500",
      "createdBy": "admin",
      "updatedBy": null,
      "createdAt": "2026-05-20T18:31:00",
      "updatedAt": "2026-05-20T18:31:00"
    }
  ]
}
```

### Response — Empty List (200 OK)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Delivery charge rules fetched successfully",
  "deliveryCharges": []
}
```

### Response — Server Error (500)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An error occurred while fetching delivery charge rules",
  "deliveryCharges": null
}
```

---

## 6. Calculate Delivery Charge for Cart Amount

**Endpoint:** `GET /api/delivery-charges/calculate`  
**Description:** Returns the applicable delivery charge for a given order subtotal.  
This is the **checkout / cart summary endpoint** — call it before placing an order to show the customer the exact shipping fee.

The engine picks the **first matching active rule** (sorted by `priority` ascending).

### Query Parameter

| Parameter     | Type       | Required | Description                            |
|---------------|------------|----------|----------------------------------------|
| `orderAmount` | BigDecimal | **Yes**  | Cart subtotal in ₹ (must be >= 0)      |

### Request — Cart below free delivery threshold

```
GET /api/delivery-charges/calculate?orderAmount=350
```

### Request — Cart above free delivery threshold

```
GET /api/delivery-charges/calculate?orderAmount=650
```

### Request — Exact threshold

```
GET /api/delivery-charges/calculate?orderAmount=500
```

---

### Response — Paid Delivery (200 OK) — order amount ₹350

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Delivery charge calculated successfully",
  "orderAmount": 350.00,
  "applicableDeliveryCharge": 50.00,
  "isFreeDelivery": false,
  "matchedRule": {
    "id": 2,
    "ruleName": "Standard Delivery Below ₹500",
    "minOrderAmount": 0.00,
    "maxOrderAmount": 499.99,
    "deliveryCharge": 50.00,
    "isFreeDelivery": false,
    "priority": 2,
    "status": "A",
    "description": "Standard delivery charge for orders under ₹500",
    "createdBy": "admin",
    "updatedBy": null,
    "createdAt": "2026-05-20T18:31:00",
    "updatedAt": "2026-05-20T18:31:00"
  }
}
```

### Response — Free Delivery (200 OK) — order amount ₹650

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Delivery charge calculated successfully",
  "orderAmount": 650.00,
  "applicableDeliveryCharge": 0.00,
  "isFreeDelivery": true,
  "matchedRule": {
    "id": 1,
    "ruleName": "Free Delivery Above ₹500",
    "minOrderAmount": 500.00,
    "maxOrderAmount": null,
    "deliveryCharge": 0.00,
    "isFreeDelivery": true,
    "priority": 1,
    "status": "A",
    "description": "Orders with subtotal ₹500 or more qualify for free delivery",
    "createdBy": "admin",
    "updatedBy": null,
    "createdAt": "2026-05-20T18:30:00",
    "updatedAt": "2026-05-20T18:30:00"
  }
}
```

### Response — No Matching Rule Found (200 OK)

> Returned when no active rule covers the given order amount (e.g., rules not yet configured).

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "No matching delivery charge rule found for the given order amount",
  "orderAmount": 350.00,
  "applicableDeliveryCharge": null,
  "isFreeDelivery": null,
  "matchedRule": null
}
```

### Response — Invalid Amount (400 Bad Request)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "orderAmount must be a non-negative value",
  "orderAmount": null,
  "applicableDeliveryCharge": null,
  "isFreeDelivery": null,
  "matchedRule": null
}
```

---

## Database Table — `delivery_charges`

The following table is auto-created by Hibernate (`ddl-auto=update`) on application startup.

```sql
CREATE TABLE delivery_charges (
    id                BIGSERIAL       PRIMARY KEY,
    rule_name         VARCHAR(255)    NOT NULL,
    min_order_amount  NUMERIC(10, 2)  NOT NULL DEFAULT 0,
    max_order_amount  NUMERIC(10, 2),                        -- NULL = no upper limit
    delivery_charge   NUMERIC(10, 2)  NOT NULL DEFAULT 0,
    is_free_delivery  BOOLEAN         NOT NULL DEFAULT FALSE,
    priority          INTEGER         NOT NULL DEFAULT 100,
    status            CHAR(1)         NOT NULL DEFAULT 'A',  -- 'A'=Active, 'I'=Inactive
    description       VARCHAR(255),
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP
);
```

### Sample Seed Data

```sql
-- Free delivery for orders ₹500 and above
INSERT INTO delivery_charges
    (rule_name, min_order_amount, max_order_amount, delivery_charge, is_free_delivery, priority, status, description, created_by)
VALUES
    ('Free Delivery Above ₹500', 500.00, NULL, 0.00, TRUE, 1, 'A',
     'Orders with subtotal ₹500 or more qualify for free delivery', 'admin');

-- Standard delivery for orders below ₹500
INSERT INTO delivery_charges
    (rule_name, min_order_amount, max_order_amount, delivery_charge, is_free_delivery, priority, status, description, created_by)
VALUES
    ('Standard Delivery Below ₹500', 0.00, 499.99, 50.00, FALSE, 2, 'A',
     'Standard delivery charge for orders under ₹500', 'admin');
```

---

## Error Reference

| HTTP Status | `responseStatus` | Scenario                                         |
|-------------|-----------------|--------------------------------------------------|
| `200 OK`    | `SUCCESS`       | Request processed successfully                   |
| `200 OK`    | `FAILURE`       | Calculate: no matching rule found                |
| `400`       | `FAILURE`       | Missing required field / invalid field value     |
| `404`       | `FAILURE`       | Rule not found for given ID                      |
| `500`       | `FAILURE`       | Unexpected server error                          |

---

## Field Reference — `DeliveryChargeResponseDTO`

| Field                    | Type            | Description                                            |
|--------------------------|-----------------|--------------------------------------------------------|
| `id`                     | Long            | Auto-generated primary key                             |
| `ruleName`               | String          | Human-readable rule label                              |
| `minOrderAmount`         | BigDecimal      | Minimum cart subtotal for this rule to apply           |
| `maxOrderAmount`         | BigDecimal/null | Maximum cart subtotal (`null` = no upper limit)        |
| `deliveryCharge`         | BigDecimal      | Delivery fee in ₹ (0.00 = free)                        |
| `isFreeDelivery`         | Boolean         | `true` when `deliveryCharge == 0`                      |
| `priority`               | Integer         | Evaluation order (lower = checked first)               |
| `status`                 | String          | `A` = Active, `I` = Inactive                           |
| `description`            | String          | Optional notes                                         |
| `createdBy`              | String          | Admin who created the rule                             |
| `updatedBy`              | String          | Admin who last updated the rule                        |
| `createdAt`              | LocalDateTime   | Record creation timestamp                              |
| `updatedAt`              | LocalDateTime   | Last update timestamp                                  |

---

## 7. Order Creation — Automatic Delivery Charge Integration

**Endpoint:** `POST /api/orders`  
**Description:** Creates an order, **automatically computes the cart subtotal** from the sent product variants, looks up the matching delivery charge rule, adds the shipping fee on top, and passes the **grand total (subtotal + shipping fee) to Razorpay** as the payment amount.

> The client does **not** need to send a total or delivery charge — the server calculates everything.

### How Delivery Charge Is Applied During Order Creation

```
1. Receive product list (variant IDs + quantities)
2. Fetch selling price for each variant from DB
3. subtotalAmount = Σ (sellingPrice × quantity)
4. Query delivery_charges WHERE status='A'
       AND minOrderAmount <= subtotalAmount
       AND (maxOrderAmount IS NULL OR maxOrderAmount >= subtotalAmount)
   ORDER BY priority ASC
   → First row wins
5. shippingFee = matchedRule.deliveryCharge   (0.00 if free delivery)
6. grandTotal  = subtotalAmount + shippingFee
7. Razorpay order created with amount = grandTotal × 100 paise
8. Response returns subtotal, shippingFee, isFreeDelivery, grandTotal + Razorpay keys
```

### Request Payload

```json
{
  "userId": "42",
  "customerId": null,
  "orderAddressId": null,
  "name": "Priya Lakshmi",
  "phone": "9876543210",
  "email": "priya@example.com",
  "address1": "No. 12, Gandhi Street",
  "address2": "T. Nagar",
  "city": "Chennai",
  "state": "Tamil Nadu",
  "postalCode": "600017",
  "country": "India",
  "landmark": "Near Panagal Park",
  "products": [
    { "productId": "101", "quantity": 2 },
    { "productId": "205", "quantity": 1 }
  ]
}
```

> **`total` field is no longer required** — the server computes the total from product prices + delivery charge.  
> `userId` or address inline fields are required to identify / create the customer.

### Request Payload Fields

| Field            | Type    | Required | Description                                          |
|------------------|---------|----------|------------------------------------------------------|
| `userId`         | String  | Cond.    | Logged-in user ID. One of `userId` or `phone` is required. |
| `customerId`     | Integer | No       | Existing customer ID (skips customer creation)       |
| `orderAddressId` | Integer | No       | Saved customer address ID (skips address inline)     |
| `name`           | String  | No       | Recipient name (used for guest / inline address)     |
| `phone`          | String  | Cond.    | Required for guest checkout / new customer           |
| `email`          | String  | No       | Customer email                                       |
| `address1`       | String  | No       | Address line 1 (used when `orderAddressId` is null)  |
| `address2`       | String  | No       | Address line 2                                       |
| `city`           | String  | No       | City                                                 |
| `state`          | String  | No       | State                                                |
| `postalCode`     | String  | No       | Postal code                                          |
| `country`        | String  | No       | Country                                              |
| `landmark`       | String  | No       | Landmark                                             |
| `products`       | Array   | **Yes**  | At least one product is required                     |
| `products[].productId` | String | **Yes** | Product variant ID                            |
| `products[].quantity`  | Integer | **Yes** | Quantity to order                            |

---

### Response — Success (200 OK) — Paid Delivery (cart subtotal ₹350)

> Cart subtotal = ₹350 → matches "Standard Delivery Below ₹500" rule → shipping fee = ₹50 → grand total = ₹400

```json
{
  "status": "success",
  "message": "order created successfully",
  "orderNumber": "ORD-200526103045-000003",
  "subtotalAmount": 350.00,
  "shippingFee": 50.00,
  "isFreeDelivery": false,
  "amount": 400.00,
  "currency": "INR",
  "storeName": "Kuchi Mittai",
  "description": "Order Payment",
  "paymentOrderId": "order_Pyz1234abcd5678",
  "paymentGatewayKey": "rzp_test_XXXXXXXXXXXXXXX"
}
```

### Response — Success (200 OK) — Free Delivery (cart subtotal ₹650)

> Cart subtotal = ₹650 → matches "Free Delivery Above ₹500" rule → shipping fee = ₹0 → grand total = ₹650

```json
{
  "status": "success",
  "message": "order created successfully",
  "orderNumber": "ORD-200526103120-000004",
  "subtotalAmount": 650.00,
  "shippingFee": 0.00,
  "isFreeDelivery": true,
  "amount": 650.00,
  "currency": "INR",
  "storeName": "Kuchi Mittai",
  "description": "Order Payment",
  "paymentOrderId": "order_Qabc5678xyz9012",
  "paymentGatewayKey": "rzp_test_XXXXXXXXXXXXXXX"
}
```

### Response — Failure: No Products (200)

```json
{
  "status": "FAILURE",
  "message": "Product is missing in the order. Please add at least one product to proceed."
}
```

### Response — Failure: Guest Without Phone (200)

```json
{
  "status": "FAILURE",
  "message": "Phone number is required for guest checkout"
}
```

### Response — Failure: Server Error (500)

```json
{
  "status": "FAILURE",
  "message": "An unexpected error occurred during Order Creation. Please try again later."
}
```

---

### Updated `OrderResponseDTO` Field Reference

| Field              | Type       | Description                                                    |
|--------------------|------------|----------------------------------------------------------------|
| `status`           | String     | `success` or `FAILURE`                                         |
| `message`          | String     | Human-readable result message                                  |
| `orderNumber`      | String     | Generated order number (e.g. `ORD-200526103045-000003`)        |
| `subtotalAmount`   | BigDecimal | Cart total **before** shipping (sum of qty × price per item)   |
| `shippingFee`      | BigDecimal | Delivery charge applied (₹0.00 = free delivery)                |
| `isFreeDelivery`   | Boolean    | `true` when `shippingFee == 0`                                 |
| `amount`           | BigDecimal | **Grand total = subtotalAmount + shippingFee** — this is the amount sent to Razorpay |
| `currency`         | String     | Always `INR`                                                   |
| `storeName`        | String     | Store display name                                             |
| `description`      | String     | Payment description                                            |
| `paymentOrderId`   | String     | Razorpay order ID (e.g. `order_Pyz1234abcd5678`)              |
| `paymentGatewayKey`| String     | Razorpay publishable key — use to initialise Razorpay checkout |

---

### Razorpay Notes Payload (stored per order on Razorpay side)

The server passes a `notes` object to Razorpay when creating the payment order so the breakdown is visible in the Razorpay dashboard:

```json
{
  "notes": {
    "subtotal": "350.00",
    "shipping_fee": "50.00",
    "order_number": "ORD-200526103045-000003"
  }
}
```

---

### Example: End-to-end Checkout Sequence

```
1. GET  /api/delivery-charges/calculate?orderAmount=350
        ← shows shipping fee = ₹50 before checkout

2. POST /api/orders  { products: [...] }
        ← server re-verifies prices, calculates delivery charge, creates Razorpay order
        ← returns paymentOrderId + paymentGatewayKey

3. Client opens Razorpay checkout with:
        key:      paymentGatewayKey
        order_id: paymentOrderId
        amount:   amount × 100   (= grand total in paise)

4. PUT  /api/payment-status  { ... }   ← on Razorpay success callback
```

---

### Database Changes — `orders` Table (already existing columns, now populated)

| Column             | Type           | Previously | Now                                        |
|--------------------|----------------|------------|--------------------------------------------|
| `subtotal_amount`  | NUMERIC(10,2)  | NULL       | Sum of all order items (qty × price)       |
| `shipping_fee`     | NUMERIC(10,2)  | NULL       | Delivery charge from matched rule          |
| `tax_amount`       | NUMERIC(10,2)  | NULL       | 0.00 (reserved for future tax logic)       |
| `discount_amount`  | NUMERIC(10,2)  | NULL       | 0.00 (reserved for future coupon logic)    |
| `total_amount`     | NUMERIC(10,2)  | client-sent| `subtotal_amount + shipping_fee` (server-computed) |

