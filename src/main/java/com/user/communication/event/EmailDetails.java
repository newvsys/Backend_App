package com.user.communication.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailDetails {

	private Long shipmentId;

	private String orderId; // holds orderNumber e.g. "ORD-260317111721-000031"

	private Long warehouseId;

	// Shared: {{customer_name}}
	private String customerName;

	// Order Status Update template
	private String orderStatus;

	private String trackingNumber;

	private String expectedDelivery;

	private String trackingUrl;

	// Order Cancel / Return template: {{update_type}}
	private String updateType;

	// Order Cancel / Return template: {{status}}
	private String status;

	// Order Cancel / Return template: {{amount}}
	private String amount;

	// Order Cancel / Return template: {{message}}
	private String message;

	// Order Cancel / Return template: {{refund_days}}
	private String refundDays;

}
