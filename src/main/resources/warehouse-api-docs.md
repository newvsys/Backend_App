# Warehouse API Documentation

**Base URL:** `http://localhost:8080/api`  
**Content-Type:** `application/json`

---

## Endpoints Summary

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | `POST` | `/api/Create-Warehouse` | Create a new warehouse |
| 2 | `PUT` | `/api/Update-Warehouse/{warehouseId}` | Update an existing warehouse |
| 3 | `DELETE` | `/api/Delete-Warehouse/{warehouseId}` | Soft-delete a warehouse |
| 4 | `GET` | `/api/Get-Warehouse/{warehouseId}` | Get warehouse by ID |
| 5 | `GET` | `/api/Get-All-Warehouses` | Get all active warehouses |

---

## 1. Create Warehouse

**Endpoint:** `POST /api/Create-Warehouse`  
**Description:** Creates a new warehouse. Status is automatically set to `A` (Active).

### Request Body

| Field | Type | Required | Description |
|---|---|---|---|
| `warehouseName` | `String` | ✅ Yes | Display name of the warehouse |
| `warehouseCode` | `String` | ✅ Yes | Unique identifier code (e.g. `"WH-CHN-001"`) |
| `channelId` | `String` | ❌ No | Shiprocket Sales Channel ID linked to this warehouse |
| `contactPerson` | `String` | ❌ No | Name of the contact person |
| `contactNumber` | `String` | ❌ No | Contact phone number |
| `email` | `String` | ❌ No | Contact email address |
| `addressLine1` | `String` | ❌ No | Primary address line |
| `addressLine2` | `String` | ❌ No | Secondary address line |
| `city` | `String` | ❌ No | City |
| `state` | `String` | ❌ No | State |
| `postalCode` | `String` | ❌ No | PIN / postal code |
| `country` | `String` | ❌ No | Country |
| `latitude` | `Double` | ❌ No | Latitude coordinate |
| `longitude` | `Double` | ❌ No | Longitude coordinate |

### Request Example

```json
{
  "warehouseName": "Chennai Central Warehouse",
  "warehouseCode": "WH-CHN-001",
  "channelId": "10576563",
  "contactPerson": "Ramesh Kumar",
  "contactNumber": "9876543210",
  "email": "ramesh.kumar@example.com",
  "addressLine1": "No. 45, Industrial Estate",
  "addressLine2": "Ambattur",
  "city": "Chennai",
  "state": "Tamil Nadu",
  "postalCode": "600058",
  "country": "India",
  "latitude": 13.0827,
  "longitude": 80.2707
}
```

### Response — Success `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Warehouse created successfully"
}
```

### Response — Failure `500`

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Failed to create warehouse: could not execute statement..."
}
```

---

## 2. Update Warehouse

**Endpoint:** `PUT /api/Update-Warehouse/{warehouseId}`  
**Description:** Partially updates an existing active warehouse. Only provided (non-null) fields are updated. `warehouseCode` cannot be changed via update.

### Path Parameter

| Parameter | Type | Required | Description |
|---|---|---|---|
| `warehouseId` | `Long` | ✅ Yes | ID of the warehouse to update |

### Request Body (all fields optional)

| Field | Type | Description |
|---|---|---|
| `warehouseName` | `String` | New display name |
| `channelId` | `String` | Shiprocket Sales Channel ID linked to this warehouse |
| `contactPerson` | `String` | Contact person name |
| `contactNumber` | `String` | Contact phone number |
| `email` | `String` | Contact email |
| `addressLine1` | `String` | Primary address line |
| `addressLine2` | `String` | Secondary address line |
| `city` | `String` | City |
| `state` | `String` | State |
| `postalCode` | `String` | PIN / postal code |
| `country` | `String` | Country |
| `latitude` | `Double` | Latitude coordinate |
| `longitude` | `Double` | Longitude coordinate |
| `status` | `String` | `"A"` (Active) or `"I"` (Inactive) |

### Request Example

```json
{
  "warehouseName": "Chennai South Warehouse",
  "channelId": "10576563",
  "contactPerson": "Suresh Babu",
  "contactNumber": "9123456780",
  "email": "suresh.babu@example.com",
  "addressLine1": "No. 12, SIDCO Industrial Area",
  "addressLine2": "Guindy",
  "city": "Chennai",
  "state": "Tamil Nadu",
  "postalCode": "600032",
  "country": "India",
  "latitude": 13.0067,
  "longitude": 80.2206,
  "status": "A"
}
```

### Response — Success `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Warehouse updated successfully"
}
```

### Response — Not Found `200 OK`

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Warehouse not found or inactive"
}
```

### Response — Failure `500`

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Failed to update warehouse: ..."
}
```

---

## 3. Delete Warehouse (Soft Delete)

**Endpoint:** `DELETE /api/Delete-Warehouse/{warehouseId}`  
**Description:** Soft-deletes a warehouse by setting its status to `I` (Inactive). The record is **not** physically removed.

### Path Parameter

| Parameter | Type | Required | Description |
|---|---|---|---|
| `warehouseId` | `Long` | ✅ Yes | ID of the warehouse to delete |

### Response — Success `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Warehouse deleted successfully"
}
```

### Response — Not Found / Already Deleted `200 OK`

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Warehouse not found or already inactive"
}
```

