package com.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_id")
	private Integer orderId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "customer_id")
	private CustomerEO customer;

	@Column(name = "order_number", unique = true)
	private String orderNumber;

	@Column(name = "order_status")
	private String orderStatus;

	@Column(name = "payment_status")
	private String paymentStatus;

	@Column(name = "currency")
	private String currency;

	@Column(name = "subtotal_amount")
	private BigDecimal subtotalAmount;

	@Column(name = "tax_amount")
	private BigDecimal taxAmount;

	@Column(name = "shipping_fee")
	private BigDecimal shippingFee;

	@Column(name = "discount_amount")
	private BigDecimal discountAmount;

	@Column(name = "total_amount")
	private BigDecimal totalAmount;

	@Column(name = "created_at")
	private java.time.LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = java.time.LocalDateTime.now();
		}
	}

}
