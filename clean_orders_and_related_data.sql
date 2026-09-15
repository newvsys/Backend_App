-- ============================================================================
-- Database Cleanup Script: Orders, Shipments, Inventory and Related Tables
-- ============================================================================
-- This script cleans all order-related data while maintaining referential
-- integrity by respecting foreign key dependencies.
--
-- Execution Order:
-- 1. Delete shipment tracking history records
-- 2. Delete Shiprocket step status history records
-- 3. Delete shipment items
-- 4. Delete shipments
-- 5. Delete return status history
-- 6. Delete return requests
-- 7. Delete refund transactions
-- 8. Delete payments
-- 9. Delete order items
-- 10. Delete order addresses
-- 11. Delete orders
-- 12. Reset inventory (optional)
-- ============================================================================

-- Disable foreign key checks during cleanup (MySQL/MariaDB)
-- For PostgreSQL, use: SET session_replication_role = 'replica';
-- For SQL Server, use: DISABLE TRIGGER ALL

-- ============================================================================
-- SECTION 1: DELETE SHIPMENT TRACKING AND STATUS HISTORY
-- ============================================================================

-- Delete shipment tracking history records
DELETE FROM shipment_tracking_history WHERE shipment_id IS NOT NULL;
TRUNCATE TABLE shipment_tracking_history;

-- Delete Shiprocket order status history
DELETE FROM shiprocket_order_status_history WHERE shipment_id IS NOT NULL;
TRUNCATE TABLE shiprocket_order_status_history;

-- Delete Generate AWB status history
DELETE FROM generate_awb_status_history WHERE shipment_id IS NOT NULL;
TRUNCATE TABLE generate_awb_status_history;

-- Delete Request Pickup status history
DELETE FROM request_pickup_status_history WHERE shipment_id IS NOT NULL;
TRUNCATE TABLE request_pickup_status_history;

-- Delete Generate Label status history
DELETE FROM generate_label_status_history WHERE shipment_id IS NOT NULL;
TRUNCATE TABLE generate_label_status_history;

-- Delete Track Shipment status history
DELETE FROM track_shipment_status_history WHERE shipment_id IS NOT NULL;
TRUNCATE TABLE track_shipment_status_history;

-- Delete Estimate status history
DELETE FROM estimate_status_history WHERE shipment_id IS NOT NULL;
TRUNCATE TABLE estimate_status_history;

-- ============================================================================
-- SECTION 2: DELETE SHIPMENT ITEMS
-- ============================================================================

DELETE FROM shipment_items WHERE shipment_id IS NOT NULL;
TRUNCATE TABLE shipment_items;

-- ============================================================================
-- SECTION 3: DELETE SHIPMENTS
-- ============================================================================

DELETE FROM shipping WHERE order_id IS NOT NULL;
TRUNCATE TABLE shipping;

-- ============================================================================
-- SECTION 4: DELETE RETURN REQUEST TRACKING
-- ============================================================================

-- Delete return status history (tracks status changes in return_request)
DELETE FROM return_status_history WHERE return_request_id IS NOT NULL;
TRUNCATE TABLE return_status_history;

-- Delete return requests (associated with orders)
DELETE FROM return_request WHERE order_id IS NOT NULL;
TRUNCATE TABLE return_request;

-- ============================================================================
-- SECTION 5: DELETE REFUNDS AND PAYMENTS
-- ============================================================================

-- Delete refund transactions (linked to orders and order items)
DELETE FROM refund_transaction WHERE order_id IS NOT NULL;
TRUNCATE TABLE refund_transaction;

-- Delete payments (linked to orders)
DELETE FROM payments WHERE order_id IS NOT NULL;
TRUNCATE TABLE payments;

-- ============================================================================
-- SECTION 6: DELETE ORDER ITEMS AND ADDRESSES
-- ============================================================================

-- Delete order items
DELETE FROM order_items WHERE order_id IS NOT NULL;
TRUNCATE TABLE order_items;

