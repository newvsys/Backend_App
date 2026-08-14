# Inventory API Documentation

**Base URL:** `http://localhost:8080`  
**Controller base path:** `/api/inventory`  
**Content-Type:** `application/json`

> Full URL pattern: `http://localhost:8080/api/inventory/...`

---

## Endpoints Summary

| # | Method | Endpoint                                | Description                                        |
|---|--------|-----------------------------------------|----------------------------------------------------|
| 1 | `POST` | `/api/inventory/load`                   | Load (add) stock for a product variant             |
| 2 | `POST` | `/api/inventory/restore`                | Restore a removed/sold item back to AVAILABLE      |
| 3 | `POST` | `/api/inventory/remove`                 | Remove a single item (mark Inactive by barcode)    |
| 4 | `GET`  | `/api/inventory/variant/{variant_id}`   | Get active inventory summary by product variant ID |
| 5 | `GET`  | `/api/inventory/details`                | Fetch inventory + per-unit item details (filterable)|
| 6 | `PUT`  | `/api/inventory/details/dates`          | Update MFG / EXP / best-before dates on detail rows|
| 7 | `POST` | `/api/inventory/refresh-counts`         | Refresh totalQty & availableQty from inventory_details |
| 8 | `GET`  | `/api/inventory/stock-report`           | Generate the current stock report (category/product/variant/warehouse/inventory combined) |
| 9 | `GET`  | `/api/inventory/out-of-stock`           | Generate the out-of-stock report (products/variants with no available stock in any warehouse) |

---

## 1. Load Inventory

### `POST /api/inventory/load`

Loads (adds) stock for a product variant into a warehouse.

**What it does:**
- Creates or updates the `InventoryEO` record (increments `totalQty` and `availableQty`).
- Generates a unique batch number for this load event.
- Inserts one `InventoryTransactionEO` record with `transactionType = LOAD_INVENTORY`.
- Generates a unique barcode for **every unit** and inserts one `InventoryDetailsEO` row per unit.

### Request Body

| Field        | Type     | Required | Description                                       |
|--------------|----------|----------|---------------------------------------------------|
| `productVarId` | `Long` | ✅ Yes  | Product variant ID to load stock for              |
| `qty`        | `Integer`| ✅ Yes  | Number of units to add (must be > 0)              |
| `whid`       | `String` | ✅ Yes  | Warehouse code (e.g. `"WH001"`)                   |
| `mfd`        | `String` | ❌ No   | Manufactured date — ISO format `"YYYY-MM-DD"`     |
| `bestBefore` | `String` | ❌ No   | Best before date — ISO format `"YYYY-MM-DD"`      |
| `expiryDate` | `String` | ❌ No   | Expiry date — ISO format `"YYYY-MM-DD"`           |

### Request Example

```json
{
  "productVarId": 101,
  "qty": 50,
  "whid": "WH001",
  "mfd": "2026-01-10",
  "bestBefore": "2027-01-10",
  "expiryDate": "2027-06-10"
}
```

