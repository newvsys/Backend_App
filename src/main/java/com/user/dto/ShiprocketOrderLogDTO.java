package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single row from the {@code shiprocket_order_log} table – a step-by-step
 * audit trail (CREATE_ORDER, GENERATE_AWB, REQUEST_PICKUP, GENERATE_LABEL, etc.) of the
 * Shiprocket integration for a given shipment/order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiprocketOrderLogDTO {

	private Long id;

	private Long shipmentId;

	private Long orderId;

	private Long warehouseId;

	/** CREATE_ORDER, GENERATE_AWB, REQUEST_PICKUP, GENERATE_LABEL, SELECT_CARTON, etc. */
	private String step;

	/** SUCCESS | FAILED */
	private String status;

	private Integer shiprocketOrderId;

	private Integer shiprocketShipmentId;

	private String awbCode;

	private String labelUrl;

	private String errorMessage;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}

