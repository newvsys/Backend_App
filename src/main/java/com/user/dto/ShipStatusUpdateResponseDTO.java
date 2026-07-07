package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO used to create an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipStatusUpdateResponseDTO {

	private String status;

	private String statusMessage;

}
