package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for GET /api/shipment/order/{orderNumber}/shiprocket-payload.
 *
 * <p>
 * Fetches the live/current shipment data for the given internal order number — first
 * resolving the linked Shiprocket order/shipment via the local DB, then calling the real
 * Shiprocket API (order details + AWB tracking) to refresh the courier, AWB and status
 * fields — and returns it in exactly the same shape expected by the request body of:
 * <ul>
 * <li>PUT /api/shipment/order/{orderNumber}</li>
 * </ul>
 * So the response of this GET can be used, as-is (after review/edits), as the request
 * body for the PUT call above.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentPutPayloadResponseDTO {

	private String responseStatus;

	private String responseMessage;

	// ── Fields matching ShippingOrderRequestDTO (PUT body) ─────────────────
	private Long warehouseId;

	private Integer shiprocketOrderId;

	private Integer shiprocketShipmentId;

	private String awbCode;

	private String courierName;

	private Integer courierCompanyId;

	private String shipmentStatus;

	private String shipmentType;

	private String trackingNumber;

	private Double length;

	private Double breadth;

	private Double height;

	private Double weight;

	private BigDecimal shippingPrice;

	private String labelUrl;

	private String trackUrl;

	/** ISO date string, e.g. "2026-08-07". */
	private String estimatedDeliveryDate;

	/** ISO date string, e.g. "2026-08-07". */
	private String expectedDeliveryDate;

}

