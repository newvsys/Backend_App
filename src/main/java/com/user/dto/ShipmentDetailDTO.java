package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentDetailDTO {

	private Long shipmentId;

	private String orderNumber;

	private Long orderId;

	private String trackingNumber;

	private String courierName;

	private Integer courierCompanyId;

	private String shipmentType;

	private String shipmentStatus;

	private LocalDateTime shippedDate;

	private LocalDateTime deliveredDate;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	/** Awb code assigned by the courier. */
	private String awb;

	/** Actual shipping price charged by the courier. */
	private BigDecimal shippingPrice;

	/** All couriers evaluated during serviceability check, in ranked order. */
	private List<CourierSelectionLogDTO> courierCandidates;

	private List<ShipTrackHistoryDTO> trackingHistory;

}
