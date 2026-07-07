package com.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundTransactionEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@Column(name = "order_item_id")
	private Long orderItemId;

	@Column(name = "return_id")
	private Long returnId;

	@Column(name = "cancel_request_id")
	private Long cancelRequestId;

	@Column(name = "payment_transaction_id", nullable = false)
	private Long paymentTransactionId;

	@Column(name = "refund_reference", length = 100)
	private String refundReference;

	@Column(name = "gateway_refund_id", length = 150)
	private String gatewayRefundId;

	@Column(name = "refund_type", length = 30, nullable = false)
	private String refundType; // FULL / PARTIAL

	@Column(name = "refund_reason", length = 255)
	private String refundReason;

	@Column(name = "requested_amount", precision = 12, scale = 2, nullable = false)
	private BigDecimal requestedAmount;

	@Column(name = "approved_amount", precision = 12, scale = 2)
	private BigDecimal approvedAmount;

	@Column(name = "refunded_amount", precision = 12, scale = 2)
	private BigDecimal refundedAmount;

	@Column(name = "currency", length = 10)
	private String currency = "INR";

	@Column(name = "status", length = 30, nullable = false)
	private String status;

	@Column(name = "failure_reason", columnDefinition = "TEXT")
	private String failureReason;

	@Column(name = "retry_count")
	private Integer retryCount = 0;

	@Column(name = "initiated_at")
	private LocalDateTime initiatedAt;

	@Column(name = "processed_at")
	private LocalDateTime processedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}
