package com.user.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "return_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequestEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "return_id", unique = true, nullable = false, length = 50)
	private String returnId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "order_id")
	private OrderEO order;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEO user;

	@Column(name = "return_type", nullable = false, length = 20)
	private String returnType;// RETURN / EXCHANGE

	@Column(name = "reason_code", nullable = false, length = 50)
	private String reasonCode;

	@Column(name = "status", nullable = false, length = 30)
	private String status;

	@Column(name = "user_comments", length = 250)
	private String userComments;

	// Reverse Pickup Details
	@Column(name = "carrier", length = 100)
	private String carrier;

	@Column(name = "reverse_tracking_number", length = 100)
	private String reverseTrackingNumber;

	@Column(name = "pickup_scheduled_date")
	private LocalDate pickupScheduledDate;

	@Column(name = "pickup_completed_date")
	private LocalDate pickupCompletedDate;

	@Column(name = "warehouse_received_date")
	private LocalDate warehouseReceivedDate;

	// QC Details
	@Column(name = "qc_status", length = 30)
	private String qcStatus;

	@Column(name = "qc_remarks", length = 255)
	private String qcRemarks;

	@Column(name = "inspected_at")
	private LocalDateTime inspectedAt;

	// Refund Details
	@Column(name = "refund_amount", precision = 10, scale = 2)
	private BigDecimal refundAmount;

	@Column(name = "payment_id", length = 100)
	private String paymentId;

	@Column(name = "refund_id", length = 100)
	private String refundId;

}
