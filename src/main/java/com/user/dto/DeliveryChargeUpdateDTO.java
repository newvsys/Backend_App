package com.user.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryChargeUpdateDTO {

	private String ruleName;

	private BigDecimal minOrderAmount;

	private BigDecimal maxOrderAmount;

	private BigDecimal deliveryCharge;

	private Integer priority;

	private String description;

	private String status;

	private String updatedBy;

}
