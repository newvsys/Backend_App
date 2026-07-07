package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for creating or updating a flash / marquee message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashMessageCreateDTO {

	/** Short internal title / label (not shown in the marquee). */
	private String title;

	/** The marquee text content (required). */
	private String message;

	/**
	 * Category tag: INFO | OFFER | WARNING | NEWS etc. Used for front-end styling.
	 */
	private String type;

	/** Background colour for the banner (e.g. "#FF5733"). Optional. */
	private String bgColor;

	/** Text colour (e.g. "#FFFFFF"). Optional. */
	private String textColor;

	/** Scroll speed hint: "slow" | "normal" | "fast" or a numeric value. Optional. */
	private String speed;

	/** Display order – lower = higher priority. Defaults to 100. */
	private Integer priority;

	/** Optional URL the marquee text should link to. */
	private String linkUrl;

	/** Start date-time for displaying the message (null = no restriction). */
	private LocalDateTime startDate;

	/** End date-time for displaying the message (null = no restriction). */
	private LocalDateTime endDate;

	/** 'A' = Active, 'I' = Inactive. Defaults to 'A' on create. */
	private String status;

	/** The user/admin who is creating / updating this record. */
	private String updatedBy;

}

