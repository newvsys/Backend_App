package com.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a flash / marquee message displayed on the website. Multiple messages can
 * coexist; they are ordered by priority (lower number = higher priority). Status 'A' =
 * Active (visible), 'I' = Inactive (hidden).
 */
@Entity
@Table(name = "flash_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashMessageEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * Short title / label for internal reference (not shown in the marquee).
	 */
	@Column(name = "title", length = 200)
	private String title;

	/**
	 * The actual text content displayed in the website marquee.
	 */
	@Column(name = "message", columnDefinition = "TEXT", nullable = false)
	private String message;

	/**
	 * Category tag: e.g. INFO, OFFER, WARNING, NEWS. Useful for front-end styling.
	 */
	@Column(name = "type", length = 50)
	private String type;

	/**
	 * Background colour for the marquee banner (e.g. "#FF5733" or "red"). Optional –
	 * front-end can fall back to its default.
	 */
	@Column(name = "bg_color", length = 20)
	private String bgColor;

	/**
	 * Text / font colour for the marquee (e.g. "#FFFFFF"). Optional.
	 */
	@Column(name = "text_color", length = 20)
	private String textColor;

	/**
	 * Scroll speed hint for the front-end marquee component (e.g. "slow", "normal",
	 * "fast", or a numeric pixel-per-second value).
	 */
	@Column(name = "speed", length = 20)
	private String speed;

	/**
	 * Display order. Lower number = shown first when multiple active messages exist.
	 */
	@Column(name = "priority", nullable = false)
	private Integer priority;

	/**
	 * Optional URL that the marquee text links to.
	 */
	@Column(name = "link_url", length = 500)
	private String linkUrl;

	/**
	 * Date-time from which this message should start being displayed. Null = no
	 * restriction.
	 */
	@Column(name = "start_date")
	private LocalDateTime startDate;

	/**
	 * Date-time after which this message should stop being displayed. Null = no
	 * restriction.
	 */
	@Column(name = "end_date")
	private LocalDateTime endDate;

	/**
	 * 'A' = Active (visible on website), 'I' = Inactive (hidden).
	 */
	@Column(name = "status", nullable = false, length = 1)
	private String status;

	@Column(name = "created_by", length = 100)
	private String createdBy;

	@Column(name = "updated_by", length = 100)
	private String updatedBy;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
		if (status == null) {
			status = "A";
		}
		if (priority == null) {
			priority = 100;
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

}

