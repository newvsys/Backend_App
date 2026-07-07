package com.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "shiprocket_order_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiprocketOrderLogEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "shipment_id")
	private Long shipmentId;

	@Column(name = "order_id")
	private Long orderId;

	@Column(name = "warehouse_id")
	private Long warehouseId;

	// Step: CREATE_ORDER, GENERATE_AWB, REQUEST_PICKUP, GENERATE_LABEL
	@Column(name = "step", length = 50)
	private String step;

	// Status: SUCCESS, FAILED
	@Column(name = "status", length = 20)
	private String status;

	@Column(name = "shiprocket_order_id")
	private Integer shiprocketOrderId;

	@Column(name = "shiprocket_shipment_id")
	private Integer shiprocketShipmentId;

	@Column(name = "awb_code", length = 100)
	private String awbCode;

	@Column(name = "label_url", length = 500)
	private String labelUrl;

	@Column(name = "error_message", length = 1000)
	private String errorMessage;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

}
