package com.user.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryChargeResponseDTO {

	private Long id;

	private String ruleName;

	private BigDecimal minOrderAmount;

	private BigDecimal maxOrderAmount;

	private BigDecimal deliveryCharge;

	private Boolean isFreeDelivery;

	private Integer priority;

	private String status;

	private String description;

	private String createdBy;

	private String updatedBy;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
