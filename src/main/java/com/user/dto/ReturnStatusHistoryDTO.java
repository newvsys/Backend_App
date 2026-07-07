package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnStatusHistoryDTO {

	private Long id;

	private String newStatus;

	private String activityType;

	private String remarks;

	private String changedBy;

	private LocalDateTime changedAt;

}
