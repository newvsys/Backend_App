package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryPaymentRequestDTO {

	/** Internal DB order ID (as string, e.g. "28"). */
	private String orderId;

	/** Human-readable order number (e.g. "ORD-260608121943-000029"). */
	private String orderNumber;

}