### Response — `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Inventory loaded successfully.",
  "batchNo": "BATCH-101-1718099234567",
  "batchId": 1718099234567,
  "inventoryId": 5,
  "quantityLoaded": 50,
  "totalAvailableQty": 50,
  "barcodes": [
    "BC-101-1718099234567-1",
    "BC-101-1718099234567-2",
    "...",
    "BC-101-1718099234567-50"
  ]
}
```

### Response Field Reference

| Field             | Type      | Description                                          |
|-------------------|-----------|------------------------------------------------------|
| `responseStatus`  | `String`  | `SUCCESS` or `FAILURE`                               |
| `responseMessage` | `String`  | Human-readable result message                        |
| `batchNo`         | `String`  | Generated batch number (`BATCH-{variantId}-{ts}`)    |
| `batchId`         | `Long`    | Timestamp-based numeric batch ID                     |
| `inventoryId`     | `Long`    | ID of the `InventoryEO` record created/updated       |
| `quantityLoaded`  | `Integer` | Number of units loaded (mirrors request `qty`)       |
| `totalAvailableQty` | `Integer` | New total `availableQty` after loading             |
| `barcodes`        | `String[]`| One unique barcode per unit (`BC-{vid}-{ts}-{seq}`)  |

### Error Responses

| HTTP Status | Scenario                                          |
|-------------|---------------------------------------------------|
| `400`       | `productVarId`, `qty` or `whid` missing           |
| `400`       | `qty` is 0 or negative                            |
| `400`       | Product variant not found or inactive             |
| `400`       | Warehouse not found or inactive                   |
| `500`       | Unexpected server error                           |

---

## 2. Restore Inventory

### `POST /api/inventory/restore`

Restores a single inventory unit (identified by its barcode) back to `AVAILABLE` status.

**What it does:**
- Sets `InventoryDetailsEO.status` → `AVAILABLE`.
- Increments `InventoryEO.availableQty` by 1.
- Inserts one `InventoryTransactionEO` record with `transactionType = RESTORE_INVENTORY`.

### Request Body

| Field     | Type     | Required | Description                        |
|-----------|----------|----------|------------------------------------|
| `barcode` | `String` | ✅ Yes  | Barcode of the item to restore     |

### Request Example

```json
{
  "barcode": "BC-101-1718099234567-5"
}
```

### Response — `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Inventory item restored successfully.",
  "barcode": "BC-101-1718099234567-5",
  "previousStatus": "SOLD",
  "inventoryId": 5,
  "availableQtyBefore": 20,
  "availableQtyAfter": 21,
  "transactionId": 42
}
```

### Response Field Reference

| Field              | Type      | Description                                        |
|--------------------|-----------|----------------------------------------------------|
| `responseStatus`   | `String`  | `SUCCESS` or `FAILURE`                             |
| `responseMessage`  | `String`  | Human-readable result message                      |
| `barcode`          | `String`  | The barcode that was restored                      |
| `previousStatus`   | `String`  | Status before restore (e.g. `SOLD`, `DAMAGED`)     |
| `inventoryId`      | `Long`    | Parent inventory record ID                         |
| `availableQtyBefore` | `Integer` | `availableQty` before this operation             |
| `availableQtyAfter`  | `Integer` | `availableQty` after this operation              |
| `transactionId`    | `Long`    | ID of the created `InventoryTransactionEO` record  |

### Error Responses

| HTTP Status | Scenario                                          |
|-------------|---------------------------------------------------|
| `400`       | `barcode` is blank or missing                     |
| `400`       | No inventory item found for the given barcode     |
| `400`       | Item is already `AVAILABLE` — no change made      |
| `500`       | Unexpected server error                           |

---

## 3. Remove Inventory

### `POST /api/inventory/remove`

Removes a single inventory unit (identified by its barcode) by marking it as Inactive (`I`).

**What it does:**
- Sets `InventoryDetailsEO.status` → `I` (Inactive).
- Decrements `InventoryEO.availableQty` by 1 (floor of 0 — never goes negative).
- Inserts one `InventoryTransactionEO` record with `transactionType = REMOVE_INVENTORY`.

### Request Body

| Field     | Type     | Required | Description                       |
|-----------|----------|----------|-----------------------------------|
| `barcode` | `String` | ✅ Yes  | Barcode of the item to remove     |

### Request Example

```json
{
  "barcode": "BC-101-1718099234567-5"
}
```

### Response — `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Inventory item removed successfully.",
  "barcode": "BC-101-1718099234567-5",
  "previousStatus": "AVAILABLE",
  "inventoryId": 5,
  "availableQtyBefore": 21,
  "availableQtyAfter": 20,
  "transactionId": 43
}
```

### Response Field Reference

| Field              | Type      | Description                                        |
|--------------------|-----------|----------------------------------------------------|
| `responseStatus`   | `String`  | `SUCCESS` or `FAILURE`                             |
| `responseMessage`  | `String`  | Human-readable result message                      |
| `barcode`          | `String`  | The barcode that was removed                       |
| `previousStatus`   | `String`  | Status before removal (e.g. `AVAILABLE`)           |
| `inventoryId`      | `Long`    | Parent inventory record ID                         |
| `availableQtyBefore` | `Integer` | `availableQty` before this operation             |
| `availableQtyAfter`  | `Integer` | `availableQty` after this operation              |
| `transactionId`    | `Long`    | ID of the created `InventoryTransactionEO` record  |

### Error Responses

| HTTP Status | Scenario                                          |
|-------------|---------------------------------------------------|
| `400`       | `barcode` is blank or missing                     |
| `400`       | No inventory item found for the given barcode     |
| `400`       | Item is already Inactive (`I`) — no change made   |
| `500`       | Unexpected server error                           |

---

## 4. Get Active Inventory by Variant ID

### `GET /api/inventory/variant/{variant_id}`

Returns the active inventory summary record for a given product variant.

### Path Parameter

| Parameter    | Type   | Required | Description           |
|--------------|--------|----------|-----------------------|
| `variant_id` | `Long` | ✅ Yes  | Product variant ID    |

### Request Example

```
GET /api/inventory/variant/101
```

### Response — `200 OK`

```json
{
  "inventoryId": 5,
  "productVarId": 101,
  "totalQty": 50,
  "availableQty": 48,
  "whid": "WH001"
}
```

### Response Field Reference

| Field          | Type      | Description                                  |
|----------------|-----------|----------------------------------------------|
| `inventoryId`  | `Integer` | Inventory record ID                          |
| `productVarId` | `Integer` | Product variant ID                           |
| `totalQty`     | `Integer` | Total quantity ever loaded                   |
| `availableQty` | `Integer` | Current available (not removed/sold) qty     |
| `whid`         | `String`  | Warehouse code                               |

### Error Responses

| HTTP Status | Scenario                                               |
|-------------|--------------------------------------------------------|
| `404`       | No active inventory found for the given `variant_id`   |
| `500`       | Unexpected server error                                |

---

## 5. Fetch Inventory Details

### `GET /api/inventory/details`

Fetches inventory records with full per-unit item details. Supports flexible filtering.  
**At least one query parameter must be provided.**

### Query Parameters

| Parameter      | Type     | Required | Description                                                   |
|----------------|----------|----------|---------------------------------------------------------------|
| `productVarId` | `Long`   | ❌ No   | Filter by product variant ID                                  |
| `warehouseId`  | `Long`   | ❌ No   | Filter by warehouse ID                                        |
| `barcode`      | `String` | ❌ No   | Fetch the single item matching this exact barcode             |

> When `barcode` is supplied it takes priority — returns the one inventory record that owns that item.  
> When `productVarId` + `warehouseId` are both supplied, the result is narrowed to the single record matching both.

### Request Examples

```
GET /api/inventory/details?productVarId=101
GET /api/inventory/details?warehouseId=2
GET /api/inventory/details?productVarId=101&warehouseId=2
GET /api/inventory/details?barcode=BC-101-1718099234567-5
```

### Response — `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Inventory fetched successfully.",
  "totalRecords": 1,
  "inventories": [
    {
      "inventoryId": 5,
      "productVarId": 101,
      "productVariantName": "SKU-CANDY-250G",
      "warehouseId": 2,
      "warehouseCode": "WH001",
      "warehouseName": "Main Warehouse",
      "totalQty": 50,
      "availableQty": 48,
      "reservedQty": 0,
      "quantityReserved": 0,
      "reorderLevel": 10,
      "safetyStock": 5,
      "status": "A",
      "items": [
        {
          "id": 1,
          "barcode": "BC-101-1718099234567-1",
          "batchNo": "BATCH-101-1718099234567",
          "status": "AVAILABLE",
          "mfd": "2026-01-10",
          "bestBefore": "2027-01-10",
          "expiryDate": "2027-06-10",
          "createdAt": "2026-06-11T10:00:00+05:30",
          "updatedAt": "2026-06-11T10:00:00+05:30"
        },
        {
          "id": 2,
          "barcode": "BC-101-1718099234567-2",
          "batchNo": "BATCH-101-1718099234567",
          "status": "I",
          "mfd": "2026-01-10",
          "bestBefore": "2027-01-10",
          "expiryDate": "2027-06-10",
          "createdAt": "2026-06-11T10:00:00+05:30",
          "updatedAt": "2026-06-11T11:30:00+05:30"
        }
      ]
    }
  ]
}
```

### Response Field Reference — Top Level

| Field             | Type      | Description                                        |
|-------------------|-----------|----------------------------------------------------|
| `responseStatus`  | `String`  | `SUCCESS` or `FAILURE`                             |
| `responseMessage` | `String`  | Human-readable result message                      |
| `totalRecords`    | `Integer` | Number of inventory records returned               |
| `inventories`     | `Array`   | List of `InventoryInfoDTO` objects                 |

### Response Field Reference — `inventories[]`

| Field               | Type      | Description                                      |
|---------------------|-----------|--------------------------------------------------|
| `inventoryId`       | `Long`    | Inventory record ID                              |
| `productVarId`      | `Long`    | Product variant ID                               |
| `productVariantName`| `String`  | Product variant SKU code                         |
| `warehouseId`       | `Long`    | Warehouse ID                                     |
| `warehouseCode`     | `String`  | Warehouse code (e.g. `"WH001"`)                  |
| `warehouseName`     | `String`  | Warehouse display name                           |
| `totalQty`          | `Integer` | Total quantity ever loaded                       |
| `availableQty`      | `Integer` | Currently available qty                          |
| `reservedQty`       | `Integer` | Legacy reserved qty                              |
| `quantityReserved`  | `Integer` | Qty reserved for pending orders                  |
| `reorderLevel`      | `Integer` | Threshold to trigger reorder                     |
| `safetyStock`       | `Integer` | Minimum buffer stock                             |
| `status`            | `String`  | Inventory record status (`A` = Active)           |
| `items`             | `Array`   | Per-unit item details — see below                |

### Response Field Reference — `items[]`

| Field        | Type     | Description                                                   |
|--------------|----------|---------------------------------------------------------------|
| `id`         | `Long`   | Item record ID                                                |
| `barcode`    | `String` | Unique barcode for this physical unit                         |
| `batchNo`    | `String` | Batch number shared by all units in the same load             |
| `status`     | `String` | `AVAILABLE` / `RESERVED` / `SOLD` / `DAMAGED` / `EXPIRED` / `I` |
| `mfd`        | `String` | Manufactured date (`YYYY-MM-DD`), or `null`                   |
| `bestBefore` | `String` | Best before date (`YYYY-MM-DD`), or `null`                    |
| `expiryDate` | `String` | Expiry date (`YYYY-MM-DD`), or `null`                         |
| `createdAt`  | `String` | ISO-8601 timestamp when the item was first loaded             |
| `updatedAt`  | `String` | ISO-8601 timestamp of the last status change                  |

### Error Responses

| HTTP Status | Scenario                                                     |
|-------------|--------------------------------------------------------------|
| `400`       | No filter parameter provided                                 |
| `400`       | `barcode` provided but no matching item found                |
| `200`       | Filters valid but no matching inventory records (`totalRecords: 0`) |
| `500`       | Unexpected server error                                      |

---

## 6. Update Inventory Dates

### `PUT /api/inventory/details/dates`

Updates the MFG date, expiry date, and/or best-before date on existing `inventory_details` rows.  
Use this to fix records that were loaded without dates (they would otherwise show as `"N/A"` on printed labels).

**Scope:** Supply **either** `batchNo` (updates every unit in that batch) **or** `barcode` (updates a single unit).  
`batchNo` takes priority if both are provided. At least one date field must be present.

### Request Body

| Field        | Type     | Required | Description                                                        |
|--------------|----------|----------|--------------------------------------------------------------------|
| `batchNo`    | `String` | ❌ No*  | Batch number — all units in this batch are updated                 |
| `barcode`    | `String` | ❌ No*  | Barcode of the single unit to update                               |
| `mfd`        | `String` | ❌ No** | Manufactured date — ISO format `"YYYY-MM-DD"`                      |
| `bestBefore` | `String` | ❌ No** | Best before date — ISO format `"YYYY-MM-DD"`                       |
| `expiryDate` | `String` | ❌ No** | Expiry date — ISO format `"YYYY-MM-DD"`                            |

> \* At least one of `batchNo` or `barcode` is required.  
> \*\* At least one date field (`mfd`, `bestBefore`, or `expiryDate`) is required.

### Request Examples

**Update all units in a batch:**
```json
{
  "batchNo": "BATCH-101-1718099234567",
  "mfd": "2026-01-10",
  "bestBefore": "2027-01-10",
  "expiryDate": "2027-06-10"
}
```

**Update a single unit by barcode:**
```json
{
  "barcode": "BC-101-1718099234567-5",
  "expiryDate": "2027-09-01"
}
```

### Response — `200 OK`

```json
{
  "status": "SUCCESS",
  "message": "Inventory dates updated successfully.",
  "updatedCount": 50
}
```

### Response Field Reference

| Field          | Type      | Description                                    |
|----------------|-----------|------------------------------------------------|
| `status`       | `String`  | `SUCCESS` or `FAILURE`                         |
| `message`      | `String`  | Human-readable result message                  |
| `updatedCount` | `Integer` | Number of `inventory_details` rows updated     |

### Error Responses

| HTTP Status | Scenario                                                      |
|-------------|---------------------------------------------------------------|
| `400`       | Neither `batchNo` nor `barcode` provided                      |
| `400`       | No date fields provided                                       |
| `400`       | `batchNo` provided but no matching items found                |
| `400`       | `barcode` provided but no matching item found                 |
| `500`       | Unexpected server error                                       |

---

## 7. Refresh Inventory Counts

### `POST /api/inventory/refresh-counts`

Resets `totalQty` and `availableQty` in the `inventory` table to match the **actual stock** available in the `inventory_details` table.  
For every inventory record refreshed, one `InventoryTransactionEO` of type `STOCK_REFRESH` is recorded in the transaction log.

**What it does:**
- `totalQty` ← count of **ALL** rows in `inventory_details` for the inventory record (any status).
- `availableQty` ← count of rows in `inventory_details` where `status = 'A'` (available).
- Writes one `STOCK_REFRESH` transaction per inventory record showing the before/after values and the net delta.

> Use this API whenever the `inventory` header quantities have drifted out of sync with the actual `inventory_details` rows (e.g. after a data fix, bulk import, or manual DB edit).

### Request Body

All fields are **optional**. Supply one filter to limit scope, or send an empty body `{}` to refresh every inventory record in the system.

| Field         | Type     | Required | Description                                                              |
|---------------|----------|----------|--------------------------------------------------------------------------|
| `inventoryId` | `Long`   | ❌ No   | Refresh only this specific inventory record (most precise)               |
| `productVarId`| `Long`   | ❌ No   | Refresh all inventory records for this product variant                   |
| `warehouseId` | `Long`   | ❌ No   | Refresh all inventory records for this warehouse                         |
| `refreshedBy` | `String` | ❌ No   | User/system identifier written to audit trail. Defaults to `"SYSTEM"`   |

> Filter priority (most-to-least specific): `inventoryId` → `productVarId` → `warehouseId` → all records.

### Request Examples

**Refresh a single inventory record:**
```json
{
  "inventoryId": 5,
  "refreshedBy": "admin"
}
```

**Refresh all records for a product variant:**
```json
{
  "productVarId": 101
}
```

**Refresh all records for a warehouse:**
```json
{
  "warehouseId": 2,
  "refreshedBy": "scheduler"
}
```

**Refresh every inventory record in the system:**
```json
{}
```

### Response — `200 OK`

```json
{
  "status": "SUCCESS",
  "message": "Inventory counts refreshed successfully for 1 record(s).",
  "refreshedCount": 1,
  "details": [
    {
      "inventoryId": 5,
      "productVariantId": 101,
      "productVariantName": "SKU-CANDY-250G",
      "warehouseId": 2,
      "warehouseName": "Main Warehouse",
      "previousAvailableQty": 48,
      "previousTotalQty": 50,
      "newAvailableQty": 45,
      "newTotalQty": 50,
      "availableQtyDelta": -3,
      "transactionId": 1024
    }
  ]
}
```

### Response Field Reference — Top Level

| Field            | Type      | Description                                                |
|------------------|-----------|------------------------------------------------------------|
| `status`         | `String`  | `SUCCESS` or `FAILURE`                                     |
| `message`        | `String`  | Human-readable result message                              |
| `refreshedCount` | `Integer` | Number of inventory records successfully refreshed         |
| `details`        | `Array`   | Per-record refresh results — see below                     |

### Response Field Reference — `details[]`

| Field                  | Type      | Description                                                         |
|------------------------|-----------|---------------------------------------------------------------------|
| `inventoryId`          | `Long`    | Inventory record ID                                                 |
| `productVariantId`     | `Long`    | Product variant ID                                                  |
| `productVariantName`   | `String`  | Product variant SKU code                                            |
| `warehouseId`          | `Long`    | Warehouse ID                                                        |
| `warehouseName`        | `String`  | Warehouse display name                                              |
| `previousAvailableQty` | `Integer` | `availableQty` value before the refresh                             |
| `previousTotalQty`     | `Integer` | `totalQty` value before the refresh                                 |
| `newAvailableQty`      | `Integer` | Recalculated `availableQty` (count of `status='A'` detail rows)     |
| `newTotalQty`          | `Integer` | Recalculated `totalQty` (count of all detail rows)                  |
| `availableQtyDelta`    | `Integer` | Net change = `newAvailableQty − previousAvailableQty` (can be negative) |
| `transactionId`        | `Long`    | ID of the `STOCK_REFRESH` transaction created for this record       |

### Error Responses

| HTTP Status | Scenario                                                            |
|-------------|---------------------------------------------------------------------|
| `400`       | `inventoryId` provided but no matching record found                 |
| `400`       | `productVarId` provided but no inventory records found for variant  |
| `400`       | `warehouseId` provided but no inventory records found for warehouse |
| `500`       | Unexpected server error                                             |

---

## 8. Current Stock Report

### `GET /api/inventory/stock-report`

Generates/views the current stock report — combines category, product, product variant, warehouse, and inventory (stock) details in a single response. All query parameters are optional and can be combined to narrow the report.

> **No-inventory rows and `availableQty=0`:** A product variant that has **no `InventoryEO` record at all** is treated as having `availableQty = 0`. By default (no `availableQty` filter), the report only returns variants that actually have an inventory record — same as before. But when your `availableQty` filter expression would match `0` (e.g. `availableQty=0`, `availableQty=<5`, `availableQty=<=0`), the report **also** includes variants with no inventory record at all (their `inventoryId`, `warehouseId`, `totalQty`, `reservedQty`, etc. are returned as `null`, `availableQty` is `0`, and `stockStatus` is `"OUT_OF_STOCK"`).

### Query Parameters

| Parameter      | Type      | Required | Description                                                          |
|----------------|-----------|----------|-----------------------------------------------------------------------|
| `warehouseId`  | `Long`    | ❌ No   | Filter by warehouse ID                                                |
| `categoryId`   | `Integer` | ❌ No   | Filter by product category ID                                        |
| `productId`    | `Integer` | ❌ No   | Filter by product ID                                                 |
| `productVarId` | `Integer` | ❌ No   | Filter by product variant ID                                         |
| `status`       | `String`  | ❌ No   | Filter by inventory status. Defaults to `"A"` (active records only). Ignored for variants with no inventory record. |
| `availableQty` | `String`  | ❌ No   | Filter by available quantity. Accepts a **plain number** for an exact match (e.g. `10`), or a **comparison expression** prefixed with `>`, `<`, `>=`, or `<=` (e.g. `>10`, `<5`, `>=10`, `<=5`). A missing inventory record is treated as `availableQty = 0` when evaluating the expression. |

> **Comparison operator syntax:** Since `>` and `<` are special characters in a URL, either URL-encode them (`%3E` for `>`, `%3C` for `<`) or pass them as-is — most HTTP clients (Postman, curl, browsers) handle it fine unqualified in the query string. Examples: `availableQty=>10`, `availableQty=%3E10`, `availableQty=<=5`.

### Request Examples

```
GET /api/inventory/stock-report
GET /api/inventory/stock-report?warehouseId=2
GET /api/inventory/stock-report?categoryId=3&productId=101
GET /api/inventory/stock-report?productVarId=101&status=A
GET /api/inventory/stock-report?categoryId=4&productId=41&productVarId=140&status=A&availableQty=10
GET /api/inventory/stock-report?availableQty=0
GET /api/inventory/stock-report?availableQty=>10
GET /api/inventory/stock-report?availableQty=<5
GET /api/inventory/stock-report?availableQty=>=10
GET /api/inventory/stock-report?availableQty=<=5
```

### Response — `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Stock report generated successfully.",
  "totalRecords": 1,
  "totalAvailableQty": 48,
  "totalStockQty": 50,
  "reportItems": [
    {
      "categoryId": 3,
      "categoryName": "Snacks",
      "productId": 101,
      "productName": "Candy Delight",
      "productSlug": "candy-delight",
      "productStatus": "A",
      "variantId": 101,
      "skuCode": "SKU-CANDY-250G",
      "packSize": "250g",
      "uom": "PACK",
      "mrp": 120.00,
      "sellingPrice": 99.00,
      "currency": "INR",
      "variantStatus": "A",
      "warehouseId": 2,
      "warehouseCode": "WH001",
      "warehouseName": "Main Warehouse",
      "warehouseCity": "Bengaluru",
      "inventoryId": 5,
      "totalQty": 50,
      "availableQty": 48,
      "reservedQty": 0,
      "quantityReserved": 0,
      "reorderLevel": 10,
      "safetyStock": 5,
      "inventoryStatus": "A",
      "stockStatus": "IN_STOCK"
    }
  ]
}
```

### Response — `200 OK` (`availableQty=0`, including a variant with NO inventory record)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Stock report generated successfully.",
  "totalRecords": 2,
  "totalAvailableQty": 0,
  "totalStockQty": 20,
  "reportItems": [
    {
      "categoryId": 3,
      "categoryName": "Snacks",
      "productId": 102,
      "productName": "Masala Peanuts",
      "productSlug": "masala-peanuts",
      "productStatus": "A",
      "variantId": 210,
      "skuCode": "MP-500G",
      "packSize": "500g",
      "uom": "PACK",
      "mrp": 150.00,
      "sellingPrice": 129.00,
      "currency": "INR",
      "variantStatus": "A",
      "warehouseId": 2,
      "warehouseCode": "WH001",
      "warehouseName": "Main Warehouse",
      "warehouseCity": "Bengaluru",
      "inventoryId": 8,
      "totalQty": 20,
      "availableQty": 0,
      "reservedQty": 0,
      "quantityReserved": 0,
      "reorderLevel": 5,
      "safetyStock": 2,
      "inventoryStatus": "A",
      "stockStatus": "OUT_OF_STOCK"
    },
    {
      "categoryId": 3,
      "categoryName": "Snacks",
      "productId": 103,
      "productName": "Banana Chips",
      "productSlug": "banana-chips",
      "productStatus": "A",
      "variantId": 305,
      "skuCode": "BC-250G",
      "packSize": "250g",
      "uom": "PACK",
      "mrp": 120.00,
      "sellingPrice": 99.00,
      "currency": "INR",
      "variantStatus": "A",
      "warehouseId": null,
      "warehouseCode": null,
      "warehouseName": null,
      "warehouseCity": null,
      "inventoryId": null,
      "totalQty": 0,
      "availableQty": 0,
      "reservedQty": null,
      "quantityReserved": null,
      "reorderLevel": null,
      "safetyStock": null,
      "inventoryStatus": null,
      "stockStatus": "OUT_OF_STOCK"
    }
  ]
}
```

