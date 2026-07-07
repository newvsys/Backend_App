package com.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_cancel_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancelRequestEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "order_id")
	private OrderEO order;

	@Column(name = "reason")
	private String reason;

	@Column(name = "comment")
	private String comment;

	@Column(name = "status")
	private String status; // REQUESTED, APPROVED, REJECTED

	@Column(name = "requested_at")
	private LocalDateTime requestedAt;

	@Column(name = "processed_at")
	private LocalDateTime processedAt;

	// Shiprocket cancel order response fields
	@Column(name = "shiprocket_cancel_status")
	private String shiprocketCancelStatus; // e.g. "success" / "failed"

	@Column(name = "shiprocket_cancel_message", length = 512)
	private String shiprocketCancelMessage; // e.g. "1 order(s) cancelled successfully"

	@Column(name = "shiprocket_cancelled_at")
	private LocalDateTime shiprocketCancelledAt;

}
