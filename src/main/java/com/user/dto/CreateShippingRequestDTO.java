package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for creating a new shipping record via POST /api/order/{orderId}/shipping
 * Accepts all ShippingEO fields with appropriate validation for creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateShippingRequestDTO {

	@NotNull(message = "Carton ID is required")
	private Long cartonId;

	private String trackingNumber;

	@NotBlank(message = "Courier name is required")
	private String courierName;

	@NotBlank(message = "Type (FORWARD/RETURN_PICKUP) is required")
	private String type; // Will be converted to enum: FORWARD | RETURN_PICKUP

	@NotBlank(message = "Shipment status is required")
	private String shipmentStatus;

	private LocalDateTime shippedDate;

	private LocalDateTime deliveredDate;
	private String orderStatus;
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
}