### Response — Failure `500`

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Failed to delete warehouse: ..."
}
```

---

## 4. Get Warehouse by ID

**Endpoint:** `GET /api/Get-Warehouse/{warehouseId}`  
**Description:** Fetches full details of a single active warehouse by its ID.

### Path Parameter

| Parameter | Type | Required | Description |
|---|---|---|---|
| `warehouseId` | `Long` | ✅ Yes | ID of the warehouse |

### Response — Success `200 OK`

```json
{
  "warehouseId": 1,
  "warehouseName": "Chennai Central Warehouse",
  "warehouseCode": "WH-CHN-001",
  "channelId": "10576563",
  "contactPerson": "Ramesh Kumar",
  "contactNumber": "9876543210",
  "email": "ramesh.kumar@example.com",
  "addressLine1": "No. 45, Industrial Estate",
  "addressLine2": "Ambattur",
  "city": "Chennai",
  "state": "Tamil Nadu",
  "postalCode": "600058",
  "country": "India",
  "latitude": 13.0827,
  "longitude": 80.2707,
  "status": "A",
  "createdAt": "2026-05-15T10:30:00",
  "updatedAt": "2026-05-15T10:30:00"
}
```

### Response Field Reference

| Field | Type | Description |
|---|---|---|
| `warehouseId` | `Long` | Unique warehouse ID |
| `warehouseName` | `String` | Warehouse display name (used as `pickup_location` in Shiprocket orders) |
| `warehouseCode` | `String` | Unique warehouse code |
| `channelId` | `String` | Shiprocket Sales Channel ID — used as `channel_id` in order creation |
| `contactPerson` | `String` | Contact person name |
| `contactNumber` | `String` | Contact phone number |
| `email` | `String` | Contact email |
| `addressLine1` | `String` | Primary address line |
| `addressLine2` | `String` | Secondary address line |
| `city` | `String` | City |
| `state` | `String` | State |
| `postalCode` | `String` | PIN / postal code — used as pickup postcode in serviceability checks |
| `country` | `String` | Country |
| `latitude` | `Double` | Latitude |
| `longitude` | `Double` | Longitude |
| `status` | `String` | `"A"` = Active, `"I"` = Inactive |
| `createdAt` | `LocalDateTime` | Record creation timestamp |
| `updatedAt` | `LocalDateTime` | Last update timestamp |

### Response — Not Found `404`

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Warehouse not found or inactive"
}
```

### Response — Failure `500`

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Failed to fetch warehouse"
}
```

---

## 5. Get All Warehouses

**Endpoint:** `GET /api/Get-All-Warehouses`  
**Description:** Returns all active warehouses (status = `A`). Soft-deleted warehouses are excluded.

### Response — Success `200 OK`

```json
[
  {
    "warehouseId": 1,
    "warehouseName": "Chennai Central Warehouse",
    "warehouseCode": "WH-CHN-001",
    "channelId": "10576563",
    "contactPerson": "Ramesh Kumar",
    "contactNumber": "9876543210",
    "email": "ramesh.kumar@example.com",
    "addressLine1": "No. 45, Industrial Estate",
    "addressLine2": "Ambattur",
    "city": "Chennai",
    "state": "Tamil Nadu",
    "postalCode": "600058",
    "country": "India",
    "latitude": 13.0827,
    "longitude": 80.2707,
    "status": "A",
    "createdAt": "2026-05-15T10:30:00",
    "updatedAt": "2026-05-15T10:30:00"
  },
  {
    "warehouseId": 2,
    "warehouseName": "Coimbatore North Warehouse",
    "warehouseCode": "WH-CBE-001",
    "channelId": "10576564",
    "contactPerson": "Anand Raj",
    "contactNumber": "9988776655",
    "email": "anand.raj@example.com",
    "addressLine1": "Plot 7, SIPCOT Industrial Park",
    "addressLine2": "Kurichi",
    "city": "Coimbatore",
    "state": "Tamil Nadu",
    "postalCode": "641021",
    "country": "India",
    "latitude": 10.9916,
    "longitude": 76.9656,
    "status": "A",
    "createdAt": "2026-05-15T11:00:00",
    "updatedAt": "2026-05-15T11:00:00"
  }
]
```

### Response — Empty List `200 OK`

```json
[]
```

### Response — Failure `500`

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Failed to fetch warehouses"
}
```

---

## `channelId` — Shiprocket Integration Note

The `channelId` field links this warehouse to a specific **Shiprocket Sales Channel**.

| Where it's used | How |
|---|---|
| `POST /api/Create-Warehouse` | Store the Shiprocket channel ID for this warehouse |
| `PUT /api/Update-Warehouse/{id}` | Update the channel ID if it changes |
| Shiprocket order creation | Automatically passed as `channel_id` in every order created for shipments from this warehouse |
| Shiprocket serviceability check | Warehouse `postalCode` is used as `pickup_postcode` |

> To find your channel ID: log in to Shiprocket → **Settings → Channels** → copy the numeric ID next to your store.

---

## Status Reference

| Value | Meaning |
|---|---|
| `A` | Active |
| `I` | Inactive (soft deleted) |

## Response Status Reference

| `responseStatus` | Meaning |
|---|---|
| `SUCCESS` | Operation succeeded |
| `FAILURE` | Operation failed |
