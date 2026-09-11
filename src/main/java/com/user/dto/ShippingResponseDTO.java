package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response payload for {@code processCreateShipmentEvent} (both the internal
 * self-service call and the {@code POST /api/shipment/create} REST endpoint).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingResponseDTO {

	private String responseStatus;

	private String responseMessage;

	private Long orderId;

	/** Ids of the ShippingEO records created during this call (one per warehouse). */
	private List<Long> shipmentIds;

}

