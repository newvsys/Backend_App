package com.user.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * Response DTO for the "calculate delivery charge" endpoint. Returns the matched rule
 * plus the charge applicable for the given order amount.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryChargeCalculateResponseDTO {

	private String responseStatus;

	private String responseMessage;

	/** Order subtotal that was used for the calculation. */
	private BigDecimal orderAmount;

	/** Delivery charge to apply (0.00 if free). */
	private BigDecimal applicableDeliveryCharge;

	/** True when delivery is free. */
	private Boolean isFreeDelivery;

	/** The rule that was matched. */
	private DeliveryChargeResponseDTO matchedRule;

}
