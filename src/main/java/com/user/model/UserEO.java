package com.user.model;

import com.user.utility.Constants;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "email", length = 100)
	private String email;

	@Column(nullable = false)
	private String role;

	@Column(length = 32, nullable = false, unique = true)
	private String phone;

	@Column(name = "first_name", length = 100)
	private String firstName;

	@Column(name = "password_hash")
	private String passwordHash;

	@Column(name = "status")
	private String status = Constants.STATUS_ACTIVE; // Read status from constant

	@Column(name = "last_login_at")
	private OffsetDateTime lastLoginAt;

	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "phone_verified_at")
	private OffsetDateTime phoneVerifiedAt;

	@Column(name = "phone_verified", length = 10)
	private String phoneVerified;

	// ---------------- Relationships ----------------

}
