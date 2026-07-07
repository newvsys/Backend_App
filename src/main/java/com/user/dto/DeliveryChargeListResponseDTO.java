package com.user.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryChargeListResponseDTO {

	private String responseStatus;

	private String responseMessage;

	private List<DeliveryChargeResponseDTO> deliveryCharges;

}
