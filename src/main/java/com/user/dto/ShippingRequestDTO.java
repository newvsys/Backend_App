package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for manually triggering shipment creation for an order via
 * {@code POST /api/shipment/create}. Mirrors {@link com.user.communication.event.OrderEvent}
 * which is normally published internally (e.g. after payment confirmation) to trigger
 * {@code ShippingServiceImpl.processCreateShipmentEvent}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingRequestDTO {

	private Long orderId;

	/** Optional. Carton number/identifier to use for this shipment, if known upfront. */
	private String cartonNo;

	/** Optional. Details for creating a new carton alongside this shipment request. */
	private RequestCreateCartonDTO requestCreateCartonDTO;

	/** Optional. Preferred/best courier company id to use for this shipment, if known upfront. */
	private Integer bestCourierId;

}

