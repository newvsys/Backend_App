package com.user.controller;

import com.user.dto.*;
import com.user.service.OtpService;
import com.user.service.UserService;
import com.user.utility.Constants;
import com.user.utility.PhoneUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

	private static final Logger logger = LogManager.getLogger(UserController.class);

	@Autowired
	private UserService userService;

	@Autowired
	private OtpService otpService;

	@PostMapping("/register")
	public ResponseEntity<ResponseDTO> registerUser(@RequestBody UserCreateDTO user) {
		logger.info("Received registerUser request for phone: {}", user.getPhone());
		ResponseDTO response = new ResponseDTO();
		// Validate phone is not empty
		if (PhoneUtil.isEmpty(user.getPhone())) {
			logger.warn("Phone number is empty for registration request");
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.PHONE_REQUIRED);
			return ResponseEntity.ok(response);
		}
		// Validate phone number format (10 digits, numeric)
		if (!PhoneUtil.isValid(user.getPhone())) {
			logger.warn("Invalid phone number format: {}", user.getPhone());
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.PHONE_INVALID);
			return ResponseEntity.ok(response);
		}
		try {
			logger.debug("Calling userService.registerUser for Phone: {}", user.getPhone());
			ResponseDTO registeredUser = userService.registerUser(user);
			logger.info("User registration successful for Phone: {}", user.getPhone());
			return ResponseEntity.ok(registeredUser);
		}
		catch (Exception e) {
			logger.error("User registration failed for Phone: {}. Error: {}", user.getPhone(), e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.TECHNICAL_ERROR_REGISTER);
			return ResponseEntity.status(500).body(response);
		}
	}

	@PostMapping("/generate-otp")
	public ResponseEntity<ResponseDTO> generateOtp(@RequestBody OtpRequestDTO request) {
		ResponseDTO response = new ResponseDTO();
		logger.info("Received generateOtp request for phone: {}", request.getPhone());
		if (request == null || PhoneUtil.isEmpty(request.getPhone())) {
			logger.error("generateOtp request received with empty or invalid phone");
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.PHONE_REQUIRED);
			return ResponseEntity.ok(response);
		}

		if (!PhoneUtil.isValid(request.getPhone())) {
			logger.error("generateOtp request received with invalid phone: {}", request.getPhone());
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.PHONE_INVALID);
			return ResponseEntity.ok(response);
		}

		try {
			logger.debug("Calling otpService.generateOtp for phone: {}", request.getPhone());
			response = otpService.generateOtp(request);
			logger.info("OTP generated successfully for phone: {}", request.getPhone());
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("OTP generation failed for phone: {}. Error: {}", request.getPhone(), e.getMessage(), e);

			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.TECHNICAL_ERROR_OTP_GENERATION);
			return ResponseEntity.status(500).body(response);
		}
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<ResponseDTO> verifyOtp(@RequestBody OtpVerifyRequestDTO request) {
		ResponseDTO response = new ResponseDTO();

		if (request == null || PhoneUtil.isEmpty(request.getPhone())) {
			logger.error("verifyOtp request received with empty phone");
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.PHONE_REQUIRED);
			return ResponseEntity.ok(response);
		}

		if (!PhoneUtil.isValid(request.getPhone())) {
			logger.error("verifyOtp request received with invalid phone: {}", request.getPhone());
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.PHONE_INVALID);
			return ResponseEntity.ok(response);
		}

		logger.info("Received verifyOtp request for phone: {}", request.getPhone());
		try {
			logger.debug("Calling otpService.verifyOtp for phone: {}", request.getPhone());
			response = otpService.verifyOtp(request);
			logger.info("OTP verification result for phone: {}: {}", request.getPhone(), response.getResponseStatus());
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("OTP verification failed for phone: {}. Error: {}", request.getPhone(), e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.TECHNICAL_ERROR_OTP_VERIFICATION);
			return ResponseEntity.status(500).body(response);
		}
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto loginRequest) {
		// ...existing code...
		AuthResponseDto response = new AuthResponseDto();

		if (loginRequest == null) {
			logger.error("Received login request with null body");
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage(Constants.LOGIN_INVALID_CREDENTIAL);
			return ResponseEntity.ok(response);
		}

		if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
			logger.error("Received login request with empty email");
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage(Constants.LOGIN_INVALID_CREDENTIAL);
			return ResponseEntity.ok(response);
		}

		if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
			logger.warn("Received login request with empty password for email: {}", loginRequest.getEmail());
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage(Constants.LOGIN_INVALID_CREDENTIAL);
			return ResponseEntity.ok(response);
		}

		logger.info("Received login request for email: {}", loginRequest.getEmail());
		try {
			logger.debug("Calling userService.findByNameEmailPhoneNo for email: {}", loginRequest.getEmail());
			response = userService.login(loginRequest.getEmail(), loginRequest.getPassword());
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Login failed for email: {}. Error: {}", loginRequest.getEmail(), e.getMessage(), e);
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage(Constants.TECHNICAL_ERROR_LOGIN);
			return ResponseEntity.ok(response);
		}
	}

	// ═══════════════════════════════════════════════════════════════════
	// OTP LOGIN APIs
	// ═══════════════════════════════════════════════════════════════════

	/**
	 * Step 1 of OTP login: validate the phone and send an OTP if an active registered
	 * user is found.
	 *
	 * POST /user/login-otp/send
	 *
	 * Request body: { "phone": "9876543210" }
	 */
	@PostMapping("/login-otp/send")
	public ResponseEntity<ResponseDTO> sendLoginOtp(@RequestBody OtpRequestDTO request) {
		ResponseDTO response = new ResponseDTO();
		logger.info("Received sendLoginOtp request for phone: {}", request != null ? request.getPhone() : null);

		if (request == null || PhoneUtil.isEmpty(request.getPhone())) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.PHONE_REQUIRED);
			return ResponseEntity.ok(response);
		}

		if (!PhoneUtil.isValid(request.getPhone())) {
			logger.warn("sendLoginOtp: invalid phone format: {}", request.getPhone());
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.PHONE_INVALID);
			return ResponseEntity.ok(response);
		}

		try {
			response = userService.sendOtpForLogin(request.getPhone());
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("sendLoginOtp failed for phone: {}. Error: {}", request.getPhone(), e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.TECHNICAL_ERROR_OTP_GENERATION);
			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * Step 2 of OTP login: verify the OTP and return auth details (same shape as
	 * password-based login).
	 *
	 * POST /user/login-otp/verify
	 *
	 * Request body: { "phone": "9876543210", "otp": "123456" }
	 */
	@PostMapping("/login-otp/verify")
	public ResponseEntity<AuthResponseDto> verifyLoginOtp(@RequestBody OtpVerifyRequestDTO request) {
		AuthResponseDto response = new AuthResponseDto();
		logger.info("Received verifyLoginOtp request for phone: {}", request != null ? request.getPhone() : null);

		if (request == null || PhoneUtil.isEmpty(request.getPhone())) {
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage(Constants.PHONE_REQUIRED);
			return ResponseEntity.ok(response);
		}

		if (!PhoneUtil.isValid(request.getPhone())) {
			logger.warn("verifyLoginOtp: invalid phone format: {}", request.getPhone());
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage(Constants.PHONE_INVALID);
			return ResponseEntity.ok(response);
		}

		if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
			logger.warn("verifyLoginOtp: OTP is empty for phone: {}", request.getPhone());
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage("OTP is required");
			return ResponseEntity.ok(response);
		}

		try {
			response = userService.loginWithOtp(request.getPhone(), request.getOtp());
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("verifyLoginOtp failed for phone: {}. Error: {}", request.getPhone(), e.getMessage(), e);
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage(Constants.TECHNICAL_ERROR_LOGIN);
			return ResponseEntity.status(500).body(response);
		}
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ResponseDTO> resetPassword(@RequestBody ResetPasswordRequestDTO request) {
		ResponseDTO response = new ResponseDTO();

		if (request == null) {
			logger.warn("resetPassword request received as null");
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.TECHNICAL_ERROR_RESETPASSWORD);
			return ResponseEntity.ok(response);
		}

		if ((request.getEmail() == null || request.getEmail().trim().isEmpty())
				&& (request.getPhoneNo() == null || PhoneUtil.isEmpty(request.getPhoneNo()))) {
			logger.warn("resetPassword request received with empty email and mobile no");
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Email or Mobile No is required");
			return ResponseEntity.ok(response);
		}

		if (request.getPhoneNo() != null) {
			if (!PhoneUtil.isValid(request.getPhoneNo())) {
				logger.warn("resetPassword request received with invalid phone: {} for email: {}", request.getPhoneNo(),
						request.getEmail());
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage(Constants.PHONE_INVALID);
				return ResponseEntity.ok(response);
			}
		}
		logger.info("Received resetPassword request for email: {}", request.getEmail());
		try {
			logger.debug("Calling userService.resetPassword for email: {}", request.getEmail());
			ResponseDTO responseDTO = userService.resetPassword(request);
			logger.info("Password reset successful for email: {}", request.getEmail());
			return ResponseEntity.ok(responseDTO);
		}
		catch (Exception e) {
			logger.error("Password reset failed for email: {}. Error: {}", request.getEmail(), e.getMessage(), e);
			ResponseDTO errorResponse = new ResponseDTO();
			errorResponse.setResponseStatus(Constants.FAILURE_STATUS);
			errorResponse.setResponseMessage(Constants.TECHNICAL_ERROR_RESETPASSWORD);
			return ResponseEntity.ok(errorResponse);
		}
	}

	@Operation(summary = "Update customer and address details")
	@PostMapping("/update-customer")
	public ResponseEntity<ResponseDTO> updateCustomer(@RequestBody CustomerDTO request) {
		logger.info("Received updateCustomer request for userId: {}", request.getUserId());
		ResponseDTO responseDTO = new ResponseDTO();
		try {
			logger.debug("Calling userService.updateCustomer for userId: {}", request.getUserId());
			responseDTO = userService.updateCustomer(request);
			logger.info("Customer update successful for userId: {}", request.getUserId());
			return ResponseEntity.ok(responseDTO);
		}
		catch (Exception e) {
			logger.error("Customer update failed for userId: {}. Error: {}", request.getUserId(), e.getMessage(), e);
			responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
			responseDTO
				.setResponseMessage("An unexpected error occurred during customer update. Please try again later.");
			return ResponseEntity.ok(responseDTO);
		}
	}

	@Operation(summary = "Get customer and address details by userId")
	@GetMapping("/get-customer-details")
	public ResponseEntity<?> getCustomerDetails(@RequestParam("userId") Long userId) {
		if (userId == null || userId <= 0) {
			return ResponseEntity.badRequest()
				.body(Map.of("status", "failed", "message", "userId must be a positive number"));
		}
		logger.info("Received getCustomerDetails request for userId: {}", userId);
		try {
			CustomerDTO customer = userService.getCustomerDetailsByUserId(userId);
			if (customer == null) {
				return ResponseEntity.status(404)
					.body(Map.of("status", "failed", "message", "No active customer found for userId: " + userId));
			}
			logger.info("Fetched customer details for userId: {}", userId);
			return ResponseEntity.ok(customer);
		}
		catch (Exception e) {
			logger.error("Fetching customer details failed for userId: {}. Error: {}", userId, e.getMessage(), e);
			throw e;
		}
	}

	// ═══════════════════════════════════════════════════════════════════
	// ADMIN APIs
	// ═══════════════════════════════════════════════════════════════════

	@Operation(summary = "Admin: Get paginated list of all users with optional search and status filter")
	@GetMapping("/admin/users")
	public ResponseEntity<AdminUserListResponseDTO> adminGetAllUsers(
			@Parameter(description = "Search by name, email, or phone") @RequestParam(required = false) String search,
			@Parameter(description = "Filter by status: ACTIVE or INACTIVE") @RequestParam(
					required = false) String status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		logger.info("Admin: getAllUsers search={}, status={}, page={}, size={}", search, status, page, size);
		AdminUserListResponseDTO response = userService.getAllUsersForAdmin(search, status, page, size);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Admin: Get full user + customer details by userId")
	@GetMapping("/admin/users/{userId}")
	public ResponseEntity<AdminUserDTO> adminGetUserById(@PathVariable Long userId) {
		logger.info("Admin: getUserById userId={}", userId);
		AdminUserDTO dto = userService.getUserDetailsByIdForAdmin(userId);
		return ResponseEntity.ok(dto);
	}

	@Operation(summary = "Admin: Get paginated list of all customers with optional search and status filter")
	@GetMapping("/admin/customers")
	public ResponseEntity<AdminUserListResponseDTO> adminGetAllCustomers(
			@Parameter(description = "Search by name, email, or mobile number") @RequestParam(
					required = false) String search,
			@Parameter(description = "Filter by status: ACTIVE or INACTIVE") @RequestParam(
					required = false) String status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		logger.info("Admin: getAllCustomers search={}, status={}, page={}, size={}", search, status, page, size);
		AdminUserListResponseDTO response = userService.getAllCustomersForAdmin(search, status, page, size);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Admin: Get full customer details by customerId")
	@GetMapping("/admin/customers/{customerId}")
	public ResponseEntity<AdminUserDTO> adminGetCustomerById(@PathVariable Integer customerId) {
		logger.info("Admin: getCustomerById customerId={}", customerId);
		AdminUserDTO dto = userService.getCustomerDetailsByIdForAdmin(customerId);
		return ResponseEntity.ok(dto);
	}

	@Operation(summary = "Admin: Activate or deactivate a user account")
	@PutMapping("/admin/users/{userId}/status")
	public ResponseEntity<ResponseDTO> adminUpdateUserStatus(@PathVariable Long userId, @RequestParam String status) {
		logger.info("Admin: updateUserStatus userId={}, status={}", userId, status);
		ResponseDTO response = userService.updateUserStatus(userId, status);
		return ResponseEntity.ok(response);
	}

}
