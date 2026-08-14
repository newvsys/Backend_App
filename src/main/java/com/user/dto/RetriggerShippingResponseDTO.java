package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for POST /api/order/{orderId}/retrigger-shipping — used by the admin UI to
 * manually re-run the Shiprocket shipping process (find best courier → generate AWB →
 * request pickup → generate label → track shipment) for an order whose shipment(s)
 * previously failed or need manual intervention (e.g. status
 * MANUAL_PROCESSING_REQUIRED, or stuck without an AWB).
 *
 * <p>
 * If a Shiprocket order was already created for a shipment (shipOrderId present), the
 * retrigger resumes from the courier-selection step onwards instead of creating a
 * duplicate Shiprocket order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetriggerShippingResponseDTO {

	private String responseStatus;

	private String responseMessage;

	private Long orderId;

	private String orderNumber;

	private List<ShipmentRetriggerResultDTO> results;

}

