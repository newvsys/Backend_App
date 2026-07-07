package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO used to create an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipStatusUpdateRequestDTO {

	private String trackingNumber;

	private String status;

	private String location;

	private String remarks;

	private LocalDateTime eventTime;

}
