package com.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserDTO {

	// ── User fields ──────────────────────────────────────────────
	private Long userId;

	private String email;

	private String phone;

	private String firstName;

	private String role;

	private String status;

	private String phoneVerified;

	private OffsetDateTime phoneVerifiedAt;

	private OffsetDateTime userCreatedAt;

	private OffsetDateTime userUpdatedAt;

	private OffsetDateTime lastLoginAt;

	// ── Customer fields ──────────────────────────────────────────
	private Integer customerId;

	private String customerFirstName;

	private String customerLastName;

	private String customerEmail;

	private String mobileNumber;

	private String customerType; // GUEST / REGISTERED

	private String customerStatus; // ACTIVE / INACTIVE

	private OffsetDateTime customerCreatedAt;

	private OffsetDateTime customerUpdatedAt;

	// ── Addresses ────────────────────────────────────────────────
	private List<CustomerAddressDTO> addresses;

}
