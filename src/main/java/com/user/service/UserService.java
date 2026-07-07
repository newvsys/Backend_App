package com.user.service;

import com.user.dto.*;
import java.util.List;

public interface UserService {

	ResponseDTO registerUser(UserCreateDTO user);

	AuthResponseDto login(String email, String password);

	/**
	 * Verifies the OTP sent with purpose "LOGIN" and, if valid, returns the authenticated
	 * user's details exactly like the password-based login.
	 */
	AuthResponseDto loginWithOtp(String phone, String otp);

	/**
	 * Sends an OTP for login after confirming the phone belongs to an active registered
	 * user. Returns a ResponseDTO indicating success or the reason for failure.
	 */
	ResponseDTO sendOtpForLogin(String phone);

	ResponseDTO resetPassword(ResetPasswordRequestDTO resetPasswordRequestDTO);

	ResponseDTO updateCustomer(CustomerDTO customerUpdateDTO);

	CustomerDTO getCustomerDetailsByUserId(Long userId);

	// ── Admin APIs ────────────────────────────────────────────────
	/**
	 * Paginated list of all users + their customer details, with optional search & status
	 * filter
	 */
	AdminUserListResponseDTO getAllUsersForAdmin(String search, String status, int page, int size);

	/** Full user + customer details by userId */
	AdminUserDTO getUserDetailsByIdForAdmin(Long userId);

	/** Full customer details by customerId */
	AdminUserDTO getCustomerDetailsByIdForAdmin(Integer customerId);

	/** Paginated list of customers with optional search & status filter */
	AdminUserListResponseDTO getAllCustomersForAdmin(String search, String status, int page, int size);

	/** Activate or deactivate a user */
	ResponseDTO updateUserStatus(Long userId, String status);

}
