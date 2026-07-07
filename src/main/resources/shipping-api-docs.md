# Shipping API Documentation

**Base URL:** `http://localhost:8080/api`  
**Controller:** `ShippingController`  
**Last Updated:** 2026-07-02

---

## Table of Contents

1. [Shipping History](#1-shipping-history)
2. [List All Shipments](#2-list-all-shipments)
3. [Update Shipment Status](#3-update-shipment-status)
4. [Carton Management](#4-carton-management)
   - 4.1 [Get All Cartons](#41-get-all-cartons)
   - 4.2 [Get Carton by ID](#42-get-carton-by-id)
   - 4.3 [Create Carton](#43-create-carton)
   - 4.4 [Update Carton](#44-update-carton)
   - 4.5 [Delete (Deactivate) Carton](#45-delete-deactivate-carton)
5. [Manual Shiprocket Steps](#5-manual-shiprocket-steps)
   - 5.1 [Generate AWB](#51-generate-awb)
   - 5.2 [Request Pickup](#52-request-pickup)
   - 5.3 [Generate Label](#53-generate-label)
6. [Courier Candidate Logs](#6-courier-candidate-logs)
   - 6.1 [By Shipment ID](#61-courier-candidates-by-shipment-id)
   - 6.2 [By Order ID](#62-courier-candidates-by-order-id)
7. [Manual Shiprocket Override](#7-manual-shiprocket-override)
8. [Order-Number Based Shipment Management](#8-order-number-based-shipment-management)
   - 8.1 [Get Shipping Details by Order Number](#81-get-shipping-details-by-order-number)
   - 8.2 [Update Shipping by Order Number](#82-update-shipping-by-order-number)
   - 8.3 [Create Shipping by Order Number](#83-create-shipping-by-order-number)

---

## 1. Shipping History

Fetch the shipment tracking history for an internal tracking number.

```
GET /api/shipping-history/{trackingNumber}
```

### Path Parameters

| Parameter      | Type   | Required | Description                       |
|----------------|--------|----------|-----------------------------------|
| trackingNumber | String | Yes      | Internal tracking number (e.g. `TRK-ORD12345_1`) |

### Response – 200 OK

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "...",
  "history": [
    {
      "status": "IN_TRANSIT",
      "location": "Mumbai Hub",
      "remarks": "Package picked up by courier",
      "date": "2026-06-30T10:20:00"
    }
  ]
}
```

### Error Responses

| Status | Condition                     |
|--------|-------------------------------|
| 400    | trackingNumber is null/empty  |
| 500    | Internal server error         |

---

## 2. List All Shipments

Retrieve all shipment records, optionally filtered by status and/or order number.

```
GET /api/shipments
```

### Query Parameters

| Parameter   | Type   | Required | Description                                                        |
|-------------|--------|----------|--------------------------------------------------------------------|
| status      | String | No       | Filter by shipment status (e.g. `CREATED`, `IN_TRANSIT`, `DELIVERED`) |
| orderNumber | String | No       | Filter by order number (e.g. `ORD-20260601-001`)                   |

### Example

```
GET /api/shipments?status=IN_TRANSIT&orderNumber=ORD-20260601-001
```

### Response – 200 OK

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Shipments fetched successfully. Total: 2",
  "shipments": [
    {
      "shipmentId": 101,
      "orderId": 55,
      "orderNumber": "ORD-20260601-001",
      "trackingNumber": "TRK-ORD-20260601-001_1",
      "awb": "AWBXYZ123",
      "courierName": "Delhivery",
      "courierCompanyId": 7,
      "shipmentType": "FORWARD",
      "shipmentStatus": "IN_TRANSIT",
      "shippedDate": "2026-06-02T08:00:00",
      "deliveredDate": null,
      "shippingPrice": 45.00,
      "createdAt": "2026-06-01T12:00:00",
      "updatedAt": "2026-06-02T08:05:00",
      "trackingHistory": [ ... ],
      "courierCandidates": [ ... ]
    }
  ]
}
```

### Error Responses

| Status | Condition             |
|--------|-----------------------|
| 500    | Internal server error |

---

## 3. Update Shipment Status

Update the status of a shipment by tracking number and optionally add a tracking history entry.

```
POST /api/shipment-status-update
```

### Request Body

```json
{
  "trackingNumber": "TRK-ORD-20260601-001_1",
  "status": "DELIVERED",
  "location": "Customer Doorstep",
  "remarks": "Delivered successfully",
  "eventTime": "2026-06-05T14:30:00"
}
```

| Field          | Type            | Required | Description                        |
|----------------|-----------------|----------|------------------------------------|
| trackingNumber | String          | Yes      | Internal tracking number           |
| status         | String          | Yes      | New shipment status                |
| location       | String          | No       | Location of the event              |
| remarks        | String          | No       | Additional remarks                 |
| eventTime      | LocalDateTime   | No       | Timestamp of the event             |

### Response – 200 OK

```json
{
  "status": "SUCCESS",
  "statusMessage": "Shipment status updated successfully"
}
```

### Error Responses

| Status | Condition                              |
|--------|----------------------------------------|
| 400    | trackingNumber or status is null/empty |
| 500    | Internal server error                  |

---

## 4. Carton Management

### 4.1 Get All Cartons

Retrieve all cartons. Optionally filter by status.

```
GET /api/cartons?status=A
```

#### Query Parameters

| Parameter | Type   | Required | Description                              |
|-----------|--------|----------|------------------------------------------|
| status    | String | No       | `A` = Active, `I` = Inactive             |

#### Response – 200 OK

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Cartons fetched successfully. Total: 3",
  "cartons": [
    {
      "id": 1,
      "name": "Small Box",
      "length": 20.0,
      "breadth": 15.0,
      "height": 10.0,
      "maxWeight": 2000.0,
      "emptyWeight": 250.0,
      "status": "A",
      "who": "admin"
    }
  ]
}
```

---

### 4.2 Get Carton by ID

```
GET /api/carton/{id}
```

#### Path Parameters

| Parameter | Type | Required | Description      |
|-----------|------|----------|------------------|
| id        | Long | Yes      | Carton ID (PK)   |

#### Response – 200 OK

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Carton fetched successfully",
  "id": 1,
  "name": "Small Box",
  "length": 20.0,
  "breadth": 15.0,
  "height": 10.0,
  "maxWeight": 2000.0,
  "emptyWeight": 250.0,
  "status": "A",
  "who": "admin"
}
```

#### Error Responses

| Status | Condition        |
|--------|------------------|
| 400    | id is null       |
| 200    | Carton not found (`responseStatus: FAILURE`) |

---

### 4.3 Create Carton

```
POST /api/carton
```

#### Request Body

```json
{
  "name": "Medium Box",
  "length": 30.0,
  "breadth": 25.0,
  "height": 20.0,
  "maxWeight": 5000.0,
  "emptyWeight": 400.0,
  "who": "admin"
}
```

| Field       | Type   | Required | Description                       |
|-------------|--------|----------|-----------------------------------|
| name        | String | Yes      | Carton name/label                 |
| length      | Double | Yes      | Length in cm                      |
| breadth     | Double | Yes      | Breadth in cm                     |
| height      | Double | Yes      | Height in cm                      |
| maxWeight   | Double | Yes      | Max payload in grams              |
| emptyWeight | Double | Yes      | Empty carton weight in grams      |
| who         | String | No       | Creator identifier (e.g. `admin`) |

#### Response – 200 OK

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Carton created successfully with id: 4",
  "id": 4,
  "name": "Medium Box",
  ...
}
```

#### Error Responses

| Status | Condition              |
|--------|------------------------|
| 400    | Request body is null   |

---

### 4.4 Update Carton

```
PUT /api/carton/{id}
```

#### Path Parameters

| Parameter | Type | Required | Description    |
|-----------|------|----------|----------------|
| id        | Long | Yes      | Carton ID (PK) |

#### Request Body

```json
{
  "name": "Medium Box v2",
  "length": 32.0,
  "breadth": 26.0,
  "height": 22.0,
  "maxWeight": 5500.0,
  "emptyWeight": 420.0,
  "who": "admin"
}
```

#### Response – 200 OK

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Carton updated successfully with id: 4",
  "id": 4,
  ...
}
```

#### Error Responses

| Status | Condition                   |
|--------|-----------------------------|
| 400    | id is null or body is null  |

---

### 4.5 Delete (Deactivate) Carton

Physical deletion is not supported. This endpoint performs a **soft-delete** by changing the carton status.

```
DELETE /api/carton/{id}
```

#### Path Parameters

| Parameter | Type | Required | Description    |
|-----------|------|----------|----------------|
| id        | Long | Yes      | Carton ID (PK) |

#### Request Body

```json
{
  "status": "I",
  "who": "admin"
}
```

| Field  | Type   | Required | Description                          |
|--------|--------|----------|--------------------------------------|
| status | String | Yes      | `A` (Activate) or `I` (Deactivate)   |
| who    | String | No       | Operator identifier                  |

#### Response – 200 OK

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Carton 4 deactivated successfully"
}
```

#### Error Responses

| Status | Condition                            |
|--------|--------------------------------------|
| 400    | id null / status null or not A or I  |

---

## 5. Manual Shiprocket Steps

These endpoints allow manually triggering individual steps of the Shiprocket automation flow. Useful when the automated event-driven pipeline fails at a specific step.

### 5.1 Generate AWB

Manually generate or assign an AWB code for a Shiprocket shipment.

```
POST /api/shipment/generate-awb
```

#### Request Body

```json
{
  "shipmentId": 12345,
  "courierId": 7
}
```

| Field      | Type    | Required | Description                                          |
|------------|---------|----------|------------------------------------------------------|
| shipmentId | Integer | Yes      | Shiprocket `shipment_id` (from CREATE_ORDER response)|
| courierId  | Integer | No       | Courier company ID. Null = auto-select               |

#### Response – 200 OK

```json
{
  "awbAssignStatus": 1,
  "response": {
    "resolvedAwbCode": "AWBXYZ123",
    "resolvedCourierCompanyId": 7,
    "resolvedCourierName": "Delhivery",
    "resolvedEtd": "2026-06-08"
  }
}
```

#### Error Responses

| Status | Condition              |
|--------|------------------------|
| 400    | shipmentId is null     |

---

### 5.2 Request Pickup

Manually request a courier pickup for one or more Shiprocket shipments.

```
POST /api/shipment/request-pickup
```

#### Request Body

```json
{
  "shipmentId": [12345, 12346]
}
```

| Field      | Type         | Required | Description                              |
|------------|--------------|----------|------------------------------------------|
| shipmentId | List<Integer>| Yes      | List of Shiprocket shipment IDs          |

#### Response – 200 OK

```json
{
  "pickupId": 9876,
  "pickupScheduledDate": "2026-06-03",
  "pickupToken": "TKN-987"
}
```

#### Error Responses

| Status | Condition                               |
|--------|-----------------------------------------|
| 400    | shipmentId list is null or empty        |

---

### 5.3 Generate Label

Manually generate a shipping label PDF for one or more Shiprocket shipments.

```
POST /api/shipment/generate-label
```

#### Request Body

```json
{
  "shipmentId": [12345]
}
```

| Field      | Type         | Required | Description                    |
|------------|--------------|----------|--------------------------------|
| shipmentId | List<Integer>| Yes      | List of Shiprocket shipment IDs|

#### Response – 200 OK

```json
{
  "labelCreated": 1,
  "labelUrl": "https://shiprocket.co/label/AWBXYZ123.pdf"
}
```

#### Error Responses

| Status | Condition                              |
|--------|----------------------------------------|
| 400    | shipmentId list is null or empty       |

---

## 6. Courier Candidate Logs

View the list of couriers that were evaluated during the automated courier selection step.

### 6.1 Courier Candidates by Shipment ID

```
GET /api/shipment/{shipmentId}/courier-candidates
```

#### Path Parameters

| Parameter  | Type | Required | Description                   |
|------------|------|----------|-------------------------------|
| shipmentId | Long | Yes      | Internal shipment ID (PK)     |

#### Response – 200 OK

```json
[
  {
    "id": 1,
    "courierCompanyId": 7,
    "courierName": "Delhivery",
    "rate": 48.50,
    "estimatedDeliveryDays": 3,
    "rank": 1,
    "isSelected": true,
    "awbCode": "AWBXYZ123",
    "shippingPrice": 48.50,
    "createdAt": "2026-06-01T12:30:00"
  },
  {
    "id": 2,
    "courierCompanyId": 12,
    "courierName": "BlueDart",
    "rate": 65.00,
    "estimatedDeliveryDays": 2,
    "rank": 2,
    "isSelected": false,
    "awbCode": null,
    "shippingPrice": null,
    "createdAt": "2026-06-01T12:30:00"
  }
]
```

#### Error Responses

| Status | Condition         |
|--------|-------------------|
| 400    | shipmentId is null|

---

### 6.2 Courier Candidates by Order ID

Returns courier candidates for **all shipments** belonging to a given order, sorted by shipment then rank.

```
GET /api/order/{orderId}/courier-candidates
```

#### Path Parameters

| Parameter | Type | Required | Description            |
|-----------|------|----------|------------------------|
| orderId   | Long | Yes      | Internal order ID (PK) |

#### Response – 200 OK

Same structure as [6.1](#61-courier-candidates-by-shipment-id).

#### Error Responses

| Status | Condition       |
|--------|-----------------|
| 400    | orderId is null |

---

## 7. Manual Shiprocket Override

Used as a **fallback** when the automated Shiprocket pipeline (CREATE_ORDER → GENERATE_AWB → REQUEST_PICKUP → GENERATE_LABEL) fails at any step. Allows an admin to manually supply missing data and keep the `shipping` and `shipment_tracking_history` tables consistent.

> **Note:** This endpoint works on an existing shipping record only.  
> To create a brand-new record, use [POST /api/shipment/order/{orderNumber}](#83-create-shipping-by-order-number).

```
POST /api/shipment/manual-update
```

### Request Body

At least **one** of `shipmentId`, `orderId`, or `orderNumber` must be provided.  
All other fields are optional — only the fields you supply are updated.

```json
{
  "shipmentId": 101,
  "orderId": null,
  "orderNumber": null,

  "shiprocketOrderId": 9900001,
  "shiprocketShipmentId": 8800001,

  "awbCode": "AWBXYZ123",
  "courierName": "Delhivery",
  "courierCompanyId": 7,

  "shipmentStatus": "PICKUP_SCHEDULED",
  "shippingPrice": 48.50,

  "labelUrl": "https://shiprocket.co/label/AWBXYZ123.pdf",
  "trackUrl": "https://shiprocket.co/track/AWBXYZ123",

  "estimatedDeliveryDate": "2026-06-08",
  "expectedDeliveryDate": "2026-06-07",
  "pickupScheduledDate": "2026-06-03",

  "pickupId": 9876,
  "pickupToken": "TKN-987",

  "historyStatus": "PICKUP_SCHEDULED",
  "historyLocation": "Warehouse, Chennai",
  "historyRemarks": "Manual pickup scheduled by admin",

  "step": "REQUEST_PICKUP",
  "notes": "Automated pickup request timed out — manually scheduled"
}
```

#### Request Fields

**Identifiers (at least one required)**

| Field       | Type    | Description                             |
|-------------|---------|-----------------------------------------|
| shipmentId  | Long    | Internal shipment PK (highest priority) |
| orderId     | Long    | Internal order PK                       |
| orderNumber | String  | Human-readable order number             |

**Shiprocket IDs**

| Field                | Type    | Description                              |
|----------------------|---------|------------------------------------------|
| shiprocketOrderId    | Integer | Shiprocket `order_id` from CREATE_ORDER  |
| shiprocketShipmentId | Integer | Shiprocket `shipment_id` from CREATE_ORDER|

**Courier / AWB**

| Field           | Type    | Description                        |
|-----------------|---------|------------------------------------|
| awbCode         | String  | AWB code from GENERATE_AWB step    |
| courierName     | String  | Courier partner name               |
| courierCompanyId| Integer | Shiprocket courier company ID      |

**Status & Pricing**

| Field          | Type       | Description                        |
|----------------|------------|------------------------------------|
| shipmentStatus | String     | New status (e.g. `PICKUP_SCHEDULED`)|
| shippingPrice  | BigDecimal | Shipping cost in INR               |

**URLs**

| Field    | Type   | Description                   |
|----------|--------|-------------------------------|
| labelUrl | String | Shipping label PDF URL        |
| trackUrl | String | Public tracking URL           |

**Dates** (format: `yyyy-MM-dd HH:mm:ss` or `yyyy-MM-dd`)

| Field                 | Type   | Description                   |
|-----------------------|--------|-------------------------------|
| estimatedDeliveryDate | String | Estimated delivery date       |
| expectedDeliveryDate  | String | Expected delivery date (ETD)  |
| pickupScheduledDate   | String | Scheduled pickup date         |

**Pickup**

| Field       | Type   | Description                         |
|-------------|--------|-------------------------------------|
| pickupId    | Long   | Pickup ID from REQUEST_PICKUP       |
| pickupToken | String | Pickup token from REQUEST_PICKUP    |

**Tracking History (optional — creates a new history row)**

| Field           | Type   | Description                             |
|-----------------|--------|-----------------------------------------|
| historyStatus   | String | Status label for the history entry      |
| historyLocation | String | Location for the history entry          |
| historyRemarks  | String | Remarks for the history entry           |

**Audit**

| Field | Type   | Description                           |
|-------|--------|---------------------------------------|
| step  | String | Step label: `CREATE_ORDER`, `GENERATE_AWB`, `REQUEST_PICKUP`, `GENERATE_LABEL`, `MANUAL_OVERRIDE` |
| notes | String | Free-text reason for manual override  |

### Response – 200 OK

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Shipment record updated successfully via manual override.",
  "shipmentId": 101,
  "orderNumber": "ORD-20260601-001",
  "shipmentStatus": "PICKUP_SCHEDULED",
  "shiprocketOrderId": 9900001,
  "shiprocketShipmentId": 8800001,
  "awbCode": "AWBXYZ123",
  "courierName": "Delhivery",
  "courierCompanyId": 7,
  "labelUrl": "https://shiprocket.co/label/AWBXYZ123.pdf",
  "trackUrl": "https://shiprocket.co/track/AWBXYZ123",
  "shippingPrice": 48.50,
  "updatedAt": "2026-07-02T10:15:00",
  "historyEntryCreated": true,
  "stepLogged": "REQUEST_PICKUP"
}
```

### Error Responses

| Status | Condition                                              |
|--------|--------------------------------------------------------|
| 400    | Body is null / no identifier provided / record not found |
| 500    | Internal server error                                  |

---

## 8. Order-Number Based Shipment Management

These three endpoints provide a clean CRUD interface for managing a shipping record using the **order number** as the primary key. They are the recommended way to manually create or update shipment records for failure recovery.

---

### 8.1 Get Shipping Details by Order Number

Fetch the full shipping record (all fields + tracking history) for the given order number.

```
GET /api/shipment/order/{orderNumber}
```

#### Path Parameters

| Parameter   | Type   | Required | Description                  |
|-------------|--------|----------|------------------------------|
| orderNumber | String | Yes      | e.g. `ORD-20260601-001`      |

#### Response – 200 OK

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Shipping details fetched successfully.",
  "shipmentId": 101,
  "orderNumber": "ORD-20260601-001",
  "orderId": 55,
  "shiprocketOrderId": 9900001,
  "shiprocketShipmentId": 8800001,
  "awbCode": "AWBXYZ123",
  "courierName": "Delhivery",
  "courierCompanyId": 7,
  "shipmentStatus": "IN_TRANSIT",
  "shipmentType": "FORWARD",
  "trackingNumber": "TRK-ORD-20260601-001_1",
  "length": 20.0,
  "breadth": 15.0,
  "height": 10.0,
  "weight": 1.2,
  "shippingPrice": 48.50,
  "labelUrl": "https://shiprocket.co/label/AWBXYZ123.pdf",
  "trackUrl": "https://shiprocket.co/track/AWBXYZ123",
  "warehouseId": 1,
  "warehouseName": "Chennai Warehouse",
  "pickupScheduledDate": "2026-06-03T09:00:00",
  "estimatedDeliveryDate": "2026-06-08T00:00:00",
  "expectedDeliveryDate": "2026-06-07T00:00:00",
  "shippedDate": "2026-06-03T10:00:00",
  "deliveredDate": null,
  "createdAt": "2026-06-01T12:00:00",
  "updatedAt": "2026-06-03T10:05:00",
  "pickupId": 9876,
  "pickupToken": "TKN-987",
  "trackingHistory": [
    {
      "status": "CREATED",
      "location": "Chennai Warehouse",
      "remarks": "Order Confirmed shipment will be created.",
      "date": "2026-06-01T12:00:00"
    },
    {
      "status": "PICKUP_SCHEDULED",
      "location": "Chennai Warehouse",
      "remarks": "Pickup scheduled",
      "date": "2026-06-03T09:00:00"
    },
    {
      "status": "IN_TRANSIT",
      "location": "Mumbai Hub",
      "remarks": "In transit to destination",
      "date": "2026-06-04T14:00:00"
    }
  ]
}
```

#### Error Responses

| Status | Condition                               |
|--------|-----------------------------------------|
| 400    | orderNumber is null/empty               |
| 404    | No shipping record found for that order |
| 500    | Internal server error                   |

---

### 8.2 Update Shipping by Order Number

Update an existing shipping record. **Only non-null fields in the request body are applied** — omit a field to leave it unchanged.

```
PUT /api/shipment/order/{orderNumber}
```

#### Path Parameters

| Parameter   | Type   | Required | Description             |
|-------------|--------|----------|-------------------------|
| orderNumber | String | Yes      | e.g. `ORD-20260601-001` |

#### Request Body

All fields are optional. Supply only the fields you want to change.

```json
{
  "warehouseId": 1,
  "shiprocketOrderId": 9900001,
  "shiprocketShipmentId": 8800001,
  "awbCode": "AWBXYZ123",
  "courierName": "Delhivery",
  "courierCompanyId": 7,
  "shipmentStatus": "PICKUP_SCHEDULED",
  "shipmentType": "FORWARD",
  "trackingNumber": "TRK-ORD-20260601-001_1",
  "length": 20.0,
  "breadth": 15.0,
  "height": 10.0,
  "weight": 1.2,
  "shippingPrice": 48.50,
  "labelUrl": "https://shiprocket.co/label/AWBXYZ123.pdf",
  "trackUrl": "https://shiprocket.co/track/AWBXYZ123",
  "pickupScheduledDate": "2026-06-03",
  "estimatedDeliveryDate": "2026-06-08",
  "expectedDeliveryDate": "2026-06-07",
  "shippedDate": "2026-06-03 10:00:00",
  "deliveredDate": null,
  "pickupId": 9876,
  "pickupToken": "TKN-987",
  "historyStatus": "PICKUP_SCHEDULED",
  "historyLocation": "Chennai Warehouse",
  "historyRemarks": "Pickup scheduled manually",
  "notes": "Automated pickup failed — manual override"
}
```

#### Request Fields

| Field                | Type       | Required | Description                                                     |
|----------------------|------------|----------|-----------------------------------------------------------------|
| warehouseId          | Long       | No       | Link shipment to a different warehouse (update only)            |
| shiprocketOrderId    | Integer    | No       | Shiprocket order ID                                             |
| shiprocketShipmentId | Integer    | No       | Shiprocket shipment ID                                          |
| awbCode              | String     | No       | AWB code                                                        |
| courierName          | String     | No       | Courier name                                                    |
| courierCompanyId     | Integer    | No       | Courier company ID                                              |
| shipmentStatus       | String     | No       | New status (e.g. `CREATED`, `PICKUP_SCHEDULED`, `IN_TRANSIT`, `DELIVERED`, `CANCELLED`) |
| shipmentType         | String     | No       | `FORWARD` or `RETURN_PICKUP`                                    |
| trackingNumber       | String     | No       | Internal tracking number                                        |
| length               | Double     | No       | Parcel length (cm)                                              |
| breadth              | Double     | No       | Parcel breadth (cm)                                             |
| height               | Double     | No       | Parcel height (cm)                                              |
| weight               | Double     | No       | Parcel weight (kg)                                              |
| shippingPrice        | BigDecimal | No       | Shipping cost (INR)                                             |
| labelUrl             | String     | No       | Label PDF URL                                                   |
| trackUrl             | String     | No       | Public tracking URL                                             |
| pickupScheduledDate  | String     | No       | Date string `yyyy-MM-dd` or `yyyy-MM-dd HH:mm:ss`              |
| estimatedDeliveryDate| String     | No       | Date string `yyyy-MM-dd` or `yyyy-MM-dd HH:mm:ss`              |
| expectedDeliveryDate | String     | No       | Date string `yyyy-MM-dd` or `yyyy-MM-dd HH:mm:ss`              |
| shippedDate          | String     | No       | Date string `yyyy-MM-dd` or `yyyy-MM-dd HH:mm:ss`              |
| deliveredDate        | String     | No       | Date string `yyyy-MM-dd` or `yyyy-MM-dd HH:mm:ss`              |
| pickupId             | Long       | No       | Pickup ID                                                       |
| pickupToken          | String     | No       | Pickup token                                                    |
| historyStatus        | String     | No       | If set, creates a new tracking history row with this status     |
| historyLocation      | String     | No       | Location for the new tracking history row                       |
| historyRemarks       | String     | No       | Remarks for the new tracking history row                        |
| notes                | String     | No       | Audit note recorded in `shiprocket_order_log`                   |

#### Response – 200 OK

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Shipping record updated successfully.",
  "shipmentId": 101,
  "orderNumber": "ORD-20260601-001",
  "shipmentStatus": "PICKUP_SCHEDULED",
  "shiprocketOrderId": 9900001,
  "shiprocketShipmentId": 8800001,
  "awbCode": "AWBXYZ123",
  "courierName": "Delhivery",
  "courierCompanyId": 7,
  "labelUrl": "https://shiprocket.co/label/AWBXYZ123.pdf",
  "trackUrl": "https://shiprocket.co/track/AWBXYZ123",
  "shippingPrice": 48.50,
  "updatedAt": "2026-07-02T11:00:00",
  "historyEntryCreated": true,
  "stepLogged": "MANUAL_UPDATE"
}
```

#### Error Responses

| Status | Condition                              |
|--------|----------------------------------------|
| 400    | orderNumber or body is null/empty      |
| 404    | No shipping record found for that order (use POST to create one) |
| 500    | Internal server error                  |

---

### 8.3 Create Shipping by Order Number

Create a brand-new shipping record for the given order number. Returns **400** if a non-cancelled shipping record already exists — use **PUT** to update it instead.

```
POST /api/shipment/order/{orderNumber}
```

#### Path Parameters

| Parameter   | Type   | Required | Description             |
|-------------|--------|----------|-------------------------|
| orderNumber | String | Yes      | e.g. `ORD-20260601-001` |

#### Request Body

Same fields as [PUT 8.2](#82-update-shipping-by-order-number).  
`warehouseId` is recommended so the record is linked to the correct warehouse.  
`shipmentStatus` defaults to `CREATED` and `shipmentType` defaults to `FORWARD` if not supplied.

```json
{
  "warehouseId": 1,
  "shiprocketOrderId": 9900001,
  "shiprocketShipmentId": 8800001,
  "awbCode": "AWBXYZ123",
  "courierName": "Delhivery",
  "courierCompanyId": 7,
  "shipmentStatus": "CREATED",
  "shipmentType": "FORWARD",
  "trackingNumber": "TRK-ORD-20260601-001_1",
  "length": 20.0,
  "breadth": 15.0,
  "height": 10.0,
  "weight": 1.2,
  "shippingPrice": 48.50,
  "historyStatus": "CREATED",
  "historyLocation": "Chennai Warehouse",
  "historyRemarks": "Manual shipment record created by admin",
  "notes": "Automated order creation failed — created manually"
}
```

#### Response – 201 Created

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Shipping record created successfully.",
  "shipmentId": 102,
  "orderNumber": "ORD-20260601-001",
  "shipmentStatus": "CREATED",
  "shiprocketOrderId": 9900001,
  "shiprocketShipmentId": 8800001,
  "awbCode": "AWBXYZ123",
  "courierName": "Delhivery",
  "courierCompanyId": 7,
  "labelUrl": null,
  "trackUrl": null,
  "shippingPrice": 48.50,
  "updatedAt": "2026-07-02T11:05:00",
  "historyEntryCreated": true,
  "stepLogged": "MANUAL_CREATE"
}
```

#### Error Responses

| Status | Condition                                                         |
|--------|-------------------------------------------------------------------|
| 400    | orderNumber or body is null/empty                                 |
| 400    | Order not found for the given order number                        |
| 400    | A non-cancelled shipping record already exists (use PUT to update)|
| 500    | Internal server error                                             |

---

## Shipment Status Values

| Status                       | Description                                   |
|------------------------------|-----------------------------------------------|
| `CREATED`                    | Shipment record created, awaiting pickup      |
| `PICKUP_SCHEDULED`           | Pickup has been scheduled                     |
| `IN_TRANSIT`                 | Package is on the way                         |
| `DELIVERED`                  | Package delivered to customer                 |
| `CANCELLED`                  | Shipment cancelled                            |
| `RETURN_REQUESTED`           | Customer has raised a return request          |
| `RETURN_PICKUP_INITIATED`    | Courier pickup for return has been initiated  |
| `RECEIVED`                   | Returned package received at warehouse        |

---

## Shipment Type Values

| Type            | Description                            |
|-----------------|----------------------------------------|
| `FORWARD`       | Regular outgoing delivery shipment     |
| `RETURN_PICKUP` | Return pickup shipment from customer   |

---

## Common Response Fields

| Field           | Type   | Description                     |
|-----------------|--------|---------------------------------|
| responseStatus  | String | `SUCCESS` or `FAILURE`          |
| responseMessage | String | Human-readable result or error  |

---

## cURL Examples

### Fetch shipping details for an order
```bash
curl -X GET "http://localhost:8080/api/shipment/order/ORD-20260601-001" \
  -H "accept: application/json"
```

### Create a new shipping record
```bash
curl -X POST "http://localhost:8080/api/shipment/order/ORD-20260601-001" \
  -H "Content-Type: application/json" \
  -d '{
    "warehouseId": 1,
    "shipmentStatus": "CREATED",
    "shipmentType": "FORWARD",
    "historyStatus": "CREATED",
    "historyRemarks": "Manual record by admin",
    "notes": "Automated flow failed"
  }'
```

### Update AWB and courier for an existing shipment
```bash
curl -X PUT "http://localhost:8080/api/shipment/order/ORD-20260601-001" \
  -H "Content-Type: application/json" \
  -d '{
    "awbCode": "AWBXYZ123",
    "courierName": "Delhivery",
    "courierCompanyId": 7,
    "shipmentStatus": "PICKUP_SCHEDULED",
    "historyStatus": "PICKUP_SCHEDULED",
    "historyLocation": "Chennai Warehouse",
    "notes": "AWB assigned manually"
  }'
```

### Fetch all IN_TRANSIT shipments
```bash
curl -X GET "http://localhost:8080/api/shipments?status=IN_TRANSIT" \
  -H "accept: application/json"
```

### Trigger manual AWB generation
```bash
curl -X POST "http://localhost:8080/api/shipment/generate-awb" \
  -H "Content-Type: application/json" \
  -d '{"shipmentId": 12345, "courierId": 7}'
```