> In the second example above, `variantId 305` has **no `InventoryEO` record in the database at all** — the API still returns it (because `availableQty=0` was requested) with all inventory/warehouse fields as `null` and `availableQty: 0`.



| Field               | Type      | Description                                                |
|---------------------|-----------|--------------------------------------------------------------|
| `responseStatus`    | `String`  | `SUCCESS` or `FAILURE`                                       |
| `responseMessage`   | `String`  | Human-readable result message                                |
| `totalRecords`      | `Integer` | Number of (product-variant, warehouse) rows returned          |
| `totalAvailableQty` | `Long`    | Sum of `availableQty` across all returned rows                |
| `totalStockQty`     | `Long`    | Sum of `totalQty` across all returned rows                    |
| `reportItems`       | `Array`   | List of `StockReportItemDTO` objects — see below              |

### Response Field Reference — `reportItems[]`

| Field              | Type         | Description                                                         |
|--------------------|--------------|-----------------------------------------------------------------------|
| `categoryId`       | `Integer`    | Product category ID                                                 |
| `categoryName`     | `String`     | Product category name                                                |
| `productId`        | `Integer`    | Product ID                                                            |
| `productName`      | `String`     | Product display name                                                  |
| `productSlug`      | `String`     | Product URL slug                                                      |
| `productStatus`    | `String`     | Product status (`A` = Active)                                        |
| `variantId`        | `Integer`    | Product variant ID                                                    |
| `skuCode`          | `String`     | Product variant SKU code                                             |
| `packSize`         | `String`     | Pack size (e.g. `"250g"`)                                             |
| `uom`              | `String`     | Unit of measure (e.g. `"PACK"`)                                       |
| `mrp`              | `BigDecimal` | Maximum retail price                                                  |
| `sellingPrice`     | `BigDecimal` | Current selling price                                                 |
| `currency`         | `String`     | Currency code (e.g. `"INR"`)                                          |
| `variantStatus`    | `String`     | Product variant status (`A` = Active)                                 |
| `warehouseId`      | `Long`       | Warehouse ID                                                          |
| `warehouseCode`    | `String`     | Warehouse code (e.g. `"WH001"`)                                       |
| `warehouseName`    | `String`     | Warehouse display name                                                |
| `warehouseCity`    | `String`     | City where the warehouse is located                                   |
| `inventoryId`      | `Integer`    | Inventory record ID                                                   |
| `totalQty`         | `Integer`    | Total quantity ever loaded                                            |
| `availableQty`     | `Integer`    | Currently available qty                                               |
| `reservedQty`      | `Integer`    | Legacy reserved qty                                                   |
| `quantityReserved` | `Integer`    | Qty reserved for pending orders                                       |
| `reorderLevel`     | `Integer`    | Threshold to trigger reorder                                          |
| `safetyStock`      | `Integer`    | Minimum buffer stock                                                  |
| `inventoryStatus`  | `String`     | Inventory record status (`A` = Active)                                |
| `stockStatus`      | `String`     | Derived: `OUT_OF_STOCK` / `LOW_STOCK` / `IN_STOCK` based on `availableQty` vs `reorderLevel` |

