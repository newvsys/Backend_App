package com.user.communication.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundInitiatedEvent {

	private Long orderId;

	private Long cancelRequestId;

	private String refundReference;

	private BigDecimal amount;

	private String currency;

}
