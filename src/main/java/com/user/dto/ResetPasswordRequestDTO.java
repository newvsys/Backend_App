package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequestDTO {

	private String phoneNo; // User's 10-digit mobile number (required if using mobile)

	private String email; // User's email (required if using email)

	private String otp; // The OTP code received by the user

	private String newPassword; // The new password to set

}
