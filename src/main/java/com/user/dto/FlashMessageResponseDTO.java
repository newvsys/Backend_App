package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO representing a single flash / marquee message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashMessageResponseDTO {

	private Long id;

	private String title;

	private String message;

	private String type;

	private String bgColor;

	private String textColor;

	private String speed;

	private Integer priority;

	private String linkUrl;

	private LocalDateTime startDate;

	private LocalDateTime endDate;

	/** 'A' = Active, 'I' = Inactive. */
	private String status;

	private String createdBy;

	private String updatedBy;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}

