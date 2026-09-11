package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for returning shipping record details.
 * Used in responses for GET and POST shipping endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingDetailDTO {

	private Long shipmentId;

	private Long orderId;

	private String orderNumber;

	private Long cartonId;

	private String cartonNo;

	private String trackingNumber;

	private String courierName;

	private String type;

	private String shipmentStatus;

	private LocalDateTime shippedDate;

	private LocalDateTime deliveredDate;

	private Double length;

	private Double breadth;

	private Double height;

	private Double weight;

	private String awb;

	private String labelUrl;

	private Integer shipOrderId;

	private Integer shipShipmentId;

	private Long pickupId;

	private LocalDateTime pickupScheduledDate;

	private String pickupToken;

	private Integer courierCompanyId;

	private LocalDateTime estimatedDeliveryDate;

	private LocalDateTime expectedDeliveryDate;

	private String trackUrl;

	private BigDecimal shippingPrice;

	private String shiprocketOrderStatus;

	private String generateAwbStatus;

	private String requestPickupStatus;

	private String generateLabelStatus;

	private String trackShipmentStatus;

	private String estimateStatus;

	private Long warehouseId;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}

