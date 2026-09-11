package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight DTO for status history audit logs. Used in responses to represent
 * the execution history of a single Shiprocket automation step for a shipment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusHistoryLogDTO {

	private Long id;

	private String status;

	private String remarks;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}

