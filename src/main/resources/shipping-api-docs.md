# Shipping API Documentation

**Base URL:** `http://localhost:8080/api`  
**Controller:** `ShippingController`  
**Last Updated:** 2026-08-14

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
   - 8.4 [Get Live Shiprocket PUT Payload by Order Number](#84-get-live-shiprocket-put-payload-by-order-number)
9. [Retrigger Shipping Process](#9-retrigger-shipping-process)

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

#### Tracking History & Order Status Sync Behaviour

Every successful PUT automatically keeps the `shipment_tracking_history` table and the parent order's status in sync with the `shipmentStatus` you send — you no longer need to also pass `historyStatus` just to get a history row recorded.

| Field provided in request      | Tracking history behaviour                                                                                   |
|---------------------------------|----------------------------------------------------------------------------------------------------------------|
| `historyStatus` provided        | A new history row is inserted with `status = historyStatus`, `location = historyLocation`, `remarks = historyRemarks` (as before). |
| `historyStatus` **omitted**, `shipmentStatus` provided | **Auto-fallback:** a history row is inserted with `status = shipmentStatus` and `remarks = "Auto-recorded: current shipment status."`, so the current status is never missing from the history list. |
| Either case                     | **Idempotent** — if a row with that exact status (case-insensitive) already exists for this shipment, no duplicate is inserted and `historyEntryCreated` is `false`. |

Additionally, after the shipment record is saved, the parent `OrderEO.orderStatus` is synced to match:

- If `shipmentStatus` (case-insensitive) equals `DELIVERED` → order status is set to `DELIVERED`.
- Otherwise → order status is set directly to the new `shipmentStatus` value (e.g. `CANCELLED`, `IN_TRANSIT`, `RETURN_REQUESTED`, etc.).
- The order is only written to the DB if its current status actually differs from the derived value (no redundant updates).
- This sync failure is logged but never fails the overall request — a warning is logged if the update can't be applied.

> Example: sending `{"shipmentStatus": "CANCELLED"}` (without `historyStatus`) now (1) inserts a `CANCELLED` tracking-history row if one doesn't already exist, and (2) updates the linked order's `orderStatus` to `CANCELLED` if it isn't already.

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
|--------|---------------------------------------------------------------------|
| 400    | orderNumber or body is null/empty                                 |
| 400    | Order not found for the given order number                        |
| 400    | A non-cancelled shipping record already exists (use PUT to update)|
| 500    | Internal server error                                             |

> Same **Tracking History & Order Status Sync Behaviour** as [PUT 8.2](#tracking-history--order-status-sync-behaviour) applies here — if `historyStatus` is omitted, the current `shipmentStatus` (or the `CREATED` default) is auto-recorded as a history entry, and the linked order's `orderStatus` is synced to match.

---

### 8.4 Get Live Shiprocket PUT Payload by Order Number

Fetches the **live** shipment data directly from the real Shiprocket API for the given internal order number, and returns it in **exactly the same shape** expected by the request body of [PUT /api/shipment/order/{orderNumber}](#82-update-shipping-by-order-number).

Typical usage: call this GET, review/adjust the returned JSON, then send it as the body of the PUT call for the same order number.

> **Note:** This endpoint does **not** read from the local shipping DB at all. The Shiprocket order is located purely via the live Shiprocket "search orders" API (matching on `channel_order_id`, i.e. the internal order number that was sent to Shiprocket at order-creation time). All fields are then populated/refreshed from the live Shiprocket "order details" and "track AWB" APIs. If the live AWB/courier calls fail, the last known values from the order search are used as a fallback.

```
GET /api/shipment/order/{orderNumber}/shiprocket-payload
```

#### Path Parameters

| Parameter   | Type   | Required | Description             |
|-------------|--------|----------|-------------------------|
| orderNumber | String | Yes      | e.g. `ORD-20260601-001` |

#### Response – 200 OK

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Fetched live Shiprocket shipment payload successfully.",
  "warehouseId": null,
  "shiprocketOrderId": 9900001,
  "shiprocketShipmentId": 8800001,
  "awbCode": "AWBXYZ123",
  "courierName": "Delhivery",
  "courierCompanyId": 7,
  "shipmentStatus": "IN_TRANSIT",
  "shipmentType": null,
  "trackingNumber": null,
  "length": null,
  "breadth": null,
  "height": null,
  "weight": null,
  "shippingPrice": null,
  "labelUrl": null,
  "trackUrl": null,
  "estimatedDeliveryDate": null,
  "expectedDeliveryDate": "2026-06-07"
}
```

#### Response Fields

| Field                 | Type       | Description                                                            |
|-----------------------|------------|--------------------------------------------------------------------------|
| responseStatus        | String     | `SUCCESS` or `FAILURE`                                                  |
| responseMessage       | String     | Human-readable result or error                                          |
| warehouseId           | Long       | Not populated by this endpoint (local DB is not consulted); include manually if needed for the PUT body |
| shiprocketOrderId     | Integer    | Shiprocket `order_id`, resolved via live Shiprocket order search        |
| shiprocketShipmentId  | Integer    | Shiprocket `shipment_id`, from live Shiprocket order details            |
| awbCode               | String     | AWB code, from live Shiprocket order details                           |
| courierName           | String     | Courier partner name, refreshed from live AWB tracking (falls back to order details / search result) |
| courierCompanyId      | Integer    | Not currently populated by this endpoint                                |
| shipmentStatus        | String     | Shipment status, refreshed from live AWB tracking (falls back to order details / search result) |
| shipmentType          | String     | Not populated by this endpoint                                          |
| trackingNumber        | String     | Not populated by this endpoint                                          |
| length / breadth / height / weight | Double | Not populated by this endpoint                              |
| shippingPrice         | BigDecimal | Not populated by this endpoint                                          |
| labelUrl / trackUrl   | String     | Not populated by this endpoint                                          |
| estimatedDeliveryDate | String     | Not populated by this endpoint                                          |
| expectedDeliveryDate  | String     | ISO date string (e.g. `2026-06-07`), from live Shiprocket AWB tracking (`edd`) |

Since the returned JSON matches the [PUT 8.2](#82-update-shipping-by-order-number) request body shape, it can be copied — after filling in `warehouseId` and any other fields you want to persist — and sent directly as the PUT request body for the same order number.

#### Error Responses

| Status | Condition                                                     |
|--------|----------------------------------------------------------------|
| 400    | orderNumber is null/empty                                      |
| 404    | No shipment found on Shiprocket for the given order number     |
| 500    | Internal server error                                           |

#### cURL Example

```bash
curl -X GET "http://localhost:8080/api/shipment/order/ORD-20260601-001/shiprocket-payload" \
  -H "accept: application/json"
```

---

## 9. Retrigger Shipping Process

Manually re-run the Shiprocket shipping process (find best courier → generate AWB → request pickup → generate label → track shipment) for an order whose shipment(s) previously failed or need manual intervention (e.g. status `MANUAL_PROCESSING_REQUIRED`, or a shipment stuck without an AWB/label). Used by the admin UI as a one-click retry action.

If a Shiprocket order was already created for a shipment (`shipOrderId` present), the retrigger resumes processing from the courier-selection step onwards instead of creating a duplicate Shiprocket order.

Safe to call multiple times:
- A short cooldown period (`RETRIGGER_SHIPPING_COOLDOWN_MINUTES`) is enforced per shipment based on the last logged attempt, to avoid accidental rapid re-triggering.
- Any shipment that is already fully processed (AWB assigned, pickup scheduled, label generated, tracking URL captured) is skipped rather than reprocessed/duplicated.

```
POST /api/order/{orderNumber}/retrigger-shipping
```

### Path Parameters

| Parameter   | Type   | Required | Description             |
|-------------|--------|----------|--------------------------|
| orderNumber | String | Yes      | e.g. `ORD-20260601-001` |

### Behaviour

All active **FORWARD** shipments under the order (excluding `CANCELLED`/`DELIVERED`) are considered. For each one:

| Result `action` | Meaning                                                                                     |
|------------------|----------------------------------------------------------------------------------------------|
| `RETRIGGERED`    | The Shiprocket flow was re-run and the shipment ended up fully processed, or in the expected `MANUAL_PROCESSING_REQUIRED` stop-state. |
| `SKIPPED`        | Not retriggered — already fully processed, or still within the cooldown window since the last attempt. |
| `FAILED`         | The retrigger ran but the shipment still isn't fully processed afterward, or an unexpected exception was thrown while retriggering. `failedStep` and `failureReason` are populated to explain why. |

When a shipment ends in `FAILED`, the service looks up the most recent `FAILED` row in `shiprocket_order_log` for that shipment to determine:
- `failedStep` — the Shiprocket step that failed, e.g. `GENERATE_AWB`, `REQUEST_PICKUP`, `GENERATE_LABEL`. If the retrigger call itself threw an unexpected exception (rather than an internal step failure), `failedStep` is reported as `RETRIGGER`.
- `failureReason` — the actual error message captured for that step (`shiprocket_order_log.error_message`), or the (root-cause) exception message if the failure came from an unhandled exception.

### Response – 200 OK (retrigger succeeded for at least one shipment)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Retriggered shipping process for 1 of 1 shipment(s) under orderNumber=ORD-20260601-001",
  "orderId": 55,
  "orderNumber": "ORD-20260601-001",
  "results": [
    {
      "shipmentId": 101,
      "trackingNumber": "TRK-ORD-20260601-001_1",
      "previousStatus": "MANUAL_PROCESSING_REQUIRED",
      "currentStatus": "PICKUP_SCHEDULED",
      "action": "RETRIGGERED",
      "message": "Resumed processing for existing Shiprocket order_id=9900001",
      "failedStep": null,
      "failureReason": null
    }
  ]
}
```

### Response – 200 OK (nothing to do, already processed)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "All 1 shipment(s) under orderNumber=ORD-20260601-001 are already fully processed; nothing to retrigger",
  "orderId": 55,
  "orderNumber": "ORD-20260601-001",
  "results": [
    {
      "shipmentId": 101,
      "trackingNumber": "TRK-ORD-20260601-001_1",
      "previousStatus": "IN_TRANSIT",
      "currentStatus": "IN_TRANSIT",
      "action": "SKIPPED",
      "message": "Shipment is already fully processed (AWB=AWBXYZ123, pickup scheduled, label & tracking generated); retrigger is not needed",
      "failedStep": null,
      "failureReason": null
    }
  ]
}
```

### Response – 400 Bad Request (nothing could be retriggered / all failed)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "No shipment could be retriggered for orderNumber=ORD-20260601-001. Reason(s): shipmentId=101 [GENERATE_AWB]: No serviceable courier found for pickup pincode 600001 and delivery pincode 400001",
  "orderId": 55,
  "orderNumber": "ORD-20260601-001",
  "results": [
    {
      "shipmentId": 101,
      "trackingNumber": "TRK-ORD-20260601-001_1",
      "previousStatus": "MANUAL_PROCESSING_REQUIRED",
      "currentStatus": "MANUAL_PROCESSING_REQUIRED",
      "action": "FAILED",
      "message": "Retrigger attempt did not complete successfully at step 'GENERATE_AWB'. Reason: No serviceable courier found for pickup pincode 600001 and delivery pincode 400001",
      "failedStep": "GENERATE_AWB",
      "failureReason": "No serviceable courier found for pickup pincode 600001 and delivery pincode 400001"
    }
  ]
}
```

When the top-level request itself fails (not tied to a specific shipment — e.g. order not found, unexpected exception before/around the per-shipment loop), `responseMessage` includes the (root-cause) exception message directly and `results` is empty.

### Response Fields

| Field                     | Type    | Description                                                                 |
|---------------------------|---------|-------------------------------------------------------------------------------|
| responseStatus            | String  | `SUCCESS` or `FAILURE`                                                        |
| responseMessage           | String  | Human-readable summary. On failure, aggregates all per-shipment failure reasons in the form `shipmentId=X [STEP]: reason; shipmentId=Y [STEP]: reason`. |
| orderId                   | Long    | Internal order ID                                                              |
| orderNumber               | String  | Echoed order number                                                           |
| results                   | Array   | One entry per active FORWARD shipment considered                              |
| results[].shipmentId      | Long    | Internal shipment ID                                                          |
| results[].trackingNumber  | String  | Internal tracking number                                                      |
| results[].previousStatus  | String  | Shipment status before the retrigger attempt                                  |
| results[].currentStatus   | String  | Shipment status after the retrigger attempt (may be unchanged)                |
| results[].action          | String  | `RETRIGGERED`, `SKIPPED`, or `FAILED`                                          |
| results[].message         | String  | Human-readable outcome/reason for this shipment                               |
| results[].failedStep      | String  | Populated only when `action=FAILED` — the Shiprocket step that failed (e.g. `GENERATE_AWB`, `REQUEST_PICKUP`, `GENERATE_LABEL`), or `RETRIGGER` for an unexpected exception. `null` otherwise. |
| results[].failureReason   | String  | Populated only when `action=FAILED` — the detailed error message for the failed step or exception. `null` otherwise. |

### Error Responses

| Status | Condition                                                                 |
|--------|-----------------------------------------------------------------------------|
| 400    | orderNumber is null/blank                                                    |
| 400    | No order found for orderNumber                                              |
| 400    | No active FORWARD shipment found to retrigger (none exist, or all CANCELLED/DELIVERED) |
| 400    | No shipment under the order could be retriggered (see `responseMessage` for aggregated reasons) |
| 500    | Unexpected internal server error (`responseMessage` includes the root-cause exception message) |

### cURL Example

```bash
curl -X POST "http://localhost:8080/api/order/ORD-20260601-001/retrigger-shipping" \
  -H "accept: application/json"
```

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

### Fetch live Shiprocket PUT payload for an order
```bash
curl -X GET "http://localhost:8080/api/shipment/order/ORD-20260601-001/shiprocket-payload" \
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

---

## Changelog

- **2026-08-14** — `POST /api/order/{orderNumber}/retrigger-shipping` per-shipment results now include `failedStep` and `failureReason` when `action=FAILED`, surfacing the actual Shiprocket step and error message (from `shiprocket_order_log`, or the underlying exception) instead of a generic failure status. The top-level `responseMessage` now aggregates all individual failure reasons when nothing could be retriggered, and the 500 error handler also reports the root-cause message. See [Section 9](#9-retrigger-shipping-process).
- **2026-08-14** — `PUT`/`POST /api/shipment/order/{orderNumber}` now auto-record the current `shipmentStatus` as a tracking-history entry (idempotent, no duplicates) when `historyStatus` isn't explicitly supplied, and automatically sync the parent order's `orderStatus` to match the new shipment status (with `DELIVERED` mapping preserved). See [8.2](#tracking-history--order-status-sync-behaviour).

