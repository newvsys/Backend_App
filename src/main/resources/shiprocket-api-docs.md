# Shiprocket / Shipping API Documentation

Base URL: `/api/shipping`

---

## Table of Contents

1. [Create Order](#1-create-order)
2. [Check Serviceability (Simple)](#2-check-serviceability-simple)
3. [Check Serviceability (Full Params)](#3-check-serviceability-full-params)
4. [Check Serviceability by Product Variants](#4-check-serviceability-by-product-variants)
5. [Track Shipment (Raw)](#5-track-shipment-raw)
6. [Track Shipment (Combined)](#6-track-shipment-combined)
7. [Shiprocket Webhook](#7-shiprocket-webhook)

---

## 1. Create Order

Creates a new shipment order in Shiprocket when a customer places an order.

### Request

```
POST /api/shipping/create-order
Content-Type: application/json
```

### Request Body

Free-form JSON map matching the Shiprocket Create Order API payload.

| Field | Type | Required | Description |
|---|---|---|---|
| `order_id` | String | Yes | Your internal order ID |
| `order_date` | String | Yes | Date of order (`yyyy-MM-dd HH:mm`) |
| `pickup_location` | String | Yes | Pickup warehouse name configured in Shiprocket |
| `billing_customer_name` | String | Yes | Customer full name |
| `billing_address` | String | Yes | Billing street address |
| `billing_city` | String | Yes | Billing city |
| `billing_pincode` | String | Yes | Billing PIN code |
| `billing_state` | String | Yes | Billing state |
| `billing_country` | String | Yes | Billing country (e.g. `India`) |
| `billing_email` | String | Yes | Customer email |
| `billing_phone` | String | Yes | Customer phone number |
| `shipping_is_billing` | Boolean | Yes | `true` if shipping address = billing address |
| `order_items` | Array | Yes | List of order item objects (see below) |
| `payment_method` | String | Yes | `"Prepaid"` or `"COD"` |
| `sub_total` | Number | Yes | Order subtotal in rupees |
| `length` | Number | Yes | Package length in cm |
| `breadth` | Number | Yes | Package breadth in cm |
| `height` | Number | Yes | Package height in cm |
| `weight` | Number | Yes | Package weight in kg |

**Order Item object:**

| Field | Type | Description |
|---|---|---|
| `name` | String | Product name |
| `sku` | String | Product SKU |
| `units` | Integer | Quantity |
| `selling_price` | Number | Price per unit in rupees |

### Example Request

```json
{
  "order_id": "ORD-1001",
  "order_date": "2026-06-08 10:30",
  "pickup_location": "Primary Warehouse",
  "billing_customer_name": "Ravi Kumar",
  "billing_address": "12, MG Road",
  "billing_city": "Bangalore",
  "billing_pincode": "560001",
  "billing_state": "Karnataka",
  "billing_country": "India",
  "billing_email": "ravi@example.com",
  "billing_phone": "9876543210",
  "shipping_is_billing": true,
  "order_items": [
    {
      "name": "Mysore Pak",
      "sku": "SKU-001",
      "units": 2,
      "selling_price": 250
    }
  ],
  "payment_method": "Prepaid",
  "sub_total": 500,
  "length": 15,
  "breadth": 10,
  "height": 5,
  "weight": 0.5
}
```

### Response

`200 OK` — Raw response map from Shiprocket API.

```json
{
  "order_id": 987654,
  "shipment_id": 1122334,
  "status": "NEW",
  "status_code": 1,
  "onboarding_completed_now": 0,
  "awb_code": "",
  "courier_company_id": null,
  "courier_name": null
}
```

---

## 2. Check Serviceability (Simple)

Quick delivery availability check using only a destination pincode.

### Request

```
GET /api/shipping/serviceability?delivery={pincode}
```

### Query Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `delivery` | String | Yes | Destination pincode |

### Example Request

```
GET /api/shipping/serviceability?delivery=560001
```

### Response

`200 OK` — Raw serviceability response from Shiprocket API.

```json
{
  "data": {
    "available_courier_companies": [
      {
        "courier_company_id": 1,
        "courier_name": "Delhivery",
        "rate": 55.0,
        "etd": "2026-06-10",
        "cod": 1
      }
    ]
  }
}
```

---

## 3. Check Serviceability (Full Params)

Full serviceability check with all Shiprocket-supported parameters — weight, dimensions, COD, mode of travel, etc.

### Request

```
POST /api/shipping/serviceabilityWithAllParams
Content-Type: application/json
```

### Request Body

| Field | Type | Required | Description |
|---|---|---|---|
| `pickupPostcode` | Integer | Required* | Pickup warehouse PIN code. Defaults to configured warehouse pincode if omitted |
| `deliveryPostcode` | Integer | **Required** | Destination PIN code |
| `cod` | Integer | Conditional | `1` = Cash on Delivery, `0` = Prepaid. Required if `orderId` is not provided |
| `weight` | String | Conditional | Shipment weight in kg (e.g. `"0.5"`). Required if `orderId` is not provided |
| `orderId` | Integer | Optional | Shiprocket order ID if the order is already created |
| `length` | Integer | Optional | Package length in cm |
| `breadth` | Integer | Optional | Package breadth in cm |
| `height` | Integer | Optional | Package height in cm |
| `declaredValue` | Integer | Optional | Declared shipment value in rupees |
| `mode` | String | Optional | Travel mode: `"Surface"` or `"Air"` |
| `isReturn` | Integer | Optional | `1` = return order, `0` = forward order. `declaredValue` required if set to `1` |
| `couriersType` | Integer | Optional | `1` = show only document couriers |
| `onlyLocal` | Integer | Optional | `1` = show only hyperlocal couriers |
| `qcCheck` | Integer | Optional | Filter QC-enabled couriers. `isReturn` must be `1` |

### Example Request

```json
{
  "pickupPostcode": 560001,
  "deliveryPostcode": 400001,
  "weight": "0.5",
  "cod": 0,
  "length": 15,
  "breadth": 10,
  "height": 5,
  "declaredValue": 500,
  "mode": "Surface"
}
```

### Response

`200 OK` — Raw serviceability response from Shiprocket API (same structure as Simple Serviceability).

`400 Bad Request` — If `deliveryPostcode` is missing.

---

## 4. Check Serviceability by Product Variants

Checks Shiprocket delivery serviceability for one or more product variants.  
For each variant the service:
1. Finds all warehouses that hold stock for that variant (via inventory records).
2. Uses each warehouse's postal code as the **pickup postcode**.
3. Calls the Shiprocket serviceability API for every **(warehouse → deliveryPostcode)** pair.
4. Returns `serviceable: true` only when **all** pairs return ≥ 1 available courier.

> **Default warehouse fallback:** If no inventory is found for a variant, or a warehouse record has no postal code, the system automatically falls back to the **default warehouse** (configured as `DEFAULT_WAREHOUSE_NAME` in constants) for the serviceability check. The response includes `usingDefaultWarehouse: true` on those entries.

### Request

```
POST /api/shipping/serviceability/by-variants
Content-Type: application/json
```

### Request Body

| Field | Type | Required | Description |
|---|---|---|---|
| `deliveryPostcode` | String | **Required** | Destination PIN code to check delivery to |
| `productVariantIds` | Array\<Long\> | **Required** | One or more product variant IDs to check (≥ 1) |

### Example Request

```json
{
  "deliveryPostcode": "560001",
  "productVariantIds": [1, 2, 3]
}
```

### Response — `200 OK`

| Field | Type | Description |
|---|---|---|
| `serviceable` | Boolean | `true` only when every warehouse→destination pair for every variant is serviceable |
| `deliveryPostcode` | String | The destination postcode that was checked |
| `message` | String | Human-readable summary |
| `variants` | Array | Per-variant breakdown (see below) |

**`variants[]` item:**

| Field | Type | Description |
|---|---|---|
| `productVariantId` | Long | Product variant ID |
| `serviceable` | Boolean | `true` only when all warehouses for this variant are serviceable |
| `warehouses` | Array | Per-warehouse breakdown (see below) |

**`warehouses[]` item:**

| Field | Type | Description |
|---|---|---|
| `warehouseId` | Long | Internal warehouse ID |
| `warehouseName` | String | Warehouse display name |
| `warehousePostalCode` | String | Pickup postal code used for the check |
| `serviceable` | Boolean | `true` if ≥ 1 courier available for this warehouse→destination pair |
| `availableCourierCount` | Integer | Number of available courier companies returned by Shiprocket |
| `usingDefaultWarehouse` | Boolean | `true` if the default warehouse was used as fallback (no inventory / no postal code found for the variant's actual warehouse) |
| `reason` | String | Populated only when `serviceable = false` — explains why (e.g. `"No courier companies available for this route"`) |

### Example Response — All Serviceable

```json
{
  "serviceable": true,
  "deliveryPostcode": "560001",
  "message": "Delivery is available from all warehouses to the requested postcode",
  "variants": [
    {
      "productVariantId": 1,
      "serviceable": true,
      "warehouses": [
        {
          "warehouseId": 1,
          "warehouseName": "Chennai Central Warehouse",
          "warehousePostalCode": "600058",
          "serviceable": true,
          "availableCourierCount": 4,
          "usingDefaultWarehouse": false,
          "reason": null
        }
      ]
    },
    {
      "productVariantId": 2,
      "serviceable": true,
      "warehouses": [
        {
          "warehouseId": 2,
          "warehouseName": "Coimbatore North Warehouse",
          "warehousePostalCode": "641021",
          "serviceable": true,
          "availableCourierCount": 3,
          "usingDefaultWarehouse": false,
          "reason": null
        }
      ]
    }
  ]
}
```

### Example Response — Fallback to Default Warehouse (No Inventory Found)

```json
{
  "serviceable": true,
  "deliveryPostcode": "560001",
  "message": "Delivery is available from all warehouses to the requested postcode",
  "variants": [
    {
      "productVariantId": 5,
      "serviceable": true,
      "warehouses": [
        {
          "warehouseId": 1,
          "warehouseName": "Default Warehouse",
          "warehousePostalCode": "600058",
          "serviceable": true,
          "availableCourierCount": 4,
          "usingDefaultWarehouse": true,
          "reason": null
        }
      ]
    }
  ]
}
```

### Example Response — Not Serviceable

```json
{
  "serviceable": false,
  "deliveryPostcode": "737121",
  "message": "Delivery is not available from one or more warehouses to the requested postcode",
  "variants": [
    {
      "productVariantId": 1,
      "serviceable": false,
      "warehouses": [
        {
          "warehouseId": 1,
          "warehouseName": "Chennai Central Warehouse",
          "warehousePostalCode": "600058",
          "serviceable": false,
          "availableCourierCount": 0,
          "usingDefaultWarehouse": false,
          "reason": "No courier companies available for this route"
        }
      ]
    }
  ]
}
```

### Error Responses — `400 Bad Request`

```json
{ "serviceable": false, "message": "deliveryPostcode is required" }
```

```json
{ "serviceable": false, "message": "productVariantIds must contain at least one ID" }
```

### Fallback Logic Summary

| Situation | Behaviour |
|---|---|
| No inventory records found for a variant | Use default warehouse for the check; `usingDefaultWarehouse: true` |
| Inventory found but warehouse is null | Use default warehouse; `usingDefaultWarehouse: true` |
| Warehouse found but postal code is blank | Use default warehouse's postal code; `usingDefaultWarehouse: true` |
| Default warehouse also has no postal code | Return `serviceable: false` with descriptive `reason` |
| Default warehouse not configured at all | Return `serviceable: false` with descriptive `reason` |

---

## 5. Get Available Courier Services (Excluding Blocklisted)

Returns the list of courier services available for a given order, **excluding** any courier whose `courier_company_id` is present in the internal courier blocklist (`Constants.BLOCKLISTED_COURIER_COMPANY_IDS`). Useful for letting the customer/admin explicitly pick a courier instead of relying on auto-selection, while ensuring blocked couriers are never shown.

Pickup postcode, delivery postcode, weight and dimensions are **resolved internally** from the order's existing shipment (if any, preferring an active non-cancelled `FORWARD` shipment) and its shipping address — no request body is required. This mirrors the internal serviceability lookup used by the automated Shiprocket order-processing flow.

### Request

```
GET /api/shipping/available-courier-services/{orderId}
```

### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `orderId` | Long | Yes | Internal order id (`OrderEO.orderId`) |

### Internal Resolution Logic

| Value | Resolved From |
|---|---|
| Delivery postcode | Order's shipping address (`OrderAddressEO.postalCode`) |
| Pickup postcode | Warehouse of the order's preferred shipment; falls back to the default warehouse (`Constants.DEFAULT_WAREHOUSE_NAME`) |
| Weight | Existing shipment's recorded weight (`ShippingEO.weight`), floored at a minimum of `1.1` kg |
| Length / Breadth / Height | Existing shipment's recorded dimensions, if present and greater than `0` |
| COD | `0` (Prepaid) if the order's payment status is `PAID`, otherwise `1` (Cash on Delivery) |

### Example Request

```
GET /api/shipping/available-courier-services/1024
```

### Response — `200 OK`

| Field | Type | Description |
|---|---|---|
| `responseStatus` | String | `"SUCCESS"` or `"FAILURE"` |
| `responseMessage` | String | Human-readable result message |
| `totalCount` | Integer | Number of courier services returned (after excluding blocklisted couriers) |
| `currentlyUsedCourierId` | Integer | The `courier_company_id` currently assigned to the order's shipment (`ShippingEO.courierCompanyId`), if a shipment record exists. `null` if no shipment exists yet or no courier has been assigned |
| `courierServices` | Array | List of available courier service objects (see below) |

**`courierServices[]` item:**

| Field | Type | Description |
|---|---|---|
| `courierId` | Integer | Shiprocket `courier_company_id` |
| `courierName` | String | Courier display name (e.g. `"Delhivery Surface"`) |
| `price` | Double | Freight/shipping rate for this courier |
| `codCharges` | Double | Additional COD handling charges, if applicable |
| `otherCharges` | Double | Any other charges levied by the courier |
| `estimatedDeliveryDays` | Double | Estimated number of days for delivery |
| `estimatedDeliveryDate` | String | Estimated delivery date as returned by Shiprocket (raw `etd` field) |
| `rating` | Double | Courier's overall rating (out of 5), if provided |
| `codAvailable` | Boolean | Whether Cash-on-Delivery is available with this courier |
| `isAir` | Boolean | Whether this is an air-shipping courier option |
| `isSurface` | Boolean | Whether this is a surface-shipping courier option |

### Example Response

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Fetched 3 available courier service(s).",
  "totalCount": 3,
  "currentlyUsedCourierId": 10,
  "courierServices": [
    {
      "courierId": 10,
      "courierName": "Delhivery Surface",
      "price": 55.5,
      "codCharges": 20.0,
      "otherCharges": 0.0,
      "estimatedDeliveryDays": 3.0,
      "estimatedDeliveryDate": "Sep 12, 2026",
      "rating": 4.2,
      "codAvailable": true,
      "isAir": false,
      "isSurface": true
    },
    {
      "courierId": 22,
      "courierName": "Xpressbees Air",
      "price": 89.0,
      "codCharges": 25.0,
      "otherCharges": 0.0,
      "estimatedDeliveryDays": 1.0,
      "estimatedDeliveryDate": "Sep 10, 2026",
      "rating": 3.9,
      "codAvailable": true,
      "isAir": true,
      "isSurface": false
    }
  ]
}
```

> **Blocklist behaviour:** Any courier whose `courier_company_id` is in `Constants.BLOCKLISTED_COURIER_COMPANY_IDS` (e.g. `54`) is silently omitted from `courierServices` — it will never appear in the response even if Shiprocket returns it as serviceable.

### Error Responses

| Status | Condition |
|---|---|
| 400 | `orderId` is missing/null, order not found, no shipping address / delivery postcode found, unable to resolve a pickup postcode, or invalid postcode values (`responseStatus: FAILURE`) |
| 502 | Shiprocket serviceability API call failed (`responseStatus: FAILURE`) |
| 500 | Unexpected internal server error (`responseStatus: FAILURE`) |

### cURL Example

```bash
curl -X GET "http://localhost:8080/api/shipping/available-courier-services/1024"
```

---


## 6. Track Shipment (Raw)

Returns live tracking data directly from the Shiprocket API for a given AWB code.

### Request

```
GET /api/shipping/track/{awb}
```

### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `awb` | String | Yes | Courier AWB (Air Waybill) tracking number |

### Example Request

```
GET /api/shipping/track/14492489597159
```

### Response

`200 OK` — Raw live tracking response from Shiprocket API.

```json
{
  "tracking_data": {
    "track_status": 1,
    "shipment_track": [
      {
        "awb_code": "14492489597159",
        "courier_company_id": 1,
        "courier_name": "Delhivery",
        "current_status": "Delivered",
        "delivered_date": "2026-06-07 14:30:00"
      }
    ],
    "shipment_track_activities": [
      {
        "date": "2026-06-07 14:30:00",
        "activity": "Delivered",
        "location": "Mumbai, Maharashtra"
      }
    ]
  }
}
```

---

## 7. Track Shipment (Combined)

Returns a combined view of:
- Internal shipment record from the database
- Local tracking history stored in DB
- Live tracking data fetched from Shiprocket API
- Courier selection log

### Request

```
GET /api/shipping/track-shipment/{awbCode}
```

### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `awbCode` | String | Yes | Courier AWB tracking number |

### Example Request

```
GET /api/shipping/track-shipment/14492489597159
```

### Response

`200 OK`

| Field | Type | Description |
|---|---|---|
| `responseStatus` | String | `"SUCCESS"` or `"FAILURE"` |
| `responseMessage` | String | Human-readable status message |
| `shipmentId` | Long | Internal DB shipment ID |
| `awbCode` | String | AWB / courier tracking number |
| `trackingNumber` | String | Internal tracking number |
| `courierName` | String | Courier company name |
| `courierCompanyId` | Integer | Shiprocket courier company ID |
| `shipmentStatus` | String | Current shipment status |
| `shipmentType` | String | `"FORWARD"` or `"RETURN"` |
| `shippedDate` | DateTime | Date the shipment was dispatched |
| `deliveredDate` | DateTime | Actual delivery date (if delivered) |
| `estimatedDeliveryDate` | DateTime | ETD from Shiprocket |
| `expectedDeliveryDate` | DateTime | Expected delivery date |
| `pickupScheduledDate` | DateTime | Scheduled pickup date |
| `labelUrl` | String | URL to download the shipping label |
| `trackUrl` | String | Shiprocket tracking page URL |
| `orderId` | Long | Internal order ID linked to this shipment |
| `orderNumber` | String | Order number string |
| `shippingPrice` | Decimal | Actual shipping charge in rupees |
| `trackingHistory` | Array | Local tracking history events (see below) |
| `courierCandidates` | Array | All couriers evaluated during serviceability check |
| `shiprocketTracking` | Object | Raw live tracking data from Shiprocket API |

**Tracking History item:**

| Field | Type | Description |
|---|---|---|
| `status` | String | Status label (e.g. `"In Transit"`) |
| `location` | String | City / location of scan |
| `remarks` | String | Additional remarks |
| `eventTime` | DateTime | Date-time of this tracking event |

### Example Response

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Shipment tracked successfully",
  "shipmentId": 42,
  "awbCode": "14492489597159",
  "trackingNumber": "TRK-20260608-001",
  "courierName": "Delhivery",
  "courierCompanyId": 1,
  "shipmentStatus": "Delivered",
  "shipmentType": "FORWARD",
  "shippedDate": "2026-06-05T10:00:00",
  "deliveredDate": "2026-06-07T14:30:00",
  "estimatedDeliveryDate": "2026-06-08T00:00:00",
  "labelUrl": "https://shiprocket.co/label/123",
  "trackUrl": "https://shiprocket.co/tracking/14492489597159",
  "orderId": 1001,
  "orderNumber": "ORD-1001",
  "shippingPrice": 55.00,
  "trackingHistory": [
    {
      "status": "Delivered",
      "location": "Mumbai, Maharashtra",
      "remarks": "Delivered",
      "eventTime": "2026-06-07T14:30:00"
    },
    {
      "status": "In Transit",
      "location": "Pune Hub",
      "remarks": "In Transit",
      "eventTime": "2026-06-06T08:00:00"
    }
  ],
  "courierCandidates": [],
  "shiprocketTracking": { }
}
```

---

## 8. Shiprocket Webhook

Receives real-time shipment status update notifications from Shiprocket. Shiprocket calls this endpoint automatically whenever a shipment status changes (e.g. Picked Up → In Transit → Out for Delivery → Delivered).

> **Note:** This endpoint is called by **Shiprocket**, not by the frontend. It must be configured in the Shiprocket dashboard under **Settings → Webhooks**.

> Always returns HTTP `200 OK` regardless of outcome to prevent Shiprocket from retrying on business-logic errors.

### Request

```
POST /api/shipping/webhook
Content-Type: application/json
```

### Request Body (`ShiprocketWebhookDTO`)

| Field | Type | Required | Description |
|---|---|---|---|
| `awb` | String | **Required** | AWB / courier tracking number |
| `current_status` | String | **Required** | Human-readable status (e.g. `"Delivered"`, `"In Transit"`) |
| `current_status_id` | Integer | Optional | Shiprocket numeric status code |
| `shipment_id` | String/Integer | Optional | Shiprocket internal shipment ID |
| `order_id` | String/Integer | Optional | Shiprocket / merchant order ID |
| `etd` | String | Optional | Estimated delivery date (`"yyyy-MM-dd HH:mm:ss"`) |
| `scans` | Array | Optional | List of scan events — newest first (see below) |

**Scan Event object:**

| Field | Type | Description |
|---|---|---|
| `date` | String | Scan date-time (`"yyyy-MM-dd HH:mm:ss"`) |
| `activity` | String | Activity description (e.g. `"Delivered"`) |
| `location` | String | City / location of the scan |
| `sr-status` | String | Shiprocket numeric status string |
| `sr-status-label` | String | Shiprocket status label |

### Example Request

```json
{
  "awb": "14492489597159",
  "current_status": "Delivered",
  "current_status_id": 7,
  "shipment_id": 123456789,
  "order_id": "ORD-1001",
  "etd": "2026-06-08 16:00:00",
  "scans": [
    {
      "date": "2026-06-07 14:30:00",
      "activity": "Delivered",
      "location": "Mumbai, Maharashtra",
      "sr-status": "7",
      "sr-status-label": "DELIVERED"
    },
    {
      "date": "2026-06-06 08:00:00",
      "activity": "In Transit",
      "location": "Pune Hub",
      "sr-status": "6",
      "sr-status-label": "IN_TRANSIT"
    }
  ]
}
```

### Processing Logic

1. Validates `awb` and `current_status` are present.
2. Looks up the internal `ShippingEO` record by AWB code.
3. Extracts `location` and `eventTime` from the latest scan event (index 0).
4. Delegates to `ShippingService.shipmentStatusUpdate()` which:
   - Updates shipment status in DB
   - Appends a tracking history entry
   - Updates the linked order status
   - Triggers refund / return flows if applicable

### Response

`200 OK` (always)

| Field | Type | Description |
|---|---|---|
| `status` | String | `"SUCCESS"` or `"FAILURE"` |
| `statusMessage` | String | Human-readable result message |

**Success:**
```json
{
  "status": "SUCCESS",
  "statusMessage": "Shipment status updated successfully"
}
```

**Failure (missing AWB):**
```json
{
  "status": "FAILURE",
  "statusMessage": "AWB is required"
}
```

**Failure (unknown AWB):**
```json
{
  "status": "FAILURE",
  "statusMessage": "No shipment found for AWB: 14492489597159"
}
```

---

## Common Status Values

| Status String | Description |
|---|---|
| `Pickup Scheduled` | Pickup has been scheduled |
| `Picked Up` | Package collected from warehouse |
| `In Transit` | Package in transit between hubs |
| `Out for Delivery` | Package with last-mile delivery agent |
| `Delivered` | Successfully delivered to customer |
| `Delivery Failed` | Delivery attempt failed |
| `RTO Initiated` | Return-to-origin initiated |
| `RTO Delivered` | Package returned to origin warehouse |
| `Cancelled` | Shipment cancelled |

---

## API Summary

| Method | Endpoint | Description | Caller |
|---|---|---|---|
| `POST` | `/api/shipping/create-order` | Create Shiprocket order on checkout | Frontend / Order Service |
| `GET` | `/api/shipping/serviceability` | Quick pincode serviceability check | Frontend |
| `POST` | `/api/shipping/serviceabilityWithAllParams` | Full serviceability + rate check with all params | Frontend |
| `POST` | `/api/shipping/serviceability/by-variants` | Serviceability check by product variant IDs (warehouse-aware, default warehouse fallback) | Frontend |
| `GET` | `/api/shipping/available-courier-services/{orderId}` | List available courier services for an order (pickup/delivery/weight resolved internally), excluding blocklisted couriers | Frontend / Admin |
| `GET` | `/api/shipping/track/{awb}` | Raw live tracking from Shiprocket | Frontend / Internal |
| `GET` | `/api/shipping/track-shipment/{awbCode}` | Combined DB + live tracking view | Frontend |
| `POST` | `/api/shipping/webhook` | Receive status updates from Shiprocket | Shiprocket (external) |

---

## Changelog

- **2026-09-08** — Changed `GET /api/shipping/available-courier-services/{orderId}` from a request-body-based POST to an order-id-based GET. Pickup/delivery postcode and shipment weight/dimensions are now resolved internally from the order's existing shipment and shipping address (mirroring the internal serviceability lookup used during automated Shiprocket processing), and the response now includes `currentlyUsedCourierId` — the courier currently assigned to the order's shipment, if any. See [Section 5](#5-get-available-courier-services-excluding-blocklisted).
- **2026-09-08** — Added `POST /api/shipping/available-courier-services` — lists all Shiprocket-serviceable couriers for a given pickup/delivery pair while excluding any courier present in the internal blocklist (`Constants.BLOCKLISTED_COURIER_COMPANY_IDS`). See [Section 5](#5-get-available-courier-services-excluding-blocklisted).


