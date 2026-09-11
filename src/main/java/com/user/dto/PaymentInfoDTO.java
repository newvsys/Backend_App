package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment details for an order, surfaced via the order-shipment-details admin API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfoDTO {

	private String paymentMethod;

	private String paymentProvider;

	private String transactionId;

	private BigDecimal amount;

	private String paymentStatus;

	private LocalDateTime paymentTime;

}

