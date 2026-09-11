package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShipmentDetailDTO {

	private Integer orderId;

	private String orderNumber;

	private String orderStatus;

	private String paymentStatus;

	private String currency;

	private BigDecimal subtotalAmount;

	private BigDecimal taxAmount;

	private BigDecimal shippingFee;

	private BigDecimal discountAmount;

	private BigDecimal totalAmount;

	private LocalDateTime orderCreatedAt;

	// Customer info
	private String customerName;

	private String customerEmail;

	private String customerPhone;

	// Addresses
	private OrderAddressDTO shippingAddress;

	private OrderAddressDTO billingAddress;

	/** Payment details (method, provider, transaction id, amount, status, time). */
	private PaymentInfoDTO payment;

	/** All shipments linked to this order. One order can have multiple shipments. */
	private List<ShipmentInfoDTO> shipments;

	/**
	 * Ready-to-use payload with all fields required to create a NEW Shiprocket order
	 * for this order (billing details, order items, dimensions/weight, payment
	 * method, etc.) — built from the order's current data.
	 */
	private ShiprocketOrderPayloadDTO shiprocketOrderPayload;

}
