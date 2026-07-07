package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

	private Integer id;

	private String email;

	private String role;

	private String phone;

	private String firstName;

	private String lastName;

	private String password;

	private String passwordHash;

	private Boolean isActive = true;

	private String locale = "en";

	private OffsetDateTime emailVerifiedAt;

	private OffsetDateTime phoneVerifiedAt;

	private String metadata;

	private OffsetDateTime createdAt = OffsetDateTime.now();

	private OffsetDateTime updatedAt = OffsetDateTime.now();

}
