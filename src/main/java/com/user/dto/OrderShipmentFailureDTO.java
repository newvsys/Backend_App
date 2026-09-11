package com.user.dto;

import com.user.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Combined view of an {@code OrderEO} and its related {@code ShippingEO}, returned by
 * the "Confirmed / Ready to Ship orders with a failed Shiprocket step" admin API.
 * <p>
 * Order details and shipment details are grouped into their own nested objects
 * ({@link OrderDetails} and {@link ShippingDetails}) so the two concerns stay clearly
 * separated in the API response. {@code shippingDetails} will be {@code null} when the
 * order does not yet have any shipment record.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderShipmentFailureDTO {

	private OrderDetails orderDetails;

	private ShippingDetails shippingDetails;

	/** Names of the specific step(s) whose status equals FAILURE for this shipment. */
	private List<String> failedSteps;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class OrderDetails {

		private Long orderId;

		private String orderNumber;

		private String orderStatus;

		private String paymentStatus;

		private BigDecimal totalAmount;

		private LocalDateTime orderCreatedAt;

		private String customerName;

		private String customerEmail;

		private String customerMobile;

	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class ShippingDetails {

		private Long shipmentId;

		private String trackingNumber;

		private String shipmentType;

		private String shipmentStatus;

		private String awb;

		private String courierName;

		private Integer courierCompanyId;

		private BigDecimal shippingPrice;

		private LocalDateTime shippedDate;

		private LocalDateTime deliveredDate;

		private LocalDateTime shipmentCreatedAt;

		private LocalDateTime shipmentUpdatedAt;

		/** Id of the {@code CartonEO} used to pack this shipment, if any. */
		private Long cartonId;

		private Double length;

		private Double breadth;

		private Double height;

		private Double weight;

		private String labelUrl;

		private Integer shipOrderId;

		private Integer shipShipmentId;

		private Long pickupId;

		private String pickupToken;

		private LocalDateTime estimatedDeliveryDate;

		private LocalDateTime expectedDeliveryDate;

		private String trackUrl;

		// ── Individual Shiprocket step outcomes ────────────────────────────
		private String shiprocketOrderStatus;

		private String generateAwbStatus;

		private String requestPickupStatus;

		private String generateLabelStatus;

		private String trackShipmentStatus;

		private String estimateStatus;

	// ── Status history logs for each Shiprocket step ────────────────────
	private List<StatusHistoryLogDTO> shiprocketOrderStatuslog;

	private List<StatusHistoryLogDTO> generateAwbStatuslog;

	private List<StatusHistoryLogDTO> requestPickupStatuslog;

	private List<StatusHistoryLogDTO> generateLabelStatuslog;

	private List<StatusHistoryLogDTO> trackShipmentStatuslog;

	private List<StatusHistoryLogDTO> estimateStatuslog;

	// ── Shipment audit logs (complete history from shiprocket_order_log) ──
	private List<ShipmentLogDTO> shipmentlogs;

	}

}
