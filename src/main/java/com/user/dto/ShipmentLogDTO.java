package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight DTO for shipment audit logs from shiprocket_order_log table.
 * Contains the complete audit trail of all Shiprocket automation steps
 * (CREATE_ORDER, GENERATE_AWB, REQUEST_PICKUP, GENERATE_LABEL, TRACK_SHIPMENT, etc.)
 * for a shipment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentLogDTO {

	private Long id;

	private Long shipmentId;

	private Long orderId;

	private Long warehouseId;

	private String step;

	private String status;

	private Integer shiprocketOrderId;

	private Integer shiprocketShipmentId;

	private String awbCode;

	private String labelUrl;

	private String errorMessage;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}

