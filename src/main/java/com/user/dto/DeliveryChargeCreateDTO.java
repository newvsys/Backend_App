package com.user.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryChargeCreateDTO {

	/**
	 * Human-readable rule label, e.g. "Free Delivery Above ₹500".
	 */
	private String ruleName;

	/**
	 * Minimum order subtotal for this rule to apply (inclusive). Defaults to 0.
	 */
	private BigDecimal minOrderAmount;

	/**
	 * Maximum order subtotal for this rule to apply (inclusive). Leave null for "no upper
	 * limit".
	 */
	private BigDecimal maxOrderAmount;

	/**
	 * Delivery fee charged. 0.00 = free delivery.
	 */
	private BigDecimal deliveryCharge;

	/**
	 * Evaluation priority. Lower number = evaluated first.
	 */
	private Integer priority;

	/** Optional human-readable description. */
	private String description;

	/** 'A' = Active, 'I' = Inactive. Defaults to 'A' if not supplied. */
	private String status;

	private String createdBy;

}
