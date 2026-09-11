package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response payload for the API that fetches all orders with status Confirmed or Ready to
 * Ship, whose linked shipment has at least one Shiprocket step status equal to FAILURE
 * (shiprocket_order_status, generate_awb_status, request_pickup_status,
 * generate_label_status, track_shipment_status, estimate_status).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedShiprocketOrdersResponseDTO {

	private String responseStatus;

	private String responseMessage;

	private int totalCount;

	private List<OrderShipmentFailureDTO> orders;

}

