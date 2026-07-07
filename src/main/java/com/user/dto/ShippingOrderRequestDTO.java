package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for manually creating or updating a shipping record by order number.
 *
 * <p>
 * Used by:
 * <ul>
 * <li>POST /api/shipment/order/{orderNumber} — create a new shipping record when none
 * exists</li>
 * <li>PUT /api/shipment/order/{orderNumber} — update fields on an existing shipping
 * record</li>
 * </ul>
 * All fields are optional for PUT; only the fields you supply will be updated. For POST,
 * warehouseId is recommended so the record can be associated to the right warehouse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingOrderRequestDTO {

	// ── Warehouse (used when creating a new record) ───────────────────────
	/** Internal warehouse ID to link the shipment to. */
	private Long warehouseId;

	// ── Shiprocket identifiers ─────────────────────────────────────────────
	/** Shiprocket order_id returned by the CREATE_ORDER step. */
	private Integer shiprocketOrderId;

	/** Shiprocket shipment_id returned by the CREATE_ORDER step. */
	private Integer shiprocketShipmentId;

	// ── Courier / AWB fields ──────────────────────────────────────────────
	/** AWB (Air Waybill) code from the GENERATE_AWB step. */
	private String awbCode;

	/** Human-readable courier name (e.g. "Delhivery", "BlueDart"). */
	private String courierName;

	/** Courier company ID used in Shiprocket's serviceability / AWB APIs. */
	private Integer courierCompanyId;

	// ── Shipment fields ───────────────────────────────────────────────────
	/**
	 * Shipment status to persist. Examples: CREATED, PICKUP_SCHEDULED, IN_TRANSIT,
	 * DELIVERED, CANCELLED.
	 */
	private String shipmentStatus;

	/**
	 * Shipment type. Defaults to FORWARD for new records. Allowed: FORWARD |
	 * RETURN_PICKUP
	 */
	private String shipmentType;

	/** Tracking number (e.g. TRK{orderNumber}_{warehouseId}). */
	private String trackingNumber;

	// ── Dimensions & weight ───────────────────────────────────────────────
	private Double length;

	private Double breadth;

	private Double height;

	private Double weight;

	// ── Financial ─────────────────────────────────────────────────────────
	/** Shipping price charged by the courier (INR). */
	private BigDecimal shippingPrice;

	// ── URL fields ────────────────────────────────────────────────────────
	/** URL of the generated shipping label PDF. */
	private String labelUrl;

	/** Public tracking URL for the end customer. */
	private String trackUrl;

	// ── Date fields (ISO-8601: "yyyy-MM-dd HH:mm:ss" or "yyyy-MM-dd") ────
	/** Scheduled pickup date (from REQUEST_PICKUP step). */
	private String pickupScheduledDate;

	/** Estimated delivery date. */
	private String estimatedDeliveryDate;

	/** Expected delivery date (ETD). */
	private String expectedDeliveryDate;

	/** Date the shipment was actually shipped / handed to courier. */
	private String shippedDate;

	/** Date the shipment was actually delivered. */
	private String deliveredDate;

	// ── Pickup fields ─────────────────────────────────────────────────────
	/** Pickup ID returned by the REQUEST_PICKUP API. */
	private Long pickupId;

	/** Pickup token number returned by the REQUEST_PICKUP API. */
	private String pickupToken;

	// ── Tracking history entry (optional) ────────────────────────────────
	/**
	 * If supplied, a new row is inserted into shipment_tracking_history. Leave null to
	 * skip creating a history entry.
	 */
	private String historyStatus;

	/** Geographic location associated with this tracking event. */
	private String historyLocation;

	/** Human-readable remarks for this tracking history entry. */
	private String historyRemarks;

	// ── Audit ─────────────────────────────────────────────────────────────
	/** Optional free-text notes describing the reason for the manual action. */
	private String notes;

}
