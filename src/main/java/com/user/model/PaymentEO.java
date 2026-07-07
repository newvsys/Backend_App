package com.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "payment_id")
	private Integer paymentId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "order_id")
	private OrderEO order;

	@Column(name = "payment_method")
	private String paymentMethod;

	@Column(name = "payment_provider")
	private String paymentProvider;

	@Column(name = "payment_provider_order_id")
	private String paymentProviderOrderId;

	@Column(name = "transaction_id")
	private String transactionId;

	@Column(name = "amount")
	private BigDecimal amount;

	@Column(name = "payment_status")
	private String paymentStatus;

	@Column(name = "payment_time")
	private LocalDateTime paymentTime;

}
