package com.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Stores Firebase Cloud Messaging (FCM) device/browser registration tokens used to push
 * notifications to the Admin web app (e.g. "New order received").
 */
@Entity
@Table(name = "device_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceTokenEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "token", length = 512, nullable = false, unique = true)
	private String token;

	// Optional link to the admin user that registered this token.
	@Column(name = "user_id")
	private Long userId;

	// Role of the user this token belongs to, e.g. "admin". Used to target
	// notifications without requiring a user join (e.g. multiple admins/devices).
	@Column(name = "role", length = 32)
	private String role;

	// Platform/browser info, e.g. WEB.
	@Column(name = "platform", length = 32)
	private String platform;

	@Column(name = "active", nullable = false)
	private Boolean active;

	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

}

