package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for manually creating or updating a Shiprocket shipment order and its
 * corresponding tracking history.
 *
 * <p>
 * Use this API when the automated Shiprocket flow fails at any step (CREATE_ORDER,
 * GENERATE_AWB, REQUEST_PICKUP, GENERATE_LABEL) so that an admin can supply the missing
 * data and keep the shipping & history tables in a consistent state.
 *
 * <p>
 * Shipment identification: supply at least one of {@code shipmentId}, {@code orderId}, or
 * {@code orderNumber}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualShiprocketUpdateRequestDTO {

	// ── Shipment identification (at least one is required) ────────────────────

	/** Internal shipment ID (primary key of the shipping table). */
	private Long shipmentId;

	/** Internal order ID — used to look up the shipment when shipmentId is absent. */
	private Long orderId;

	/**
	 * Human-readable order number — used when neither shipmentId nor orderId is supplied.
	 */
	private String orderNumber;

	// ── Shiprocket-returned identifiers ──────────────────────────────────────

	/** Shiprocket order_id returned by the CREATE_ORDER API call. */
	private Integer shiprocketOrderId;

	/** Shiprocket shipment_id returned by the CREATE_ORDER API call. */
	private Integer shiprocketShipmentId;

	// ── Courier / AWB fields ──────────────────────────────────────────────────

	/** AWB (Air Waybill) code returned by the GENERATE_AWB step. */
	private String awbCode;

	/** Name of the courier partner (e.g. "Delhivery", "BlueDart"). */
	private String courierName;

	/** Courier company ID used in Shiprocket's serviceability / AWB APIs. */
	private Integer courierCompanyId;

	// ── Shipment financial / status fields ───────────────────────────────────

	/** Shipping price charged by the courier (INR). */
	private BigDecimal shippingPrice;

	/**
	 * New shipment status to persist in the shipping table. Examples: CREATED,
	 * PICKUP_SCHEDULED, IN_TRANSIT, DELIVERED. Leave null to skip updating the shipment
	 * status field.
	 */
	private String shipmentStatus;

	// ── URL fields ────────────────────────────────────────────────────────────

	/** URL of the generated shipping label PDF. */
	private String labelUrl;

	/** Public tracking URL for the end customer. */
	private String trackUrl;

	// ── Dates (ISO-8601 string: "yyyy-MM-dd HH:mm:ss" or "yyyy-MM-dd") ───────

	/** Estimated delivery date as a parseable date string. */
	private String estimatedDeliveryDate;

	/** Expected delivery date (ETD) returned by the AWB / tracking API. */
	private String expectedDeliveryDate;

	/** Scheduled pickup date returned by the REQUEST_PICKUP step. */
	private String pickupScheduledDate;

	// ── Pickup fields ─────────────────────────────────────────────────────────

	/** Pickup ID returned by the REQUEST_PICKUP API. */
	private Long pickupId;

	/** Pickup token number returned by the REQUEST_PICKUP API. */
	private String pickupToken;

	// ── Tracking history entry (optional) ───────────────────────────────────

	/**
	 * Status to record in the shipment_tracking_history table. If null or blank, no
	 * history entry is created.
	 */
	private String historyStatus;

	/** Geographic location associated with this tracking event. */
	private String historyLocation;

	/** Human-readable remarks for this tracking history entry. */
	private String historyRemarks;

	// ── Audit / log fields ────────────────────────────────────────────────────

	/**
	 * The Shiprocket processing step this manual update corresponds to. Allowed values:
	 * CREATE_ORDER, GENERATE_AWB, REQUEST_PICKUP, GENERATE_LABEL, MANUAL_OVERRIDE.
	 * Defaults to MANUAL_OVERRIDE if not provided.
	 */
	private String step;

	/** Optional free-text notes describing why the manual update was needed. */
	private String notes;

}
