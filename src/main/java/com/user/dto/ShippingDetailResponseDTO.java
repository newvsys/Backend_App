package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO returned by GET /api/shipment/order/{orderNumber}. Contains the full
 * shipping record and its tracking history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingDetailResponseDTO {

	/** SUCCESS or FAILURE. */
	private String responseStatus;

	/** Human-readable result message. */
	private String responseMessage;

	// ── Shipment identifiers ───────────────────────────────────────────────
	/** Internal shipment PK from the shipping table. */
	private Long shipmentId;

	/** Human-readable order number. */
	private String orderNumber;

	/** Internal order ID. */
	private Long orderId;

	// ── Shiprocket identifiers ─────────────────────────────────────────────
	/** Shiprocket order_id stored after the CREATE_ORDER step. */
	private Integer shiprocketOrderId;

	/** Shiprocket shipment_id stored after the CREATE_ORDER step. */
	private Integer shiprocketShipmentId;

	// ── Courier / AWB ─────────────────────────────────────────────────────
	/** AWB (Air Waybill) code. */
	private String awbCode;

	/** Courier partner name. */
	private String courierName;

	/** Courier company ID. */
	private Integer courierCompanyId;

	// ── Shipment status & type ─────────────────────────────────────────────
	/** Current shipment status (e.g. CREATED, IN_TRANSIT, DELIVERED). */
	private String shipmentStatus;

	/** Shipment type: FORWARD or RETURN_PICKUP. */
	private String shipmentType;

	/** Internal tracking number. */
	private String trackingNumber;

	// ── Dimensions & weight ───────────────────────────────────────────────
	private Double length;

	private Double breadth;

	private Double height;

	private Double weight;

	// ── Financial ─────────────────────────────────────────────────────────
	private BigDecimal shippingPrice;

	// ── URL fields ────────────────────────────────────────────────────────
	private String labelUrl;

	private String trackUrl;

	// ── Warehouse ─────────────────────────────────────────────────────────
	private Long warehouseId;

	private String warehouseName;

	// ── Dates ─────────────────────────────────────────────────────────────
	private LocalDateTime pickupScheduledDate;

	private LocalDateTime estimatedDeliveryDate;

	private LocalDateTime expectedDeliveryDate;

	private LocalDateTime shippedDate;

	private LocalDateTime deliveredDate;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	// ── Pickup ────────────────────────────────────────────────────────────
	private Long pickupId;

	private String pickupToken;

	// ── Tracking history ──────────────────────────────────────────────────
	/** Chronological list of tracking history events for this shipment. */
	private List<ShipTrackHistoryDTO> trackingHistory;

}
