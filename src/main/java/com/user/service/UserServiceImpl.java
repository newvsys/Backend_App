package com.user.service;

import com.user.dto.*;
import com.user.model.*;
import com.user.repository.*;
import com.user.utility.Constants;
import com.user.utility.ObjectMapper;
import com.user.utility.PasswordUtil;
import com.user.utility.UserMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

	private static final Logger logger = LogManager.getLogger(UserServiceImpl.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OTPStoreRepository otpRepo;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private CustomerAddressRepository customerAddressRepository;

	@Autowired
	@Lazy
	private OtpService otpService;

	@Override
	public ResponseDTO registerUser(UserCreateDTO userdto) {
		logger.info("UserServiceImpl entry for registerUser phone : {}", userdto.getPhone());
		ResponseDTO response = new ResponseDTO();
		try {
			UserEO existingUser = userRepository.findByPhone(userdto.getPhone());
			if (existingUser != null && Constants.STATUS_ACTIVE.equals(existingUser.getStatus())) {
				logger.error("Active user already exists for phone: {}", userdto.getPhone());
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage(Constants.ACTIVE_USER_ALREADY_EXISTS);
				return response;
			}
			else if (existingUser != null && Constants.STATUS_INACTIVE.equals(existingUser.getStatus())) {
				logger.error("Deleting inactive user for phone: {} and creating new User with this details",
						userdto.getPhone());
				userRepository.delete(existingUser);
			}
			// Create new user
			UserEO user = ObjectMapper.userCreateDTOToUserEO(userdto);
			UserEO savedUser = null;
			// validate mobile no verified or not
			List<OTPStoreEO> otpstoreEOList = otpRepo.findByIdentifierAndStatusAndPurpose(userdto.getPhone(),
					Constants.OTP_STATUS_VERIFIED, "Mobile No Verification");

			if (otpstoreEOList != null && !otpstoreEOList.isEmpty()) {
				OTPStoreEO otpstoreEO = otpstoreEOList.get(0);
				if (Constants.OTP_STATUS_VERIFIED.equals(otpstoreEO.getStatus())) {
					logger.info("Phone number verified for phone: {}", userdto.getPhone());
					user.setPhoneVerified(Constants.PHONE_VERIFIED_YES);

					if (otpstoreEO.getVerifiedAt() != null) {
						user.setPhoneVerifiedAt(otpstoreEO.getVerifiedAt().atOffset(java.time.ZoneOffset.UTC));
					}

					savedUser = userRepository.save(user);
					CustomerEO customer = new CustomerEO();
					customer.setUser(savedUser);
					if (userdto.getFirstName() != null)
						customer.setFirstName(userdto.getFirstName());
					if (userdto.getEmail() != null)
						customer.setEmail(userdto.getEmail());
					if (userdto.getPhone() != null)
						customer.setMobileNumber(userdto.getPhone());
					customer.setCustomerType(Constants.CUSTOMER_TYPE_GUEST);
					customer.setStatus(Constants.STATUS_ACTIVE);
					customer.setCreatedAt(java.time.OffsetDateTime.now());
					customer.setCreatedBy(userdto.getPhone());
					customerRepository.save(customer);
					logger.info("Associated customer record created for userId: {}", savedUser.getId());
				}
				else {
					logger.error("Mobile No not verified for phone: {}", userdto.getPhone());
					response.setResponseStatus(Constants.FAILURE_STATUS);
					response.setResponseMessage(Constants.MOBILE_NO_NOT_VERIFIED);
					return response;
				}
			}
			else {
				logger.error("Mobile No not verified for phone: {}", userdto.getPhone());
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage(Constants.MOBILE_NO_NOT_VERIFIED);
				return response;
			}

			logger.info("User created successfully for phone: {}", userdto.getPhone());
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage(Constants.USER_CREATED_MSG);
		}
		catch (Exception e) {
			logger.error("Error registering user for phone: {}. Exception: {}", userdto.getPhone(), e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.TECHNICAL_ERROR_REGISTER);
		}
		return response;
	}

	@Override
	public AuthResponseDto login(String value, String password) {
		// ...existing code...
		AuthResponseDto response = new AuthResponseDto();
		try {

			logger.info("Login users by name/email/phone: {}", value);
			List<UserEO> users = userRepository.findByFirstNameOrEmailOrPhone(value, value, value);
			if (users.size() > 1) {
				logger.error("Multiple users found for email: {}", value);
				response.setStatus(Constants.FAILURE_STATUS);
				response.setMessage(Constants.MULTIPLE_ACTIVE_USERS);

				return response;
			}
			else if (users.isEmpty()) {
				logger.error("No user found for email: {}", value);
				response.setStatus(Constants.FAILURE_STATUS);
				response.setMessage(Constants.NO_ACTIVE_USER);
				return response;
			}
			else {
				UserEO user = users.get(0);
				if (user != null && Objects.equals(user.getPasswordHash(), password)) {
					logger.info("Login successful for userId: {}", user.getId());
					response.setStatus(Constants.SUCCESS_STATUS);
					response.setMessage(Constants.LOGIN_SUCCESS);
					response.setId(user.getId());
					response.setEmail(user.getEmail());
					response.setRole(user.getRole());
					return response;
				}
				else {
					logger.warn("Invalid password attempt for identifier: {}", value);
					response.setStatus(Constants.FAILURE_STATUS);
					response.setMessage(Constants.INVALID_PASSWORD);
					return response;
				}

			}
		}
		catch (Exception e) {
			logger.error("Error occurred while login: ", e);
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage(Constants.TECHNICAL_ERROR_LOGIN);

		}
		return response;
	}

	// ─── OTP Login ────────────────────────────────────────────────────────────

	@Override
	public ResponseDTO sendOtpForLogin(String phone) {
		logger.info("sendOtpForLogin: checking if active user exists for phone={}", phone);
		ResponseDTO response = new ResponseDTO();
		try {
			// Verify an active user with this phone exists before sending OTP
			List<UserEO> users = userRepository.findByPhoneAndStatus(phone, Constants.STATUS_ACTIVE);
			if (users == null || users.isEmpty()) {
				logger.warn("sendOtpForLogin: no active user found for phone={}", phone);
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage(Constants.OTP_LOGIN_USER_NOT_FOUND);
				return response;
			}

			// Delegate OTP generation to OtpServiceImpl via the OTP store directly
			// (re-uses the same logic as /generate-otp with purpose = LOGIN)
			OtpRequestDTO otpRequest = OtpRequestDTO.builder()
				.phone(phone)
				.purpose(Constants.PURPOSE_LOGIN)
				.build();

			// Call generateOtp through the existing service bean
			return otpService.generateOtp(otpRequest);
		}
		catch (Exception e) {
			logger.error("sendOtpForLogin error for phone={}: {}", phone, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.TECHNICAL_ERROR_OTP_GENERATION);
			return response;
		}
	}

	@Override
	public AuthResponseDto loginWithOtp(String phone, String otp) {
		logger.info("loginWithOtp: attempting OTP login for phone={}", phone);
		AuthResponseDto response = new AuthResponseDto();
		try {
			// Step 1: verify the OTP
			OtpVerifyRequestDTO verifyRequest = OtpVerifyRequestDTO.builder()
				.phone(phone)
				.otp(otp)
				.purpose(Constants.PURPOSE_LOGIN)
				.build();

			ResponseDTO verifyResult = otpService.verifyOtp(verifyRequest);

			if (!Constants.SUCCESS_STATUS.equals(verifyResult.getResponseStatus())) {
				logger.warn("loginWithOtp: OTP verification failed for phone={} — {}", phone,
						verifyResult.getResponseMessage());
				response.setStatus(Constants.FAILURE_STATUS);
				response.setMessage(verifyResult.getResponseMessage());
				return response;
			}

			// Step 2: fetch the active user and return auth details
			List<UserEO> users = userRepository.findByPhoneAndStatus(phone, Constants.STATUS_ACTIVE);
			if (users == null || users.isEmpty()) {
				logger.error("loginWithOtp: no active user found for phone={} after OTP verification", phone);
				response.setStatus(Constants.FAILURE_STATUS);
				response.setMessage(Constants.OTP_LOGIN_USER_NOT_FOUND);
				return response;
			}

			UserEO user = users.get(0);
			logger.info("loginWithOtp: login successful for userId={}, phone={}", user.getId(), phone);

			// Step 3: update lastLoginAt
			user.setLastLoginAt(java.time.OffsetDateTime.now());
			userRepository.save(user);

			response.setStatus(Constants.SUCCESS_STATUS);
			response.setMessage(Constants.OTP_LOGIN_SUCCESS);
			response.setId(user.getId());
			response.setEmail(user.getEmail());
			response.setRole(user.getRole());
			return response;
		}
		catch (Exception e) {
			logger.error("loginWithOtp error for phone={}: {}", phone, e.getMessage(), e);
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage(Constants.TECHNICAL_ERROR_LOGIN);
			return response;
		}
	}

	@Override
	public ResponseDTO resetPassword(ResetPasswordRequestDTO resetPasswordRequestDTO) {
		ResponseDTO responseDTO = new ResponseDTO();
		try {
			if (resetPasswordRequestDTO == null) {
				logger.warn("resetPassword called with null request");
				responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
				responseDTO.setResponseMessage(Constants.TECHNICAL_ERROR_RESETPASSWORD);
				return responseDTO;
			}

			String phoneNo = resetPasswordRequestDTO.getPhoneNo();
			String email = resetPasswordRequestDTO.getEmail();

			String identifier = (phoneNo != null && !phoneNo.trim().isEmpty()) ? phoneNo : email;
			if (identifier == null || identifier.trim().isEmpty()) {
				logger.warn("resetPassword called without phoneNo/email");

				responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
				responseDTO.setResponseMessage(Constants.NO_IDENTIFIER_PROVIDED);
				return responseDTO;
			}

			logger.info("Resetting password for identifier: {}", identifier);

			String purpose = Constants.PURPOSE_FORGOT_PASSWORD;
			String userOtp = resetPasswordRequestDTO.getOtp();
			String newPassword = resetPasswordRequestDTO.getNewPassword();

			if (userOtp == null || userOtp.trim().isEmpty()) {
				logger.warn("resetPassword called with empty otp for identifier: {}", identifier);
				responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
				responseDTO.setResponseMessage(Constants.OTP_VERIFICATION_FAILED);
				return responseDTO;

			}

			if (newPassword == null || newPassword.trim().isEmpty()) {
				logger.warn("resetPassword called with empty newPassword for identifier: {}", identifier);
				responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
				responseDTO.setResponseMessage(Constants.NEW_PASSWORD_REQUIRED_RESETPASSWORD);
				return responseDTO;
			}

			// OTP was already verified by MSG91 Widget in the verifyOtp step.
			// otpHash stores the MSG91 reqId (not a BCrypt hash), so we must NOT
			// compare by otpHash — just look up the most recent VERIFIED record.
			List<OTPStoreEO> verifiedOtpList = otpRepo.findByIdentifierAndStatusAndPurpose(identifier,
					Constants.OTP_STATUS_VERIFIED, purpose);

			if (verifiedOtpList == null || verifiedOtpList.isEmpty()) {
				logger.warn("No verified OTP found for identifier: {}", identifier);
				return ResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage(Constants.OTP_VERIFICATION_FAILED)
					.build();
			}

			// Use the most recently verified OTP record
			OTPStoreEO otpstoreEO = verifiedOtpList.stream()
				.max(java.util.Comparator.comparing(OTPStoreEO::getId))
				.orElse(null);

			if (otpstoreEO == null) {
				logger.warn("OTP verification failed for identifier: {}", identifier);
				return ResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage(Constants.OTP_VERIFICATION_FAILED)
					.build();
			}

			List<UserEO> userlist = null;
			if (phoneNo != null && !phoneNo.trim().isEmpty()) {
				userlist = userRepository.findByPhoneAndStatus(phoneNo, Constants.STATUS_ACTIVE);
			}
			else if (email != null && !email.trim().isEmpty()) {
				userlist = userRepository.findByEmailAndStatus(email, Constants.STATUS_ACTIVE);
			}

			if (userlist == null || userlist.isEmpty()) {
				logger.warn("No active user found for identifier: {}", identifier);
				return ResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage(Constants.NO_ACTIVE_USER)
					.build();
			}
			else if (userlist.size() > 1) {
				logger.warn("Multiple active users found for identifier: {}", identifier);
				return ResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage(Constants.MULTIPLE_ACTIVE_USERS)
					.build();
			}

			UserEO userEO = userlist.get(0);
			userEO.setPasswordHash(PasswordUtil.hashPassword(newPassword));
			userEO.setUpdatedAt(java.time.OffsetDateTime.now());
			userEO.setUpdatedBy(purpose + "-" + identifier);
			userRepository.save(userEO);

			// Invalidate the verified OTP so it cannot be reused
			otpstoreEO.setStatus(Constants.OTP_STATUS_EXPIRED);
			otpRepo.save(otpstoreEO);

			logger.info("Password reset successful for userId: {}", userEO.getId());
			return ResponseDTO.builder()
				.responseStatus(Constants.SUCCESS_STATUS)
				.responseMessage(Constants.PASSWORD_RESET_SUCCESS)
				.build();
		}
		catch (Exception e) {

			logger.error("Error while resetting password for");

			return ResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage(Constants.TECHNICAL_ERROR_RESETPASSWORD)
				.build();
		}
	}

	@Override
	@Transactional
	public ResponseDTO updateCustomer(CustomerDTO customerUpdateDTO) {
		logger.info("Updating customer with customerId: {}", customerUpdateDTO.getCustomerId());
		ResponseDTO response = new ResponseDTO();
		try {
			CustomerEO customer = customerRepository.findById(customerUpdateDTO.getCustomerId())
				.orElseThrow(() -> new RuntimeException("Customer not found"));
			if (customerUpdateDTO.getFirstName() != null)
				customer.setFirstName(customerUpdateDTO.getFirstName());
			if (customerUpdateDTO.getEmail() != null)
				customer.setEmail(customerUpdateDTO.getEmail());
			if (customerUpdateDTO.getMobileNumber() != null)
				customer.setMobileNumber(customerUpdateDTO.getMobileNumber());
			if (customerUpdateDTO.getCustomerType() != null)
				customer.setCustomerType(customerUpdateDTO.getCustomerType());
			if (customerUpdateDTO.getStatus() != null)
				customer.setStatus(customerUpdateDTO.getStatus());
			customerRepository.save(customer);
			if (customerUpdateDTO.getAddresses() != null && !customerUpdateDTO.getAddresses().isEmpty()) {
				List<CustomerAddressEO> existingAddresses = customerAddressRepository.findByCustomer(customer);
				for (CustomerAddressDTO addressDTO : customerUpdateDTO.getAddresses()) {
					CustomerAddressEO address;
					if (addressDTO.getAddressId() != null) {
						address = existingAddresses.stream()
							.filter(a -> a.getAddressId().equals(addressDTO.getAddressId()))
							.findFirst()
							.orElse(new CustomerAddressEO());
						address.setCustomer(customer);
						existingAddresses.removeIf(a -> a.getAddressId().equals(addressDTO.getAddressId()));
					}
					else {
						address = new CustomerAddressEO();
						address.setCustomer(customer);
					}
					if (addressDTO.getAddressLine1() != null)
						address.setAddressLine1(addressDTO.getAddressLine1());
					if (addressDTO.getCity() != null)
						address.setCity(addressDTO.getCity());
					if (addressDTO.getState() != null)
						address.setState(addressDTO.getState());
					if (addressDTO.getPostalCode() != null)
						address.setPostalCode(addressDTO.getPostalCode());
					if (addressDTO.getCountry() != null)
						address.setCountry(addressDTO.getCountry());
					if (addressDTO.getLandMark() != null)
						address.setLandMark(addressDTO.getLandMark());
					if (addressDTO.getAddressLine2() != null)
						address.setAddressLine2(addressDTO.getAddressLine2());
					if (addressDTO.getContactNumber() != null)
						address.setContactNumber(addressDTO.getContactNumber());
					if (addressDTO.getAddressType() != null)
						address.setAddressType(addressDTO.getAddressType());
					if (addressDTO.getRecipientName() != null)
						address.setRecipientName(addressDTO.getRecipientName());
					customerAddressRepository.save(address);
				}
				for (CustomerAddressEO addr : existingAddresses) {
					customerAddressRepository.delete(addr);
				}
			}
			logger.info("Customer updated successfully for customerId: {}", customerUpdateDTO.getCustomerId());
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Customer updated successfully");
		}
		catch (Exception e) {
			logger.error("Error updating customer for customerId: {}. Exception: {}", customerUpdateDTO.getCustomerId(),
					e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.TECHNICAL_ERROR_CUSTOMER_UPDATE);
		}
		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public CustomerDTO getCustomerDetailsByUserId(Long userId) {
		logger.info("Fetching customer details for userId: {}", userId);
		try {
			// Single query: joins customers → users (status guard) + LEFT JOIN FETCH
			// addresses
			// Replaces the previous 3 separate round-trips (findById → findByUser →
			// findByCustomer)
			CustomerEO customer = customerRepository.findActiveWithAddressesByUserId(userId, Constants.STATUS_ACTIVE)
				.orElse(null);
			if (customer == null) {
				logger.warn("No active customer found for userId: {}", userId);
				return null;
			}
			CustomerDTO dto = new CustomerDTO();
			dto.setCustomerId(customer.getCustomerId());
			dto.setFirstName(customer.getFirstName());
			dto.setEmail(customer.getEmail());
			dto.setMobileNumber(customer.getMobileNumber());
			dto.setCustomerType(customer.getCustomerType());
			dto.setStatus(customer.getStatus());

			// Addresses are already eagerly fetched by the JOIN FETCH — no extra query
			// needed
			List<CustomerAddressDTO> addressDTOs = customer.getAddresses() == null ? List.of()
					: customer.getAddresses().stream().map(addr -> {
						CustomerAddressDTO a = new CustomerAddressDTO();
						a.setAddressId(addr.getAddressId());
						a.setAddressType(addr.getAddressType());
						a.setRecipientName(addr.getRecipientName());
						a.setAddressLine1(addr.getAddressLine1());
						a.setAddressLine2(addr.getAddressLine2());
						a.setLandMark(addr.getLandMark());
						a.setCity(addr.getCity());
						a.setState(addr.getState());
						a.setCountry(addr.getCountry());
						a.setPostalCode(addr.getPostalCode());
						a.setContactNumber(addr.getContactNumber());
						return a;
					}).toList();
			dto.setAddresses(addressDTOs);
			logger.info("Customer details fetched for userId: {}", userId);
			return dto;
		}
		catch (Exception e) {
			logger.error("Error occurred in getCustomerDetailsByUserId for userId: {}", userId, e);
			return new CustomerDTO();
		}
	}

	// ═══════════════════════════════════════════════════════════
	// ADMIN APIs
	// ═══════════════════════════════════════════════════════════

	@Override
	public AdminUserListResponseDTO getAllUsersForAdmin(String search, String status, int page, int size) {
		AdminUserListResponseDTO response = new AdminUserListResponseDTO();
		try {
			Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
			// Pre-build lowercase %pattern% — null means "no search filter"
			String searchPattern = (search != null && !search.trim().isEmpty())
					? "%" + search.trim().toLowerCase() + "%" : null;
			String statusParam = (status != null && !status.trim().isEmpty()) ? status.trim() : null;

			Page<UserEO> userPage = userRepository.searchUsers(searchPattern, statusParam, pageable);

			List<AdminUserDTO> dtos = userPage.getContent()
				.stream()
				.map(this::buildAdminUserDTO)
				.collect(Collectors.toList());

			response.setUsers(dtos);
			response.setTotalCount(userPage.getTotalElements());
			response.setPage(page);
			response.setSize(size);
			response.setTotalPages(userPage.getTotalPages());
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Users fetched successfully. Total: " + userPage.getTotalElements());
			logger.info("getAllUsersForAdmin: returned {} of {} users (page={}, size={})", dtos.size(),
					userPage.getTotalElements(), page, size);
		}
		catch (Exception e) {
			logger.error("getAllUsersForAdmin error: {}", e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while fetching users: " + e.getMessage());
		}
		return response;
	}

	@Override
	public AdminUserDTO getUserDetailsByIdForAdmin(Long userId) {
		try {
			UserEO user = userRepository.findById(userId).orElse(null);
			if (user == null) {
				logger.warn("getUserDetailsByIdForAdmin: userId={} not found", userId);
				return AdminUserDTO.builder().build();
			}
			return buildAdminUserDTO(user);
		}
		catch (Exception e) {
			logger.error("getUserDetailsByIdForAdmin error for userId={}: {}", userId, e.getMessage(), e);
			return AdminUserDTO.builder().build();
		}
	}

	@Override
	public AdminUserDTO getCustomerDetailsByIdForAdmin(Integer customerId) {
		try {
			CustomerEO customer = customerRepository.findById(customerId).orElse(null);
			if (customer == null) {
				logger.warn("getCustomerDetailsByIdForAdmin: customerId={} not found", customerId);
				return AdminUserDTO.builder().build();
			}
			return buildAdminUserDTOFromCustomer(customer);
		}
		catch (Exception e) {
			logger.error("getCustomerDetailsByIdForAdmin error for customerId={}: {}", customerId, e.getMessage(), e);
			return AdminUserDTO.builder().build();
		}
	}

	@Override
	public AdminUserListResponseDTO getAllCustomersForAdmin(String search, String status, int page, int size) {
		AdminUserListResponseDTO response = new AdminUserListResponseDTO();
		try {
			Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
			// Pre-build lowercase %pattern% — null means "no search filter"
			String searchPattern = (search != null && !search.trim().isEmpty())
					? "%" + search.trim().toLowerCase() + "%" : null;
			String statusParam = (status != null && !status.trim().isEmpty()) ? status.trim() : null;

			Page<CustomerEO> customerPage = customerRepository.searchCustomers(searchPattern, statusParam, pageable);

			List<AdminUserDTO> dtos = customerPage.getContent()
				.stream()
				.map(this::buildAdminUserDTOFromCustomer)
				.collect(Collectors.toList());

			response.setUsers(dtos);
			response.setTotalCount(customerPage.getTotalElements());
			response.setPage(page);
			response.setSize(size);
			response.setTotalPages(customerPage.getTotalPages());
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Customers fetched successfully. Total: " + customerPage.getTotalElements());
			logger.info("getAllCustomersForAdmin: returned {} of {} customers (page={}, size={})", dtos.size(),
					customerPage.getTotalElements(), page, size);
		}
		catch (Exception e) {
			logger.error("getAllCustomersForAdmin error: {}", e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while fetching customers: " + e.getMessage());
		}
		return response;
	}

	@Override
	public ResponseDTO updateUserStatus(Long userId, String status) {
		ResponseDTO response = new ResponseDTO();
		try {
			if (status == null || (!Constants.STATUS_ACTIVE.equalsIgnoreCase(status)
					&& !Constants.STATUS_INACTIVE.equalsIgnoreCase(status))) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage(
						"Status must be '" + Constants.STATUS_ACTIVE + "' or '" + Constants.STATUS_INACTIVE + "'");
				return response;
			}
			UserEO user = userRepository.findById(userId).orElse(null);
			if (user == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("User not found for userId: " + userId);
				return response;
			}
			user.setStatus(status.toUpperCase());
			user.setUpdatedAt(java.time.OffsetDateTime.now());
			user.setUpdatedBy("ADMIN");
			userRepository.save(user);

			// Mirror status on customer record if present
			CustomerEO customer = customerRepository.findByUser(user).orElse(null);
			if (customer != null) {
				customer.setStatus(status.toUpperCase());
				customerRepository.save(customer);
			}

			logger.info("updateUserStatus: userId={} set to status={}", userId, status);
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("User status updated to '" + status + "' for userId: " + userId);
		}
		catch (Exception e) {
			logger.error("updateUserStatus error for userId={}: {}", userId, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while updating user status: " + e.getMessage());
		}
		return response;
	}

	// ── Private helpers ──────────────────────────────────────────

	private AdminUserDTO buildAdminUserDTO(UserEO user) {
		AdminUserDTO dto = new AdminUserDTO();
		dto.setUserId(user.getId() != null ? user.getId().longValue() : null);
		dto.setEmail(user.getEmail());
		dto.setPhone(user.getPhone());
		dto.setFirstName(user.getFirstName());
		dto.setRole(user.getRole());
		dto.setStatus(user.getStatus());
		dto.setPhoneVerified(user.getPhoneVerified());
		dto.setPhoneVerifiedAt(user.getPhoneVerifiedAt());
		dto.setUserCreatedAt(user.getCreatedAt());
		dto.setUserUpdatedAt(user.getUpdatedAt());
		dto.setLastLoginAt(user.getLastLoginAt());

		// Attach customer details
		try {
			CustomerEO customer = customerRepository.findByUser(user).orElse(null);
			if (customer != null) {
				populateCustomerFields(dto, customer);
			}
		}
		catch (Exception e) {
			logger.warn("buildAdminUserDTO: could not load customer for userId={}: {}", user.getId(), e.getMessage());
		}
		return dto;
	}

	private AdminUserDTO buildAdminUserDTOFromCustomer(CustomerEO customer) {
		AdminUserDTO dto = new AdminUserDTO();
		populateCustomerFields(dto, customer);

		// Attach user details
		try {
			UserEO user = customer.getUser();
			if (user != null) {
				dto.setUserId(user.getId() != null ? user.getId().longValue() : null);
				dto.setEmail(user.getEmail());
				dto.setPhone(user.getPhone());
				dto.setFirstName(user.getFirstName());
				dto.setRole(user.getRole());
				dto.setStatus(user.getStatus());
				dto.setPhoneVerified(user.getPhoneVerified());
				dto.setPhoneVerifiedAt(user.getPhoneVerifiedAt());
				dto.setUserCreatedAt(user.getCreatedAt());
				dto.setUserUpdatedAt(user.getUpdatedAt());
				dto.setLastLoginAt(user.getLastLoginAt());
			}
		}
		catch (Exception e) {
			logger.warn("buildAdminUserDTOFromCustomer: could not load user for customerId={}: {}",
					customer.getCustomerId(), e.getMessage());
		}
		return dto;
	}

	private void populateCustomerFields(AdminUserDTO dto, CustomerEO customer) {
		dto.setCustomerId(customer.getCustomerId());
		dto.setCustomerFirstName(customer.getFirstName());
		dto.setCustomerLastName(customer.getLastName());
		dto.setCustomerEmail(customer.getEmail());
		dto.setMobileNumber(customer.getMobileNumber());
		dto.setCustomerType(customer.getCustomerType());
		dto.setCustomerStatus(customer.getStatus());
		dto.setCustomerCreatedAt(customer.getCreatedAt());
		dto.setCustomerUpdatedAt(customer.getUpdatedAt());

		List<CustomerAddressEO> addresses = customerAddressRepository.findByCustomer(customer);
		if (addresses != null) {
			List<CustomerAddressDTO> addressDTOs = addresses.stream().map(addr -> {
				CustomerAddressDTO a = new CustomerAddressDTO();
				a.setAddressId(addr.getAddressId());
				a.setAddressType(addr.getAddressType());
				a.setRecipientName(addr.getRecipientName());
				a.setAddressLine1(addr.getAddressLine1());
				a.setAddressLine2(addr.getAddressLine2());
				a.setLandMark(addr.getLandMark());
				a.setCity(addr.getCity());
				a.setState(addr.getState());
				a.setCountry(addr.getCountry());
				a.setPostalCode(addr.getPostalCode());
				a.setContactNumber(addr.getContactNumber());
				return a;
			}).collect(Collectors.toList());
			dto.setAddresses(addressDTOs);
		}
	}

}