-- Delete order addresses
DELETE FROM order_addresses WHERE order_id IS NOT NULL;
TRUNCATE TABLE order_addresses;

-- ============================================================================
-- SECTION 7: DELETE ORDERS
-- ============================================================================

DELETE FROM orders;
TRUNCATE TABLE orders;

-- Reset the auto-increment counter for orders (optional)
-- For MySQL/MariaDB:
ALTER TABLE orders AUTO_INCREMENT = 1;

-- For PostgreSQL:
-- ALTER SEQUENCE orders_order_id_seq RESTART WITH 1;

-- For SQL Server:
-- DBCC CHECKIDENT ('orders', RESEED, 0);

-- ============================================================================
-- SECTION 8: CLEANUP INVENTORY (OPTIONAL)
-- ============================================================================
-- Uncomment the following section if you want to reset inventory-related data.
-- This is optional as inventory may be shared across multiple orders.

/*
-- Delete inventory transactions (audit trail)
DELETE FROM inventory_transactions WHERE inventory_id IS NOT NULL;
TRUNCATE TABLE inventory_transactions;

-- Reset inventory quantities (optional - only if you want to restore stock)
UPDATE inventory SET available_qty = total_qty, updated_at = NOW();
*/

-- ============================================================================
-- SECTION 9: CLEANUP COMMUNICATION LOGS (OPTIONAL)
-- ============================================================================
-- Uncomment if you want to purge communication logs (emails, SMS, etc.)

/*
DELETE FROM communication_log WHERE purpose IN (
    'ORDER_CONFIRMATION',
    'ORDER_STATUS_UPDATE',
    'REFUND',
    'ADMIN_PROCESS_SHIPMENT'
);
*/

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================
-- Run these queries to verify the cleanup was successful

SELECT 'Orders' as table_name, COUNT(*) as row_count FROM orders
UNION ALL
SELECT 'Order Items', COUNT(*) FROM order_items
UNION ALL
SELECT 'Order Addresses', COUNT(*) FROM order_addresses
UNION ALL
SELECT 'Payments', COUNT(*) FROM payments
UNION ALL
SELECT 'Refund Transactions', COUNT(*) FROM refund_transaction
UNION ALL
SELECT 'Shipments', COUNT(*) FROM shipping
UNION ALL
SELECT 'Shipment Items', COUNT(*) FROM shipment_items
UNION ALL
SELECT 'Shipment Tracking History', COUNT(*) FROM shipment_tracking_history
UNION ALL
SELECT 'Return Requests', COUNT(*) FROM return_request
UNION ALL
SELECT 'Return Status History', COUNT(*) FROM return_status_history
UNION ALL
SELECT 'Shiprocket Order Status History', COUNT(*) FROM shiprocket_order_status_history
UNION ALL
SELECT 'Generate AWB Status History', COUNT(*) FROM generate_awb_status_history
UNION ALL
SELECT 'Request Pickup Status History', COUNT(*) FROM request_pickup_status_history
UNION ALL
SELECT 'Generate Label Status History', COUNT(*) FROM generate_label_status_history
UNION ALL
SELECT 'Track Shipment Status History', COUNT(*) FROM track_shipment_status_history
UNION ALL
SELECT 'Estimate Status History', COUNT(*) FROM estimate_status_history;

-- ============================================================================
-- END OF CLEANUP SCRIPT
-- ============================================================================
-- Note: This script deletes all order data but preserves:
-- - Customer data (customers table)
-- - Product data (products, product_variants tables)
-- - Warehouse data (warehouses table)
-- - Inventory levels (inventory table) - unless you uncomment the optional section
--
-- To restore foreign key checks (if disabled):
-- MySQL/MariaDB: SET FOREIGN_KEY_CHECKS = 1;
-- PostgreSQL: SET session_replication_role = 'origin';
-- SQL Server: ENABLE TRIGGER ALL
-- ============================================================================

