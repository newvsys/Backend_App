package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentInfoDTO {

	private Long shipmentId;

	/** Courier tracking / AWB number (shipment number). */
	private String trackingNumber;

	private String courierName;

	private Integer courierCompanyId;

	/** FORWARD | RETURN_PICKUP */
	private String shipmentType;

	private String shipmentStatus;

	private String awb;

	/** Shipping label URL. */
	private String labelUrl;

	private Integer shipOrderId;

	private Integer shipShipmentId;

	private LocalDateTime shippedDate;

	private LocalDateTime deliveredDate;

	private LocalDateTime estimatedDeliveryDate;

	private LocalDateTime expectedDeliveryDate;

	private LocalDateTime pickupScheduledDate;

	private String trackUrl;

	private Double length;

	private Double breadth;

	private Double height;

	private Double weight;

	/** Actual shipping price charged by the selected courier. */
	private BigDecimal shippingPrice;

	/** All couriers evaluated during serviceability check, in ranked order. */
	private List<CourierSelectionLogDTO> courierCandidates;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	/** Full shipment tracking/history events ordered by time ascending. */
	private List<ShipTrackHistoryDTO> shipmentHistory;

}
