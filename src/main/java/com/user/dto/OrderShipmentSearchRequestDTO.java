package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShipmentSearchRequestDTO {

	/** Filter by order status (e.g. PENDING, PROCESSING, SHIPPED, DELIVERED). */
	private String orderStatus;

	/** Filter orders created on or after this date-time. */
	private LocalDateTime orderCreatedFrom;

	/** Filter orders created on or before this date-time. */
	private LocalDateTime orderCreatedTo;

	/** Filter by exact order number. */
	private String orderNumber;

	/** Filter by shipment tracking number (AWB / courier tracking number). */
	private String shipmentNumber;

}
