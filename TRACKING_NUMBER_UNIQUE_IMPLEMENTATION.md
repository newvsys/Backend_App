# Unique Tracking Number Implementation

## Overview
Tracking numbers in the `shipping` table are now enforced to be unique across all shipments, including cancelled ones. This ensures no duplicate tracking numbers can exist in the system.

## Changes Made

### 1. Entity Model (ShippingEO.java)
Added unique constraint to the `trackingNumber` column:
```java
@Column(name = "tracking_number", unique = true, nullable = false)
private String trackingNumber;
```

**Key Points:**
- `unique = true`: Database enforces uniqueness constraint at the column level
- `nullable = false`: Tracking number is now required (must be provided or auto-generated)
- Uniqueness is enforced across ALL shipments, including cancelled ones

### 2. Database Migration (add_tracking_number_unique_constraint.sql)
Migration script to add the unique constraint to the existing `shipping` table:
```sql
ALTER TABLE shipping ADD CONSTRAINT uk_shipping_tracking_number UNIQUE (tracking_number);
```

**Note:** If duplicate tracking numbers exist before applying this migration, they must be resolved first.

### 3. Service Layer (ShippingServiceImpl.java)

#### Auto-Generation Logic
When no tracking number is provided:
1. Generate base tracking number: `TRK_{orderNumber}_{sequenceNumber}`
   - sequenceNumber = total shipment count for order + 1 (includes cancelled)
   
2. Check if generated tracking number already exists
3. If it exists, attempt alternative sequence numbers (up to +1000 attempts)
4. If no unique number found after 1000 attempts, return error

#### Manual Tracking Number Validation
When a tracking number is explicitly provided:
1. Validate it doesn't already exist in the system
2. For CREATE operations: Reject if tracking number exists
3. For UPDATE operations: Allow if it's the same shipment, reject if it belongs to another shipment

## Example Scenarios

### Scenario 1: Create New Shipment (Auto-Generate)
```
Order: ORD-260908151808-001019
Existing shipments for this order: 3

Request:
{
  "cartonId": 1,
  "courierName": "DHL",
  // trackingNumber not provided
}

Result:
- Generated: TRK_ORD-260908151808-001019_4
- Verified unique: ✓
- Shipment created with tracking number
```

### Scenario 2: Duplicate Detected (Auto-Generate with Conflict)
```
Generated: TRK_ORD-260908151808-001019_4
Conflict detected: Tracking number already exists
Fallback attempts:
  - TRK_ORD-260908151808-001019_5 ✓ (unique)
  - Shipment created with tracking number _5
```

### Scenario 3: Manual Tracking Number (Unique Check)
```
Request:
{
  "cartonId": 1,
  "courierName": "DHL",
  "trackingNumber": "CUSTOM_TRK_12345"
}

Validation:
- Check if "CUSTOM_TRK_12345" exists
- Not found: ✓
- Shipment created with provided tracking number
```

### Scenario 4: Duplicate Manual Tracking Number
```
Request:
{
  "cartonId": 1,
  "courierName": "DHL",
  "trackingNumber": "EXISTING_TRK_12345"  // Already exists
}

Response:
{
  "responseStatus": "FAILURE",
  "responseMessage": "Tracking number already exists: EXISTING_TRK_12345"
}
```

### Scenario 5: Update with Same Tracking Number
```
Existing shipment ID: 100
Tracking number: TRK_ORD_12345_1

Request (Update):
{
  "cartonId": 2,
  "courierName": "FedEx",
  "trackingNumber": "TRK_ORD_12345_1"  // Same as current
}

Result:
- Allowed: ✓ (updating same shipment)
- Shipment updated successfully
```

## Database Query Examples

### Find Duplicate Tracking Numbers (Before Migration)
```sql
SELECT tracking_number, COUNT(*) as count 
FROM shipping 
WHERE tracking_number IS NOT NULL 
GROUP BY tracking_number 
HAVING COUNT(*) > 1;
```

### Remove Duplicates (Example)
```sql
-- Keep the most recent shipment, delete older ones with duplicate tracking
DELETE FROM shipping 
WHERE tracking_number IN (
  SELECT tracking_number FROM (
    SELECT tracking_number 
    FROM shipping 
    GROUP BY tracking_number 
    HAVING COUNT(*) > 1
  ) AS duplicates
)
AND shipment_id NOT IN (
  SELECT MAX(shipment_id) 
  FROM shipping 
  GROUP BY tracking_number 
  HAVING COUNT(*) > 1
);
```

## Error Handling

### Error Case 1: Unable to Generate Unique Number
```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Unable to generate unique tracking number for orderId=12345"
}
```
**Cause:** System exhausted 1000 attempts to find a unique sequence number

### Error Case 2: Duplicate Manual Tracking Number
```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Tracking number already exists: TRK_ORD_123_456"
}
```
**Cause:** Provided tracking number is already used by another shipment

## Considerations

### 1. Cancelled Shipments
- Cancelled shipments are included in uniqueness checks
- Once a tracking number is assigned (even if later cancelled), it cannot be reused
- This is by design to maintain tracking integrity

### 2. Sequential Generation
- Sequence number is based on total shipment count (including cancelled)
- This ensures tracking numbers are somewhat sequential for an order
- Example: Order with 3 cancelled + 2 active = next sequence = 6

### 3. Performance
- Uniqueness check on auto-generation: `O(log n)` database lookup
- Retry loop: Maximum 1000 attempts (worst case)
- Recommended: Pre-allocate or batch tracking numbers for high-volume scenarios

### 4. Migration Safety
- Apply migration during maintenance window to avoid lock contention
- Backup database before applying migration
- Verify no duplicate tracking numbers exist first

## Testing Checklist

- [x] Auto-generate unique tracking number
- [x] Detect duplicate during auto-generation
- [x] Fallback to alternative sequence number
- [x] Reject duplicate manual tracking number
- [x] Allow update with same tracking number
- [x] Include cancelled shipments in uniqueness check
- [x] Database constraint prevents SQL-level duplicates
- [x] Comprehensive error messages in response

## Related APIs

- `POST /api/order/{orderId}/shipping` - Create/update shipping with tracking number
- `GET /api/shipping-history/{trackingNumber}` - Fetch by tracking number (non-cancelled only)


