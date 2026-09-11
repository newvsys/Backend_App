# Order Cancel Fix - Tracking History Record Creation

## Problem Identified

When calling the order-cancel API (`POST /api/order-cancel`), the system was:
1. ✗ NOT properly updating order status in all cases
2. ✗ NOT creating ShipmentTrackingHistoryEO records for cancelled shipments
3. ✗ Failing when order had no shipments (IndexOutOfBoundsException on `getFirst()`)

## Root Cause Analysis

### Issue 1: Unsafe List Access
**Original Code (Line 1157):**
```java
List<ShippingEO> shipping = shippingRepository.findByOrder(order);
ShippingEO ship = shipping.getFirst();  // ❌ Throws exception if list is empty
```

**Problem:** 
- If order has no shipments, `getFirst()` throws NoSuchElementException
- Exception was caught in outer try-catch, causing API to return 500 error
- Tracking history was never created because exception prevented reaching that code

### Issue 2: Single Shipment Processing
**Original Code:**
```java
ship.setShipmentStatus(Constants.SHIPMENT_STATUS_CANCELLED);
shippingRepository.save(ship);

// Create tracking history only for first shipment
// ... tracking history creation code ...
```

**Problem:**
- Only processed the first shipment (if it existed)
- If order had multiple shipments, only first was cancelled
- No null/empty list handling

## Solution Implemented

### Changes to OrderServiceImpl.cancelOrder()

#### 1. Safe Shipment List Handling
```java
// Fetch all shipments for this order (including cancelled ones)
List<ShippingEO> shippingList = shippingRepository.findByOrder(order);

if (shippingList == null || shippingList.isEmpty()) {
    logger.warn("No shipments found for order orderNumber={}, orderId={}", 
        orderCancelRequestDTO.getOrderNumber(), order.getOrderId());
} else {
    // Process each shipment
}
```

#### 2. Iterate Through All Shipments
```java
for (ShippingEO ship : shippingList) {
    // Skip if already cancelled
    if (Constants.SHIPMENT_STATUS_CANCELLED.equals(ship.getShipmentStatus())) {
        logger.info("Shipment already cancelled for shipmentId={}", ship.getShipmentId());
        continue;
    }
    
    // Cancel this shipment
    ship.setShipmentStatus(Constants.SHIPMENT_STATUS_CANCELLED);
    shippingRepository.save(ship);
    
    // Create tracking history for this shipment
    // ... tracking history creation ...
}
```

#### 3. Enhanced Tracking History
```java
ShipmentTrackingHistoryEO shipmentTrackingHistoryEO = new ShipmentTrackingHistoryEO();
shipmentTrackingHistoryEO.setShipment(ship);
shipmentTrackingHistoryEO.setStatus(Constants.SHIPMENT_STATUS_CANCELLED);
// NOW includes cancel reason
shipmentTrackingHistoryEO.setRemarks("Order Cancelled - Reason: " + cancelReasonDescription);
shipmentTrackingHistoryEO.setUpdatedAt(LocalDateTime.now());
shipmentTrackingHistoryRepository.save(shipmentTrackingHistoryEO);
logger.info("Created tracking history record for shipmentId={} with status=CANCELLED", 
    ship.getShipmentId());
```

## Benefits

### 1. Robustness
- ✅ Handles empty shipment lists gracefully (no crashes)
- ✅ Clear logging when no shipments found
- ✅ Skips already-cancelled shipments automatically

### 2. Completeness
- ✅ Processes ALL shipments for an order, not just the first
- ✅ Creates tracking history for each cancelled shipment
- ✅ Includes cancellation reason in remarks

### 3. Observability
- ✅ Better logging at each step
- ✅ Clear messages for debugging
- ✅ Differentiates between already-cancelled and newly-cancelled shipments

## API Flow After Fix

```
POST /api/order-cancel
{
  "orderNumber": "ORD-260908151808-001019",
  "reasonCode": "CANCEL_001",
  "comment": ""
}

Process:
1. ✓ Find order by order number
2. ✓ Update all order items to CANCELLED
3. ✓ Update order status to CANCELLED
4. ✓ Fetch all shipments for order
5. ✓ For each shipment:
   a. Skip if already cancelled
   b. Update shipment status to CANCELLED
   c. Create ShipmentTrackingHistoryEO record with:
      - status: "CANCELLED"
      - remarks: "Order Cancelled - Reason: {reason}"
      - updatedAt: current timestamp
   d. Call Shiprocket cancel API (if shipOrderId exists)
6. ✓ Process cancel order event
7. ✓ Return success response

GET /api/shipping-history/{trackingNumber}
- Returns ShipmentTrackingHistoryEO records
- Includes the newly created CANCELLED record
- Shows cancellation reason in remarks
```

## Test Scenarios

### Scenario 1: Order with Single Shipment
```
Order: ORD-260908151808-001019
Shipments: 1

Expected:
- Order status → CANCELLED
- Shipment status → CANCELLED
- Tracking history record created ✓
- tracking-number_1 in history shows CANCELLED ✓
```

### Scenario 2: Order with Multiple Shipments
```
Order: ORD-ABC-123456
Shipments: 3

Expected:
- Order status → CANCELLED
- All 3 shipments cancelled
- 3 tracking history records created (one per shipment)
- Each tracking number shows CANCELLED status
```

### Scenario 3: Order with No Shipments
```
Order: ORD-XYZ-654321
Shipments: 0

Expected:
- Order status → CANCELLED
- Warning logged: "No shipments found"
- No exception thrown ✓
- API returns success (not 500 error)
```

### Scenario 4: Already Cancelled Shipment
```
Order: ORD-OLD-111111
Shipments: 1 (already CANCELLED)

Expected:
- Shipment skipped (not re-processed)
- No duplicate CANCELLED record created (idempotency check)
- Log: "Shipment already cancelled for shipmentId=X"
```

## Verification

To verify the fix is working:

1. **Check Order Status:**
   ```sql
   SELECT order_status FROM orders WHERE order_number = 'ORD-260908151808-001019';
   -- Should return: CANCELLED
   ```

2. **Check Shipment Status:**
   ```sql
   SELECT shipment_status FROM shipping WHERE order_id = (
     SELECT order_id FROM orders WHERE order_number = 'ORD-260908151808-001019'
   );
   -- Should return: CANCELLED
   ```

3. **Check Tracking History:**
   ```sql
   SELECT * FROM shipment_tracking_history WHERE shipment_id = (
     SELECT shipment_id FROM shipping WHERE order_id = (
       SELECT order_id FROM orders WHERE order_number = 'ORD-260908151808-001019'
     ) AND status = 'CANCELLED'
   );
   -- Should return at least one record with status=CANCELLED
   ```

4. **Call Shipping History API:**
   ```
   GET /api/shipping-history/TRKORD-260908151808-001019_1
   
   Response should include:
   {
     "history": [
       {
         "status": "CANCELLED",
         "remarks": "Order Cancelled - Reason: ...",
         "date": "...",
         "location": "..."
       }
     ]
   }
   ```

## Files Modified

- `C:\personal\app\src\main\java\com\user\service\OrderServiceImpl.java`
  - Method: `cancelOrder()` (Lines 1153-1268)
  - Changes: Enhanced shipment handling and tracking history creation


