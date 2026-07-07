package com.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_store")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OTPStoreEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Mobile number or email
	@Column(nullable = false, length = 100)
	private String identifier;

	// REGISTRATION / LOGIN / RESET_PASSWORD
	@Column(nullable = false, length = 50)
	private String purpose;

	// BCrypt/Argon2 hashed OTP
	@Column(name = "otp_hash", nullable = false, length = 255)
	private String otpHash;

	@Column(name = "verification_token", length = 255)
	private String verificationToken;

	// Failed attempts count
	@Column(nullable = false)
	private Integer attempts = 0;

	// ACTIVE / VERIFIED / EXPIRED / BLOCKED

	@Column(nullable = false, length = 20)
	private String status;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

	// Fraud tracking
	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "device_id", length = 700)
	private String deviceId;

	@PrePersist
	protected void onCreate() {
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
	}

}
