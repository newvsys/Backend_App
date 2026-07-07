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
public class OrderStatusDTO {

	private String orderNumber;

	private String status;

	// ── Payment Details ─────────────────────────────────────────────────────
	private String paymentStatus;

	private String transactionId;

	private String paymentMethod;

	private LocalDateTime paymentTime;

	private BigDecimal total;

	private String currency;

	// ── Customer / Shipping Address ─────────────────────────────────────────
	private String name;

	private String address1;

	private String address2;

	private String landmark;

	private String city;

	private String state;

	private String country;

	private String postalCode;

	/**
	 * Single-line truncated address for display (e.g. "12, MG Road, Bangalore, Karnataka
	 * 560001").
	 */
	private String deliveryAddressSummary;

	// ── Delivery / Shipment Info ────────────────────────────────────────────
	/**
	 * Formatted estimated delivery range, e.g. "10–12 Jun 2026". Null if shipment not yet
	 * created.
	 */
	private String estimatedDelivery;

	/** Shiprocket / courier tracking page URL. Null if shipment not yet created. */
	private String trackOrderUrl;

	/** AWB / courier tracking number. Null if shipment not yet created. */
	private String awbCode;

	// ── Order Items ─────────────────────────────────────────────────────────
	private List<OrderStatusProd> products;

}
