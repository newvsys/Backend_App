package com.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_charges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryChargeEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * Human-readable label for this rule, e.g. "Free Delivery", "Standard Delivery".
	 */
	@Column(name = "rule_name", nullable = false)
	private String ruleName;

	/**
	 * Minimum cart/order subtotal (inclusive) for this rule to apply. Use 0 when there is
	 * no lower bound.
	 */
	@Column(name = "min_order_amount", nullable = false, precision = 10, scale = 2)
	private BigDecimal minOrderAmount;

	/**
	 * Maximum cart/order subtotal (inclusive) for this rule to apply. NULL means "no
	 * upper limit" (applies to all orders above minOrderAmount).
	 */
	@Column(name = "max_order_amount", precision = 10, scale = 2)
	private BigDecimal maxOrderAmount;

	/**
	 * Delivery charge to apply when this rule is matched. 0.00 means free delivery.
	 */
	@Column(name = "delivery_charge", nullable = false, precision = 10, scale = 2)
	private BigDecimal deliveryCharge;

	/**
	 * Convenience flag: true when deliveryCharge == 0. Helps UI display "FREE" badge
	 * without extra arithmetic.
	 */
	@Column(name = "is_free_delivery", nullable = false)
	private Boolean isFreeDelivery;

	/**
	 * Lower priority number = evaluated first when multiple rules could match.
	 */
	@Column(name = "priority", nullable = false)
	private Integer priority;

	/**
	 * 'A' = Active, 'I' = Inactive.
	 */
	@Column(name = "status", nullable = false, length = 1)
	private String status;

	@Column(name = "description")
	private String description;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
		if (status == null) {
			status = "A";
		}
		if (isFreeDelivery == null) {
			isFreeDelivery = (deliveryCharge != null && deliveryCharge.compareTo(BigDecimal.ZERO) == 0);
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
		if (deliveryCharge != null) {
			isFreeDelivery = (deliveryCharge.compareTo(BigDecimal.ZERO) == 0);
		}
	}

}