### Error Responses

| HTTP Status | Scenario                                                     |
|-------------|------------------------------------------------------------------|
| `200`       | Filters valid but no matching records (`totalRecords: 0`)       |
| `400`       | Invalid `availableQty` filter expression (e.g. `availableQty=abc` or `availableQty=>>10`) |
| `500`       | Unexpected server error                                         |



## 9. Out of Stock Report

### `GET /api/inventory/out-of-stock`

Generate/view the list of Out of Stock products and product variants. A product variant is
considered **out of stock** if it has **NO inventory record with `availableQty > 0` in ANY
warehouse** — i.e. either no `InventoryEO` record exists for it at all, or every existing
record across every warehouse is depleted (`availableQty <= 0`).
  
**What it does:**
- Looks up every **active** (`status = 'A'`) product variant matching the optional filters.
- Excludes any variant that has at least one inventory record with `availableQty > 0` in
  any warehouse.
- Batch-fetches the remaining inventory records (if any) for the out-of-stock variants to
  report `hasInventoryRecord` and the summed `totalAvailableQty`.

### Query Parameters (all optional)

| Parameter      | Type      | Description                                    |
|----------------|-----------|-------------------------------------------------|
| `categoryId`   | `Integer` | Filter by product category ID                  |
| `productId`    | `Integer` | Filter by product ID                           |
| `productVarId` | `Integer` | Filter by product variant ID                   |

