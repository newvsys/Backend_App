package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for the manual Shiprocket shipment create / update API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualShiprocketUpdateResponseDTO {

	/** SUCCESS or FAILURE. */
	private String responseStatus;

	/** Human-readable result message. */
	private String responseMessage;

	// ── Persisted shipment summary ─────────────────────────────────────────

	/** Internal shipment ID (shipping table PK). */
	private Long shipmentId;

	/** Order number for cross-reference. */
	private String orderNumber;

	/** Current shipment status after this update. */
	private String shipmentStatus;

	/** Shiprocket order_id now stored in the shipping record. */
	private Integer shiprocketOrderId;

	/** Shiprocket shipment_id now stored in the shipping record. */
	private Integer shiprocketShipmentId;

	/** AWB code now stored in the shipping record. */
	private String awbCode;

	/** Courier name now stored in the shipping record. */
	private String courierName;

	/** Courier company ID now stored in the shipping record. */
	private Integer courierCompanyId;

	/** Label URL now stored in the shipping record. */
	private String labelUrl;

	/** Track URL now stored in the shipping record. */
	private String trackUrl;

	/** Shipping price now stored in the shipping record. */
	private BigDecimal shippingPrice;

	/** Timestamp of the last update to the shipping record. */
	private LocalDateTime updatedAt;

	/** true if a new tracking history entry was created as part of this update. */
	private boolean historyEntryCreated;

	/** The Shiprocket step that was logged for this manual update. */
	private String stepLogged;

}
