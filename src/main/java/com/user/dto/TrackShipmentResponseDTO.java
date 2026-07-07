package com.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class TrackShipmentResponseDTO {

	private String responseStatus;

	private String responseMessage;

	// ── Internal shipment info from DB ──────────────────────────────────────
	private Long shipmentId;

	private String awbCode;

	private String trackingNumber;

	private String courierName;

	private Integer courierCompanyId;

	private String shipmentStatus;

	private String shipmentType;

	private LocalDateTime shippedDate;

	private LocalDateTime deliveredDate;

	private LocalDateTime estimatedDeliveryDate;

	private LocalDateTime expectedDeliveryDate;

	private LocalDateTime pickupScheduledDate;

	private String labelUrl;

	private String trackUrl;

	private Long orderId;

	private String orderNumber;

	/** Actual shipping price charged by the courier. */
	private BigDecimal shippingPrice;

	// ── Local tracking history from DB ──────────────────────────────────────
	private List<ShipTrackHistoryDTO> trackingHistory;

	/** All couriers evaluated during serviceability check, in ranked order. */
	private List<CourierSelectionLogDTO> courierCandidates;

	// ── Live tracking data from Shiprocket API ───────────────────────────────
	private Map<String, Object> shiprocketTracking;

}
