package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload for {@code ShippingServiceImpl.processShiprocketOrderEvent}. Captures
 * the outcome of the (mostly best-effort, step-by-step) Shiprocket order/AWB/pickup/label
 * flow so callers (e.g. {@code retriggerShippingProcess}) can inspect the result instead of
 * having to re-query the DB / step logs themselves.
 * <p>
 * {@code responseStatus} is {@link com.user.utility.Constants#SUCCESS_STATUS} only when the
 * Shiprocket order, courier, AWB and label were all successfully generated
 * (equivalent to the existing {@code allShipmentDetailsPresent} check); otherwise it is
 * {@link com.user.utility.Constants#FAILURE_STATUS} and {@code failedStep} /
 * {@code responseMessage} describe where processing stopped.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiprocketOrderEventResponseDTO {

	private String responseStatus;

	private String responseMessage;

	private Long shipmentId;

	private Long orderId;

	private Long warehouseId;

	/** Shiprocket order id returned by CREATE_ORDER. */
	private Integer shipOrderId;

	/** Shiprocket shipment id returned by CREATE_ORDER. */
	private Integer shipShipmentId;

	private String awbCode;

	private Integer courierCompanyId;

	private String courierName;

	private String labelUrl;

	private String trackUrl;

	/** Final ShippingEO.shipmentStatus after processing (e.g. PICKUP_SCHEDULED, MANUAL_PROCESSING_REQUIRED). */
	private String shipmentStatus;

	/** The step (CREATE_ORDER / FIND_BEST_COURIER / GENERATE_AWB / REQUEST_PICKUP / GENERATE_LABEL /
	 * TRACK_SHIPMENT / PROCESS_EVENT) at which processing stopped/failed, or null on full success. */
	private String failedStep;

}

