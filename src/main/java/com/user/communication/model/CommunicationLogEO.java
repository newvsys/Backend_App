package com.user.communication.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "communication_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunicationLogEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "purpose", length = 50)
	private String purpose;

	@Column(name = "channel", length = 20)
	private String channel;

	@Column(name = "recipient", length = 150)
	private String recipient;

	@Column(name = "message", columnDefinition = "TEXT")
	private String message;

	@Column(name = "status", length = 20)
	private String status;

	@Column(name = "retry_count")
	private Integer retryCount = 0;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

}
