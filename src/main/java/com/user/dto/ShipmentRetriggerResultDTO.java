package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-shipment outcome of a shipping-process retrigger request. One entry is returned
 * for every FORWARD shipment found under the order that was considered for retrigger.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentRetriggerResultDTO {

	private Long shipmentId;

	private String trackingNumber;

	/** Shipment status before the retrigger attempt. */
	private String previousStatus;

	/** Shipment status after the retrigger attempt (may be unchanged). */
	private String currentStatus;

	/**
	 * RETRIGGERED — the Shiprocket flow was re-run for this shipment. SKIPPED — this
	 * shipment was not retriggered (e.g. already delivered/cancelled, or still in
	 * cooldown). FAILED — an unexpected error occurred while retriggering.
	 */
	private String action;

	private String message;

	/**
	 * The Shiprocket processing step that failed (e.g. GENERATE_AWB, REQUEST_PICKUP,
	 * GENERATE_LABEL), populated only when action=FAILED. Null when not applicable.
	 */
	private String failedStep;

	/**
	 * Detailed failure reason for the UI to display — the underlying error message
	 * captured from the failed step (from shiprocket_order_log.error_message) or the
	 * exception message if the retrigger itself threw an error. Populated only when
	 * action=FAILED.
	 */
	private String failureReason;

}