### Request Examples

```
GET /api/inventory/out-of-stock
GET /api/inventory/out-of-stock?categoryId=3
GET /api/inventory/out-of-stock?productId=101
GET /api/inventory/out-of-stock?productVarId=101
```

### Response Example

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Out-of-stock report generated successfully.",
  "totalRecords": 1,
  "items": [
    {
      "categoryId": 3,
      "categoryName": "Snacks",
      "productId": 101,
      "productName": "Banana Chips",
      "productSlug": "banana-chips",
      "productStatus": "A",
      "variantId": 205,
      "skuCode": "BC-250G",
      "packSize": "250g",
      "uom": "PACK",
      "mrp": 120.00,
      "sellingPrice": 99.00,
      "currency": "INR",
      "variantStatus": "A",
      "hasInventoryRecord": true,
      "totalAvailableQty": 0,
      "stockStatus": "OUT_OF_STOCK"
    }
  ]
}
```

### Response Field Reference — Top Level

| Field              | Type      | Description                                             |
|--------------------|-----------|-------------------------------------------------------------|
| `responseStatus`   | `String`  | `SUCCESS` or `FAILURE`                                       |
| `responseMessage`  | `String`  | Human-readable result message                                |
| `totalRecords`     | `Integer` | Number of out-of-stock product variants returned              |
| `items`            | `Array`   | List of `OutOfStockItemDTO` objects — see below               |

### Response Field Reference — `items[]`

| Field                | Type         | Description                                                         |
|----------------------|--------------|-----------------------------------------------------------------------|
| `categoryId`         | `Integer`    | Product category ID                                                 |
| `categoryName`       | `String`     | Product category name                                                |
| `productId`          | `Integer`    | Product ID                                                            |
| `productName`        | `String`     | Product display name                                                  |
| `productSlug`        | `String`     | Product URL slug                                                      |
| `productStatus`      | `String`     | Product status (`A` = Active)                                        |
| `variantId`          | `Integer`    | Product variant ID                                                    |
| `skuCode`            | `String`     | Product variant SKU code                                             |
| `packSize`           | `String`     | Pack size (e.g. `"250g"`)                                             |
| `uom`                | `String`     | Unit of measure (e.g. `"PACK"`)                                       |
| `mrp`                | `BigDecimal` | Maximum retail price                                                  |
| `sellingPrice`       | `BigDecimal` | Current selling price                                                 |
| `currency`           | `String`     | Currency code (e.g. `"INR"`)                                          |
| `variantStatus`      | `String`     | Product variant status (`A` = Active)                                 |
| `hasInventoryRecord` | `Boolean`    | `true` if at least one `InventoryEO` record exists for this variant   |
| `totalAvailableQty`  | `Long`       | Sum of `availableQty` across every warehouse (`0` if none exist)      |
| `stockStatus`        | `String`     | Always `"OUT_OF_STOCK"` — kept for consistency with the stock-report API |

### Error Responses

| HTTP Status | Scenario                                                     |
|-------------|------------------------------------------------------------------|
| `200`       | Filters valid but no out-of-stock records found (`totalRecords: 0`) |
| `500`       | Unexpected server error                                         |

---

## Inventory Item Status Values

| Status | Description                                          |
|--------|------------------------------------------------------|
| `A`    | Available — in stock and ready to be sold/dispatched |
| `I`    | Inactive — manually removed via the remove API       |

---

## Transaction Types (InventoryTransactionEO)

| Transaction Type    | Triggered By                       | qty field meaning                          |
|---------------------|------------------------------------|--------------------------------------------|
| `LOAD_INVENTORY`    | `POST /api/inventory/load`         | `+qty` units added                         |
| `RESTORE_INVENTORY` | `POST /api/inventory/restore`      | `+1` unit restored to AVAILABLE            |
| `REMOVE_INVENTORY`  | `POST /api/inventory/remove`       | `-1` unit marked Inactive                  |
| `STOCK_REFRESH`     | `POST /api/inventory/refresh-counts` | Net delta of `availableQty` after refresh (positive = gained, negative = lost) |

---

## Barcode & Batch Number Formats

| Field     | Format                              | Example                         |
|-----------|-------------------------------------|---------------------------------|
| `batchNo` | `BATCH-{variantId}-{timestamp}`     | `BATCH-101-1718099234567`       |
| `barcode` | `BC-{variantId}-{timestamp}-{seq}` | `BC-101-1718099234567-1`        |

- `{timestamp}` = milliseconds since Unix epoch at the time of the load request.
- `{seq}` = 1-based sequence number within the batch (1 … qty).
- Both values are guaranteed unique per load operation.
