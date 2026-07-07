# Order API Documentation

Base URL: `/api`

---

## Table of Contents

1. [Create Order](#1-create-order)
2. [Get Order Status](#2-get-order-status)
3. [Get Order by ID](#3-get-order-by-id)
4. [Update Order](#4-update-order)
5. [Delete Order](#5-delete-order)
6. [Get Order History](#6-get-order-history)
7. [Get Order + Shipment Details](#7-get-order--shipment-details)
8. [Cancel Order](#8-cancel-order)
9. [Return Order](#9-return-order)
10. [Get Return Requests](#10-get-return-requests)
11. [Approve / Reject Return Request](#11-approve--reject-return-request)
12. [Process Refund](#12-process-refund)
13. [Get Refunds](#13-get-refunds)
14. [Approve Refund Amount](#14-approve-refund-amount)
15. [Create Reason](#15-create-reason)
16. [Get Reasons by Type](#16-get-reasons-by-type)
17. [Get All Reasons](#17-get-all-reasons)
18. [Get Reason by ID](#18-get-reason-by-id)
19. [Update Reason](#19-update-reason)
20. [Delete / Deactivate Reason](#20-delete--deactivate-reason)
21. [Return Policies](#21-return-policies)
22. [Return Policy Conditions](#22-return-policy-conditions)
23. [Return Policy Mappings — Create / Update / Delete / List](#23-return-policy-mappings)
24. [Get Return Policy by Product Variant](#24-get-return-policy-by-product-variant)
25. [Get Return Policy by Product Categories](#25-get-return-policy-by-product-categories)

---

## 1. Create Order

**`POST /api/orders`**

Creates a new order and initiates the payment flow. Returns a Razorpay order ID and gateway key required to open the payment modal on the frontend.

### Request Body

```json
{
  "status": "PENDING",
  "total": 220,
  "userId": "101",
  "customerId": 55,
  "orderAddressId": 12,
  "products": [
    {
      "productId": "PROD-001",
      "quantity": 2
    }
  ],
  "name": "Ravi Kumar",
  "phone": "9876543210",
  "email": "ravi@example.com",
  "address1": "12, MG Road",
  "address2": "Near City Mall",
  "landmark": "Opposite Bus Stand",
  "city": "Bangalore",
  "state": "Karnataka",
  "country": "India",
  "postalCode": "560001"
}
```

| Field           | Type            | Required | Description                                               |
|-----------------|-----------------|----------|-----------------------------------------------------------|
| `products`      | array           | ✅        | List of products to order. Must not be null or empty.     |
| `products[].productId` | string | ✅        | Product variant identifier                                |
| `products[].quantity`  | integer| ✅        | Quantity to order                                         |
| `total`         | integer         | ✅        | Total order amount in smallest currency unit              |
| `userId`        | string          | ❌        | Logged-in user ID (null for guest checkout)               |
| `customerId`    | integer         | ❌        | Customer ID (if applicable)                               |
| `orderAddressId`| integer         | ❌        | Saved address ID (if applicable)                          |
| `name`          | string          | ❌        | Customer name for shipping                                |
| `phone`         | string          | ❌        | Customer phone                                            |
| `email`         | string          | ❌        | Customer email                                            |
| `address1`      | string          | ❌        | Address line 1                                            |
| `address2`      | string          | ❌        | Address line 2                                            |
| `landmark`      | string          | ❌        | Landmark                                                  |
| `city`          | string          | ❌        | City                                                      |
| `state`         | string          | ❌        | State                                                     |
| `country`       | string          | ❌        | Country                                                   |
| `postalCode`    | string          | ❌        | PIN / ZIP code                                            |

### Response — `200 OK`

```json
{
  "orderNumber": "ORD-20240608-00123",
  "subtotalAmount": 200.00,
  "shippingFee": 20.00,
  "isFreeDelivery": false,
  "amount": 220.00,
  "currency": "INR",
  "storeName": "MyStore",
  "description": "Order for 2 items",
  "paymentOrderId": "order_PQR12345",
  "paymentGatewayKey": "rzp_live_xxx",
  "message": "Order created successfully",
  "status": "SUCCESS"
}
```

| Field              | Type    | Description                                                   |
|--------------------|---------|---------------------------------------------------------------|
| `orderNumber`      | string  | Internal order identifier (e.g. `ORD-20240608-00123`)        |
| `subtotalAmount`   | decimal | Products total before shipping                                |
| `shippingFee`      | decimal | Shipping fee added                                            |
| `isFreeDelivery`   | boolean | `true` if free delivery applied                               |
| `amount`           | decimal | Grand total passed to payment gateway                         |
| `currency`         | string  | Currency code (e.g. `INR`)                                    |
| `storeName`        | string  | Store/brand name                                              |
| `description`      | string  | Payment description                                           |
| `paymentOrderId`   | string  | Razorpay order ID — used to open the payment modal            |
| `paymentGatewayKey`| string  | Razorpay publishable key                                      |
| `message`          | string  | Human-readable result message                                 |
| `status`           | string  | `SUCCESS` or `FAILURE`                                        |

### Error Cases

| Condition                     | `status`  | `message`                          |
|-------------------------------|-----------|------------------------------------|
| `products` is null or empty   | `FAILURE` | Order product missing              |
| Unexpected server error       | `FAILED`  | Technical error (HTTP 500)         |

---

## 2. Get Order Status

**`GET /api/orderStatus/{orderId}`**

Returns the full status and details for a given order. Designed for the **post-payment confirmation / order summary page**.

### Path Parameter

| Parameter | Type   | Description                           |
|-----------|--------|---------------------------------------|
| `orderId` | string | Order number (e.g. `ORD-20240608-00123`) |

### Response — `200 OK`

```json
{
  "orderNumber": "ORD-20240608-00123",
  "status": "CONFIRMED",

  "paymentStatus": "PAID",
  "transactionId": "TXN8473628190",
  "paymentMethod": "UPI",
  "paymentTime": "2026-06-08T15:45:00",
  "total": 220.00,
  "currency": "INR",

  "name": "Ravi Kumar",
  "address1": "12, MG Road",
  "address2": "Near City Mall",
  "landmark": "Opposite Bus Stand",
  "city": "Bangalore",
  "state": "Karnataka",
  "country": "India",
  "postalCode": "560001",
  "deliveryAddressSummary": "12, MG Road, Near City Mall, Bangalore, Karnataka 560001",

  "estimatedDelivery": "10 Jun 2026 – 12 Jun 2026",
  "trackOrderUrl": "https://shiprocket.co/tracking/AWB001",
  "awbCode": "AWB001",

  "products": [
    {
      "productId": "PROD-001",
      "title": "Blue Cotton T-Shirt (M)",
      "quantity": 2,
      "mainImagePath": "https://cdn.example.com/img/prod001.jpg",
      "isReturnable": "Y",
      "returnPolicy": {
        "policyId": 1,
        "name": "7-Day Return",
        "description": "Return within 7 days",
        "returnWindowDays": 7,
        "isReturnable": true,
        "refundType": "ORIGINAL_PAYMENT",
        "returnMethod": "PICKUP",
        "conditions": []
      }
    }
  ]
}
```

#### Response Fields

**Order**

| Field         | Type   | Description                                       |
|---------------|--------|---------------------------------------------------|
| `orderNumber` | string | Internal order number                             |
| `status`      | string | Order status (e.g. `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`) |

**Payment**

| Field           | Type            | Description                                         |
|-----------------|-----------------|-----------------------------------------------------|
| `paymentStatus` | string          | Payment status (e.g. `PAID`, `PENDING`, `FAILED`)  |
| `transactionId` | string          | Gateway transaction ID                              |
| `paymentMethod` | string          | Payment method (e.g. `UPI`, `CARD`, `NET_BANKING`)  |
| `paymentTime`   | ISO datetime    | When the payment was captured                       |
| `total`         | decimal         | Amount paid                                         |
| `currency`      | string          | Currency code (e.g. `INR`)                          |

**Shipping Address**

| Field                    | Type   | Description                                                       |
|--------------------------|--------|-------------------------------------------------------------------|
| `name`                   | string | Recipient name                                                    |
| `address1`               | string | Address line 1                                                    |
| `address2`               | string | Address line 2                                                    |
| `landmark`               | string | Landmark                                                          |
| `city`                   | string | City                                                              |
| `state`                  | string | State                                                             |
| `country`                | string | Country                                                           |
| `postalCode`             | string | PIN/ZIP code                                                      |
| `deliveryAddressSummary` | string | Single-line display address (built from non-null address fields)  |

**Delivery / Shipment** _(null if shipment not yet created)_

| Field              | Type   | Description                                                      |
|--------------------|--------|------------------------------------------------------------------|
| `estimatedDelivery`| string | Formatted date range, e.g. `"10 Jun 2026 – 12 Jun 2026"`. Single date if both dates are the same. |
| `trackOrderUrl`    | string | Courier tracking page URL (use as href for "Track Order" button) |
| `awbCode`          | string | AWB / courier tracking number                                    |

**Products**

| Field                          | Type    | Description                                     |
|--------------------------------|---------|-------------------------------------------------|
| `products[].productId`         | string  | Product variant ID                              |
| `products[].title`             | string  | Product name and variant details                |
| `products[].quantity`          | integer | Ordered quantity                                |
| `products[].mainImagePath`     | string  | Product thumbnail URL                           |
| `products[].isReturnable`      | string  | `Y` or `N`                                      |
| `products[].returnPolicy`      | object  | Return policy for this product (see below)      |

**Return Policy Object**

| Field                          | Type    | Description                                     |
|--------------------------------|---------|-------------------------------------------------|
| `policyId`                     | long    | Policy ID                                       |
| `name`                         | string  | Policy name                                     |
| `description`                  | string  | Policy description                              |
| `returnWindowDays`             | integer | Number of days within which return is allowed   |
| `isReturnable`                 | boolean | Whether item is returnable                      |
| `refundType`                   | string  | e.g. `ORIGINAL_PAYMENT`, `STORE_CREDIT`         |
| `returnMethod`                 | string  | e.g. `PICKUP`, `DROP_OFF`                       |
| `conditions`                   | array   | List of conditions (see Return Policy Conditions)|

---

## 3. Get Order by ID

**`GET /api/orders/{order_id}`**

Returns basic order details by order number.

### Path Parameter

| Parameter  | Type   | Description  |
|------------|--------|--------------|
| `order_id` | string | Order number |

### Response — `200 OK`

```json
{
  "orderId": 1001,
  "userId": 55,
  "name": "Ravi Kumar",
  "phone": "9876543210",
  "email": "ravi@example.com",
  "address1": "12, MG Road",
  "address2": "Near City Mall",
  "landmark": "Opposite Bus Stand",
  "city": "Bangalore",
  "postalCode": "560001",
  "country": "India",
  "status": "CONFIRMED",
  "total": 220.00
}
```

---

## 4. Update Order

**`PUT /api/orders/{order_id}`**

Updates an existing order. Uses the same request body as [Create Order](#1-create-order).

### Path Parameter

| Parameter  | Type   | Description  |
|------------|--------|--------------|
| `order_id` | string | Order number |

### Request Body

Same as [Create Order Request Body](#1-create-order).

### Response — `200 OK`

Same as [Get Order by ID Response](#3-get-order-by-id).

---

## 5. Delete Order

**`DELETE /api/orders/{order_id}`**

Soft-deletes an order by marking its status as `D` (Deleted).

### Path Parameter

| Parameter  | Type   | Description  |
|------------|--------|--------------|
| `order_id` | string | Order number |

### Response — `200 OK`

```json
{
  "responseMessage": "Order deleted successfully",
  "responseStatus": "SUCCESS"
}
```

---

## 6. Get Order History

**`GET /api/order-history`**

Returns order history for a user, with optional search and status filters.

### Query Parameters

| Parameter | Type          | Required | Description                                                            |
|-----------|---------------|----------|------------------------------------------------------------------------|
| `userId`  | long          | ✅        | ID of the user whose order history is requested                       |
| `search`  | string        | ❌        | Free-text search on order number, product name, or any address field (recipient name, address lines, city, state, postal code, country) |
| `status`  | string (list) | ❌        | Filter by order status(es). Multiple values allowed: `?status=SHIPPED&status=DELIVERED` |

### Response — `200 OK`

```json
{
  "orders": [
    {
      "orderNumber": "ORD-20240608-00123",
      "status": "DELIVERED",
      "totalAmount": 220.00,
      "currency": "INR",
      "orderId": "1001",
      "orderDate": "2026-06-08T15:45:00",
      "shippingAddress": {
        "name": "Ravi Kumar",
        "address1": "12, MG Road",
        "address2": "Near City Mall",
        "landmark": "Opposite Bus Stand",
        "city": "Bangalore",
        "state": "Karnataka",
        "country": "India",
        "postalCode": "560001"
      },
      "products": [ /* OrderStatusProd — see Get Order Status */ ],
      "shippingProducts": [
        {
          "shipmentId": 301,
          "trackingNumber": "TRK9876543",
          "awb": "AWB9876543"
        }
      ]
    }
  ]
}
```

#### `shippingProducts` Object Fields

| Field            | Type    | Description                                                      |
|------------------|---------|------------------------------------------------------------------|
| `shipmentId`     | long    | Internal shipment ID                                             |
| `trackingNumber` | string  | Courier tracking number assigned to this shipment                |
| `awb`            | string  | AWB (Air Waybill) code assigned by the courier. `null` if AWB has not yet been generated. |

---

## 7. Get Order + Shipment Details

**`GET /api/order-shipment-details`**

Returns orders with their linked shipments and full shipment tracking history. Used by the admin panel for order management.

### Query Parameters

| Parameter          | Type   | Required | Description                                                                                     |
|--------------------|--------|----------|-------------------------------------------------------------------------------------------------|
| `status`           | string | ❌        | Filter by order status (e.g. `PENDING`, `SHIPPED`, `DELIVERED`)                                |
| `orderCreatedFrom` | string | ❌        | Lower bound for order creation date. Accepts `yyyy-MM-dd` or `yyyy-MM-ddTHH:mm:ss` (inclusive) |
| `orderCreatedTo`   | string | ❌        | Upper bound for order creation date. Accepts `yyyy-MM-dd` or `yyyy-MM-ddTHH:mm:ss` (inclusive) |
| `orderNumber`      | string | ❌        | Exact order number                                                                              |
| `shipmentNumber`   | string | ❌        | AWB / courier tracking number of any linked shipment                                            |

### Response — `200 OK`

```json
{
  "totalCount": 1,
  "orders": [
    {
      "orderId": 1001,
      "orderNumber": "ORD-20240608-00123",
      "orderStatus": "SHIPPED",
      "paymentStatus": "PAID",
      "currency": "INR",
      "subtotalAmount": 200.00,
      "taxAmount": 0.00,
      "shippingFee": 20.00,
      "discountAmount": 0.00,
      "totalAmount": 220.00,
      "orderCreatedAt": "2026-06-08T15:45:00",
      "customerName": "Ravi Kumar",
      "customerEmail": "ravi@example.com",
      "customerPhone": "9876543210",
      "shippingAddress": { /* OrderAddressDTO */ },
      "billingAddress": { /* OrderAddressDTO */ },
      "shipments": [
        {
          "shipmentId": 301,
          "trackingNumber": "AWB001",
          "courierName": "Delhivery",
          "courierCompanyId": 85,
          "shipmentType": "FORWARD",
          "shipmentStatus": "IN_TRANSIT",
          "awb": "AWB001",
          "labelUrl": "https://shiprocket.co/label/AWB001",
          "shipOrderId": 5001,
          "shipShipmentId": 6001,
          "shippedDate": "2026-06-09T10:00:00",
          "deliveredDate": null,
          "estimatedDeliveryDate": "2026-06-10T00:00:00",
          "expectedDeliveryDate": "2026-06-12T00:00:00",
          "pickupScheduledDate": "2026-06-09T09:00:00",
          "trackUrl": "https://shiprocket.co/tracking/AWB001",
          "length": 10.0,
          "breadth": 8.0,
          "height": 5.0,
          "weight": 0.5,
          "shippingPrice": 45.00,
          "courierCandidates": [
            {
              "id": 1,
              "courierCompanyId": 85,
              "courierName": "Delhivery",
              "rate": 45.00,
              "estimatedDeliveryDays": 2.0,
              "rank": 1,
              "isSelected": true,
              "awbCode": "AWB001",
              "shippingPrice": 45.00,
              "createdAt": "2026-06-08T15:45:00"
            }
          ],
          "createdAt": "2026-06-08T15:45:00",
          "updatedAt": "2026-06-09T10:00:00",
          "shipmentHistory": [
            {
              "status": "PICKED UP",
              "location": "Bangalore Hub",
              "remarks": "Shipment picked up",
              "date": "2026-06-09T10:00:00"
            }
          ]
        }
      ]
    }
  ]
}
```

#### Shipment Fields

| Field                    | Type     | Description                                                            |
|--------------------------|----------|------------------------------------------------------------------------|
| `shipmentType`           | string   | `FORWARD` (delivery) or `RETURN_PICKUP` (reverse logistics)           |
| `courierCandidates`      | array    | All couriers evaluated during serviceability check, ranked by priority |
| `shipmentHistory`        | array    | Full tracking events in chronological order                            |

---

## 8. Cancel Order

**`POST /api/order-cancel`**

Cancels an existing order.

### Request Body

```json
{
  "orderNumber": "ORD-20240608-00123",
  "reasonCode": "REASON-001",
  "comment": "Changed my mind"
}
```

| Field         | Type   | Required | Description                              |
|---------------|--------|----------|------------------------------------------|
| `orderNumber` | string | ✅        | Order number to cancel                   |
| `reasonCode`  | string | ❌        | Cancellation reason code from reason master |
| `comment`     | string | ❌        | Optional free-text comment               |

### Response — `200 OK`

```json
{
  "responseMessage": "Order cancelled successfully",
  "responseStatus": "SUCCESS"
}
```

### Error Cases

| Condition                    | `responseStatus` | `responseMessage`           |
|------------------------------|------------------|-----------------------------|
| `orderNumber` is null/empty  | `FAILURE`        | Order number is required    |
| Server error                 | `FAILURE`        | Cancellation failed (HTTP 500) |

---

## 9. Return Order

**`POST /api/order-return`**

Submits a return request for an order, with optional proof images. Uses `multipart/form-data`.

### Request (multipart/form-data)

| Part                  | Type            | Required | Description                                   |
|-----------------------|-----------------|----------|-----------------------------------------------|
| `returnOrderRequest`  | JSON string     | ✅        | Return request details (see below)            |
| `images`              | file(s)         | ❌        | Proof images (JPEG/PNG etc.)                  |

**`returnOrderRequest` JSON Part**

```json
{
  "orderNumber": "ORD-20240608-00123",
  "userId": 101,
  "reasonCode": "REASON-002",
  "comments": "Product was damaged on arrival"
}
```

| Field         | Type    | Required | Description                                 |
|---------------|---------|----------|---------------------------------------------|
| `orderNumber` | string  | ✅        | Order number to return                      |
| `userId`      | integer | ✅        | ID of the user submitting the return        |
| `reasonCode`  | string  | ❌        | Return reason code from reason master       |
| `comments`    | string  | ❌        | Additional comments                         |

### Response — `200 OK`

```json
{
  "responseMessage": "Return request submitted successfully",
  "responseStatus": "SUCCESS"
}
```

---

## 10. Get Return Requests

**`GET /api/return-requests`**

Retrieves return requests, with optional filters.

### Query Parameters

| Parameter     | Type   | Required | Description                                                  |
|---------------|--------|----------|--------------------------------------------------------------|
| `orderNumber` | string | ❌        | Filter by specific order number                              |
| `status`      | string | ❌        | Filter by return status (e.g. `PENDING`, `APPROVED`, `REJECTED`) |

### Response — `200 OK`

```json
[
  {
    "id": 10,
    "returnId": "RET-ORD-20240608-00123-1717839900000",
    "orderNumber": "ORD-20240608-00123",
    "userId": 101,
    "returnType": "RETURN",
    "reasonCode": "REASON-002",
    "reasonDescription": "Product damaged",
    "status": "PENDING",
    "userComments": "Product was damaged on arrival",
    "carrier": null,
    "reverseTrackingNumber": null,
    "pickupScheduledDate": null,
    "pickupCompletedDate": null,
    "warehouseReceivedDate": null,
    "qcStatus": null,
    "qcRemarks": null,
    "inspectedAt": null,
    "refundAmount": null,
    "paymentId": null,
    "refundId": null,
    "images": [
      {
        "id": 1,
        "imageUrl": "https://cdn.example.com/returns/img1.jpg",
        "imageType": "PROOF"
      }
    ],
    "statusHistory": [
      {
        "id": 1,
        "newStatus": "PENDING",
        "activityType": "CREATED",
        "remarks": "Return request created",
        "changedBy": "CUSTOMER",
        "changedAt": "2026-06-08T15:45:00"
      }
    ],
    "returnPolicy": { /* ReturnPolicyDetailDTO */ }
  }
]
```

---

## 11. Approve / Reject Return Request

**`POST /api/return-requests/approve`**

Approves or rejects a return request.

### Request Body

```json
{
  "returnId": "RET-ORD-20240608-00123-1717839900000",
  "status": "APPROVED",
  "comments": "Return approved after QC",
  "userId": 5
}
```

| Field      | Type   | Required | Description                                    |
|------------|--------|----------|------------------------------------------------|
| `returnId` | string | ✅        | Return ID to act upon                          |
| `status`   | string | ✅        | `APPROVED` or `REJECTED`                       |
| `comments` | string | ❌        | Admin remarks                                  |
| `userId`   | long   | ✅        | ID of the admin user performing the action     |

### Response — `200 OK`

```json
{
  "responseMessage": "Return request approved",
  "responseStatus": "SUCCESS"
}
```

### Error Cases

| Condition                | `responseStatus` | `responseMessage`                     |
|--------------------------|------------------|---------------------------------------|
| `returnId` null/empty    | `FAILURE`        | Return ID is required                 |
| `status` null/empty      | `FAILURE`        | Status is required (APPROVED / REJECTED) |
| `userId` is null         | `FAILURE`        | User ID is required                   |
| Server error             | `FAILURE`        | Failed to process return request (HTTP 500) |

---

## 12. Process Refund

**`POST /api/refund`**

Initiates a refund for a payment transaction via the refund reference number.

### Query Parameter

| Parameter           | Type   | Required | Description                            |
|---------------------|--------|----------|----------------------------------------|
| `refundReferenceNo` | string | ✅        | Refund reference number to process     |

### Response — `200 OK`

```json
{
  "responseMessage": "Refund processed successfully",
  "responseStatus": "SUCCESS"
}
```

---

## 13. Get Refunds

**`GET /api/refunds`**

Returns a list of refund records with optional filters.

### Query Parameters

| Parameter     | Type          | Required | Description                                                    |
|---------------|---------------|----------|----------------------------------------------------------------|
| `status`      | string (list) | ❌        | Filter by refund status(es). E.g. `?status=PENDING&status=SUCCESS` |
| `createdFrom` | ISO datetime  | ❌        | Lower bound for refund creation time (`yyyy-MM-ddTHH:mm:ss`)  |
| `createdTo`   | ISO datetime  | ❌        | Upper bound for refund creation time (`yyyy-MM-ddTHH:mm:ss`)  |
| `orderNumber` | string        | ❌        | Filter by specific order number                               |

### Response — `200 OK`

```json
[
  {
    "refundTransactionId": 501,
    "refundReference": "REF-2024-001",
    "gatewayRefundId": "rfnd_PQR123",
    "status": "SUCCESS",
    "refundType": "FULL",
    "refundReason": "Order cancelled",
    "failureReason": null,
    "requestedAmount": 220.00,
    "approvedAmount": 220.00,
    "refundedAmount": 220.00,
    "currency": "INR",
    "createdAt": "2026-06-08T16:00:00",
    "customerName": "Ravi Kumar",
    "customerMobile": "9876543210",
    "order": {
      "orderNumber": "ORD-20240608-00123",
      "orderStatus": "CANCELLED",
      "totalAmount": 220.00,
      "currency": "INR",
      "orderDate": "2026-06-08T15:45:00",
      "items": [
        {
          "productName": "Blue Cotton T-Shirt",
          "skuCode": "SKU-001-M",
          "quantity": 2,
          "unitPrice": 100.00,
          "totalPrice": 200.00
        }
      ]
    }
  }
]
```

---

## 14. Approve Refund Amount

**`POST /api/refunds/approve`**

Sets the approved refund amount for a pending refund before it is processed.

### Request Body

```json
{
  "refundReference": "REF-2024-001",
  "approvedAmount": 180.00
}
```

| Field              | Type    | Required | Description                                   |
|--------------------|---------|----------|-----------------------------------------------|
| `refundReference`  | string  | ✅        | Refund reference number                       |
| `approvedAmount`   | decimal | ✅        | Approved refund amount                        |

### Response — `200 OK`

```json
{
  "responseMessage": "Approved amount updated successfully",
  "responseStatus": "SUCCESS"
}
```

---

## 15. Create Reason

**`POST /api/create-reason`**

Creates a new cancellation, return, or exchange reason.

### Request Body

```json
{
  "reasonCode": "REASON-001",
  "reasonDescription": "Changed my mind",
  "type": "CANCELLATION"
}
```

| Field                | Type   | Required | Description                                              |
|----------------------|--------|----------|----------------------------------------------------------|
| `reasonCode`         | string | ✅        | Unique reason code                                       |
| `reasonDescription`  | string | ✅        | Human-readable description                               |
| `type`               | string | ✅        | `CANCELLATION`, `RETURN`, or `EXCHANGE`                  |

### Response — `200 OK`

```json
{
  "responseMessage": "Reason created successfully",
  "responseStatus": "SUCCESS"
}
```

---

## 16. Get Reasons by Type

**`GET /api/reasons/type/{type}`**

Returns all active reasons for a given type.

### Path Parameter

| Parameter | Type   | Description                                     |
|-----------|--------|-------------------------------------------------|
| `type`    | string | `CANCELLATION`, `RETURN`, or `EXCHANGE`         |

### Response — `200 OK`

```json
[
  {
    "reasonCode": "REASON-001",
    "reasonDescription": "Changed my mind",
    "type": "CANCELLATION"
  }
]
```

---

## 17. Get All Reasons

**`GET /api/reasons`**

Returns all reasons with optional status and type filters.

### Query Parameters

| Parameter | Type   | Required | Description                                                |
|-----------|--------|----------|------------------------------------------------------------|
| `status`  | string | ❌        | Filter by status: `A` (active) or `I` (inactive)          |
| `type`    | string | ❌        | Filter by type: `CANCELLATION`, `RETURN`, or `EXCHANGE`   |

### Response — `200 OK`

```json
{
  "reasons": [
    {
      "id": 1,
      "reasonCode": "REASON-001",
      "reasonDescription": "Changed my mind",
      "type": "CANCELLATION",
      "status": "A",
      "responseStatus": null,
      "responseMessage": null
    }
  ],
  "responseStatus": "SUCCESS",
  "responseMessage": null
}
```

---

## 18. Get Reason by ID

**`GET /api/reason/{id}`**

Returns a single reason by its database ID.

### Path Parameter

| Parameter | Type | Description   |
|-----------|------|---------------|
| `id`      | long | Reason ID     |

### Response — `200 OK`

```json
{
  "id": 1,
  "reasonCode": "REASON-001",
  "reasonDescription": "Changed my mind",
  "type": "CANCELLATION",
  "status": "A",
  "responseStatus": "SUCCESS",
  "responseMessage": null
}
```

---

## 19. Update Reason

**`PUT /api/reason/{id}`**

Updates a reason's description and/or type. `reasonCode` is immutable.

### Path Parameter

| Parameter | Type | Description   |
|-----------|------|---------------|
| `id`      | long | Reason ID     |

### Request Body

```json
{
  "reasonDescription": "Product not needed",
  "type": "CANCELLATION"
}
```

### Response — `200 OK`

Same structure as [Get Reason by ID](#18-get-reason-by-id).

---

## 20. Delete / Deactivate Reason

**`DELETE /api/reason/{id}`**

Soft-deletes a reason by changing its status. No physical row deletion occurs.

### Path Parameter

| Parameter | Type | Description   |
|-----------|------|---------------|
| `id`      | long | Reason ID     |

### Request Body

```json
{
  "status": "I"
}
```

| Field    | Type   | Required | Description                            |
|----------|--------|----------|----------------------------------------|
| `status` | string | ✅        | `I` to deactivate, `A` to reactivate  |

### Response — `200 OK`

```json
{
  "responseMessage": "Reason status updated",
  "responseStatus": "SUCCESS"
}
```

---

## 21. Return Policies

### 21a. Create Return Policy

**`POST /api/return-policies`**

```json
{
  "name": "7-Day Return",
  "description": "Items can be returned within 7 days",
  "returnWindowDays": 7,
  "isReturnable": true
}
```

### 21b. Update Return Policy

**`PUT /api/return-policies/{policy_id}`**

Same request body as create. `policy_id` is a path variable (long).

### 21c. Get All Return Policies

**`GET /api/return-policies`**

#### Response — `200 OK`

```json
[
  {
    "id": 1,
    "name": "7-Day Return",
    "description": "Items can be returned within 7 days",
    "returnWindowDays": 7,
    "isReturnable": true,
    "refundType": "ORIGINAL_PAYMENT",
    "returnMethod": "PICKUP"
  }
]
```

---

## 22. Return Policy Conditions

### 22a. Create Condition

**`POST /api/return-policy-conditions`**

```json
{
  "policyId": 1,
  "conditionType": "ITEM_CONDITION",
  "conditionValue": "UNUSED"
}
```

### 22b. Update Condition

**`PUT /api/return-policy-conditions/{condition_id}`**

Same request body as create. `condition_id` is a path variable (long).

### 22c. Get Conditions

**`GET /api/return-policy-conditions`**

| Parameter  | Type | Required | Description                          |
|------------|------|----------|--------------------------------------|
| `policyId` | long | ❌        | Filter conditions by policy ID       |

#### Response — `200 OK`

```json
[
  {
    "id": 1,
    "policyId": 1,
    "conditionType": "ITEM_CONDITION",
    "conditionValue": "UNUSED"
  }
]
```

---

## 23. Return Policy Mappings

### Create Mapping

**`POST /api/return-policy-mappings`**

Maps a return policy to a specific entity (product, category, or global).

```json
{
  "policyId": 1,
  "entityType": "PRODUCTS",
  "entityId": 42,
  "priority": 10
}
```

| Field        | Type    | Required | Description                                                          |
|--------------|---------|----------|----------------------------------------------------------------------|
| `policyId`   | long    | ✅        | Return policy ID to map                                              |
| `entityType` | string  | ✅        | `PRODUCTS`, `CATEGORY`, or `GLOBAL`                                  |
| `entityId`   | long    | ✅        | ID of the entity (product ID, category ID, or `0` for GLOBAL)        |
| `priority`   | integer | ❌        | Higher = more specific. Defaults to `0`.                             |

#### Response — `200 OK`

```json
{
  "responseMessage": "Return policy mapping created successfully with id: 5",
  "responseStatus": "SUCCESS"
}
```

---

### Update Mapping

**`PUT /api/return-policy-mappings/{mappingId}`**

Updates an existing return-policy mapping. Only `policyId` and `priority` can be changed. `entityType` and `entityId` are **immutable** after creation.

#### Path Parameter

| Parameter   | Type | Description        |
|-------------|------|--------------------|
| `mappingId` | long | Mapping record ID  |

#### Request Body (all fields optional — only supplied fields are updated)

```json
{
  "policyId": 2,
  "priority": 20
}
```

| Field      | Type    | Required | Description                       |
|------------|---------|----------|-----------------------------------|
| `policyId` | long    | ❌        | New return policy ID to link       |
| `priority` | integer | ❌        | Updated priority value             |

#### Response — `200 OK`

```json
{
  "responseMessage": "Return policy mapping updated successfully",
  "responseStatus": "SUCCESS"
}
```

#### Error Responses

```json
{ "responseMessage": "Return policy mapping not found for id: 99", "responseStatus": "FAILURE" }
{ "responseMessage": "Return policy not found for id: 99",         "responseStatus": "FAILURE" }
```

---

### Delete Mapping

**`DELETE /api/return-policy-mappings/{mappingId}`**

Permanently deletes a return-policy mapping record.

#### Path Parameter

| Parameter   | Type | Description       |
|-------------|------|-------------------|
| `mappingId` | long | Mapping record ID |

#### Response — `200 OK`

```json
{
  "responseMessage": "Return policy mapping deleted successfully",
  "responseStatus": "SUCCESS"
}
```

#### Error Response

```json
{ "responseMessage": "Return policy mapping not found for id: 99", "responseStatus": "FAILURE" }
```

---

### List Mappings

**`GET /api/return-policy-mappings`**

Returns all return-policy mappings, optionally filtered by `entityType` and/or `entityId`.

#### Query Parameters (all optional)

| Parameter    | Type   | Description                                                  |
|--------------|--------|--------------------------------------------------------------|
| `entityType` | string | Filter by entity type: `PRODUCTS`, `CATEGORY`, or `GLOBAL`  |
| `entityId`   | long   | Filter by entity ID                                          |

**Examples:**
```
GET /api/return-policy-mappings                          → all mappings
GET /api/return-policy-mappings?entityType=PRODUCTS      → all product-level mappings
GET /api/return-policy-mappings?entityType=PRODUCTS&entityId=42  → mappings for product 42
GET /api/return-policy-mappings?entityType=CATEGORY&entityId=3   → mappings for category 3
```

#### Response — `200 OK`

```json
[
  {
    "id": 1,
    "policyId": 1,
    "policyName": "7-Day Return",
    "entityType": "PRODUCTS",
    "entityId": 42,
    "priority": 10
  },
  {
    "id": 2,
    "policyId": 2,
    "policyName": "No Return",
    "entityType": "PRODUCTS",
    "entityId": 43,
    "priority": 5
  },
  {
    "id": 3,
    "policyId": 1,
    "policyName": "7-Day Return",
    "entityType": "CATEGORY",
    "entityId": 3,
    "priority": 10
  },
  {
    "id": 4,
    "policyId": 3,
    "policyName": "Global Default",
    "entityType": "GLOBAL",
    "entityId": 0,
    "priority": 1
  }
]
```

---

## 24. Get Return Policy by Product Variant

**`GET /api/return-policy/product-variant/{productVariantId}`**

Returns the effective return policy for a specific product variant. Resolves the highest-priority mapping (PRODUCT_VARIANT → CATEGORY → GLOBAL).

### Path Parameter

| Parameter          | Type | Description            |
|--------------------|------|------------------------|
| `productVariantId` | long | Product variant ID     |

### Response — `200 OK`

```json
{
  "policyId": 1,
  "name": "7-Day Return",
  "description": "Items can be returned within 7 days",
  "returnWindowDays": 7,
  "isReturnable": true,
  "refundType": "ORIGINAL_PAYMENT",
  "returnMethod": "PICKUP",
  "conditions": [
    {
      "id": 1,
      "policyId": 1,
      "conditionType": "ITEM_CONDITION",
      "conditionValue": "UNUSED"
    }
  ]
}
```

### Response — `404 Not Found`

Returned when no policy mapping exists for the given product variant.

---

## 25. Get Return Policy by Product Categories

**`GET /api/return-policy/by-category?categoryIds=1,2,3`**

Returns the effective return policy for each supplied product-category ID, along with the category name.

**Lookup resolution order per category:**
1. **CATEGORY** mapping — mapping where `entityType = "CATEGORY"` and `entityId = categoryId` (highest priority wins)
2. **PRODUCTS** mapping — if no CATEGORY mapping exists, fetches all active products in that category, then looks for a mapping where `entityType = "PRODUCTS"` and `entityId` matches any product in the category (highest priority among all products wins)
3. **GLOBAL** fallback — mapping where `entityType = "GLOBAL"` and `entityId = 0`
4. `null` — if no mapping found at any level

### Query Parameter

| Parameter     | Type           | Required | Description |
|---------------|----------------|----------|-------------|
| `categoryIds` | List\<Long\>   | **Yes**  | Comma-separated list of product category IDs (e.g. `?categoryIds=1,2,3`) |

### Example Request

```
GET /api/return-policy/by-category?categoryIds=1,2,3
```

### Response — `200 OK`

Array of `CategoryReturnPolicyResponseDTO` — one entry per requested category ID, preserving input order.

| Field            | Type   | Description |
|------------------|--------|-------------|
| `categoryId`     | Long   | The requested category ID |
| `categoryName`   | String | Category display name (null if category not found) |
| `resolvedVia`    | String | Which level the policy was resolved at: `"CATEGORY"`, `"PRODUCTS"`, `"GLOBAL"`, or `null` (not found) |
| `returnPolicy`   | Object | Effective return policy (see below). `null` if no mapping found |
| `mappedProducts` | Array  | List of products with individual PRODUCTS-level mappings. **Populated only when `resolvedVia = "PRODUCTS"`**. `null` otherwise |

**`returnPolicy` object:**

| Field              | Type    | Description |
|--------------------|---------|-------------|
| `policyId`         | Long    | Return policy ID |
| `name`             | String  | Policy name |
| `description`      | String  | Policy description |
| `returnWindowDays` | Integer | Number of days within which return is allowed |
| `isReturnable`     | Boolean | Whether the product is returnable |
| `refundType`       | String  | e.g. `"ORIGINAL_PAYMENT"`, `"STORE_CREDIT"` |
| `returnMethod`     | String  | e.g. `"PICKUP"`, `"DROP_OFF"` |
| `conditions`       | Array   | List of policy conditions (see below) |

**`conditions[]` item:**

| Field            | Type   | Description |
|------------------|--------|-------------|
| `id`             | Long   | Condition record ID |
| `policyId`       | Long   | Parent policy ID |
| `conditionType`  | String | e.g. `"ITEM_CONDITION"`, `"TIME_LIMIT"` |
| `conditionValue` | String | e.g. `"UNUSED"`, `"7_DAYS"` |

**`mappedProducts[]` item** *(only present when `resolvedVia = "PRODUCTS"`)*:

| Field             | Type    | Description |
|-------------------|---------|-------------|
| `productId`       | Long    | Internal product ID |
| `productName`     | String  | Product display name |
| `productSlug`     | String  | Product URL slug |
| `mappingPriority` | Integer | Priority value of this product's mapping (higher = more specific) |
| `returnPolicy`    | Object  | The return policy mapped specifically to this product (same shape as `returnPolicy` above) |

### Example Response — All Serviceable

```json
[
  {
    "categoryId": 1,
    "categoryName": "Sweets",
    "resolvedVia": "CATEGORY",
    "returnPolicy": {
      "policyId": 1,
      "name": "7-Day Return",
      "description": "Items can be returned within 7 days of delivery",
      "returnWindowDays": 7,
      "isReturnable": true,
      "refundType": "ORIGINAL_PAYMENT",
      "returnMethod": "PICKUP",
      "conditions": [
        {
          "id": 1,
          "policyId": 1,
          "conditionType": "ITEM_CONDITION",
          "conditionValue": "UNUSED"
        }
      ]
    },
    "mappedProducts": null
  },
  {
    "categoryId": 2,
    "categoryName": "Beverages",
    "resolvedVia": "PRODUCTS",
    "returnPolicy": {
      "policyId": 2,
      "name": "No Return",
      "description": "Perishable items cannot be returned",
      "returnWindowDays": 0,
      "isReturnable": false,
      "refundType": null,
      "returnMethod": null,
      "conditions": []
    },
    "mappedProducts": [
      {
        "productId": 42,
        "productName": "Mango Juice",
        "productSlug": "mango-juice",
        "mappingPriority": 10,
        "returnPolicy": {
          "policyId": 2,
          "name": "No Return",
          "description": "Perishable items cannot be returned",
          "returnWindowDays": 0,
          "isReturnable": false,
          "refundType": null,
          "returnMethod": null,
          "conditions": []
        }
      },
      {
        "productId": 43,
        "productName": "Orange Juice",
        "productSlug": "orange-juice",
        "mappingPriority": 5,
        "returnPolicy": {
          "policyId": 3,
          "name": "3-Day Return",
          "description": "Return within 3 days if sealed",
          "returnWindowDays": 3,
          "isReturnable": true,
          "refundType": "STORE_CREDIT",
          "returnMethod": "DROP_OFF",
          "conditions": []
        }
      }
    ]
  },
  {
    "categoryId": 3,
    "categoryName": "Snacks",
    "resolvedVia": "GLOBAL",
    "returnPolicy": {
      "policyId": 4,
      "name": "Global Default",
      "description": "Default return policy for all unmapped products",
      "returnWindowDays": 7,
      "isReturnable": true,
      "refundType": "ORIGINAL_PAYMENT",
      "returnMethod": "PICKUP",
      "conditions": []
    },
    "mappedProducts": null
  },
  {
    "categoryId": 99,
    "categoryName": "Uncategorised",
    "resolvedVia": null,
    "returnPolicy": null,
    "mappedProducts": null
  }
]
```

> **Note:** Categories with no CATEGORY-level mapping automatically look for a PRODUCTS-level mapping (any product within the category). If still not found, falls back to the GLOBAL policy. If none exist, `returnPolicy` is `null`.

### Response — `400 Bad Request`

Returned when `categoryIds` is missing or empty.

### How to create a CATEGORY mapping

Use `POST /api/return-policy-mappings` with:

```json
{
  "policyId": 1,
  "entityType": "CATEGORY",
  "entityId": 1,
  "priority": 10
}
```

### How to create a PRODUCTS mapping (used as category-level fallback)

```json
{
  "policyId": 1,
  "entityType": "PRODUCTS",
  "entityId": 42,
  "priority": 5
}
```

### How to create a GLOBAL fallback mapping

```json
{
  "policyId": 2,
  "entityType": "GLOBAL",
  "entityId": 0,
  "priority": 1
}
```

---

## Common Response Schemas

### `ResponseDTO`

```json
{
  "responseMessage": "string",
  "responseStatus": "SUCCESS | FAILURE"
}
```

### `OrderAddressDTO`

```json
{
  "name": "string",
  "address1": "string",
  "address2": "string",
  "landmark": "string",
  "city": "string",
  "state": "string",
  "country": "string",
  "postalCode": "string"
}
```

### Order Status Values

| Status      | Description                                  |
|-------------|----------------------------------------------|
| `PENDING`   | Order placed, payment not yet confirmed      |
| `CONFIRMED` | Payment received, awaiting fulfillment       |
| `SHIPPED`   | Shipment dispatched                          |
| `DELIVERED` | Delivered to customer                        |
| `CANCELLED` | Order cancelled                              |
| `D`         | Soft-deleted                                 |

### Payment Status Values

| Status    | Description             |
|-----------|-------------------------|
| `PAID`    | Payment captured        |
| `PENDING` | Awaiting payment        |
| `FAILED`  | Payment failed          |
| `REFUNDED`| Fully refunded          |

### Shipment Type Values

| Type             | Description                        |
|------------------|------------------------------------|
| `FORWARD`        | Outward delivery shipment          |
| `RETURN_PICKUP`  | Reverse logistics / return pickup  |

