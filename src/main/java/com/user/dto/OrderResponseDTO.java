package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {

	private String orderNumber;

	private BigDecimal subtotalAmount;

	private BigDecimal shippingFee;

	private Boolean isFreeDelivery;

	private BigDecimal amount; // grand total (subtotal + shippingFee) — passed to payment
								// gateway

	private String currency;

	private String storeName;

	private String description;

	private String paymentOrderId;

	private String paymentGatewayKey;

	private String message;

	private String status;

}