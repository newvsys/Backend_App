package com.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the standard Shiprocket webhook payload sent on shipment status changes.
 *
 * Example payload: { "awb": "14492489597159", "current_status": "Delivered",
 * "current_status_id": 7, "shipment_id": 123456789, "order_id": "ORDER123", "etd":
 * "2024-01-15 16:00:00", "scans": [ { "date": "2024-01-15 14:30:00", "activity":
 * "Delivered", "location": "Mumbai, Maharashtra", "sr-status": "7", "sr-status-label":
 * "DELIVERED" } ] }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShiprocketWebhookDTO {

	/** AWB / courier tracking number assigned to the shipment */
	@JsonProperty("awb")
	private String awb;

	/** Human-readable current status (e.g. "Delivered", "In Transit") */
	@JsonProperty("current_status")
	private String currentStatus;

	/** Numeric status code used by Shiprocket */
	@JsonProperty("current_status_id")
	private Integer currentStatusId;

	/** Shiprocket internal shipment ID */
	@JsonProperty("shipment_id")
	private Object shipmentId; // can be Integer or String from Shiprocket

	/** Shiprocket / merchant order ID */
	@JsonProperty("order_id")
	private Object orderId;

	/** Estimated delivery date */
	@JsonProperty("etd")
	private String etd;

	/** List of scan events (most-recent first or last depending on courier) */
	@JsonProperty("scans")
	private List<ScanEvent> scans;

	// ──────────────────────────────────────────────────────────────────────────
	// Nested scan-event object
	// ──────────────────────────────────────────────────────────────────────────

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ScanEvent {

		/** Date-time of the scan event (format: "yyyy-MM-dd HH:mm:ss") */
		@JsonProperty("date")
		private String date;

		/** Activity description */
		@JsonProperty("activity")
		private String activity;

		/** City / location where the scan happened */
		@JsonProperty("location")
		private String location;

		/** Shiprocket numeric status string for this scan */
		@JsonProperty("sr-status")
		private String srStatus;

		/** Shiprocket status label for this scan */
		@JsonProperty("sr-status-label")
		private String srStatusLabel;

	}

}
