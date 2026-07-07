package com.user.service;

import com.user.communication.service.CommunicationService;
import com.user.communication.service.NotificationService;
import com.user.dto.*;
import com.user.model.OTPStoreEO;
import com.user.repository.OTPStoreRepository;
import com.user.utility.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class OtpServiceImpl implements OtpService {

	private static final Logger logger = LogManager.getLogger(OtpServiceImpl.class);

	private static final Pattern INDIA_PHONE_PATTERN = Pattern.compile("^91[6-9]\\d{9}$");

	public static boolean isValidIndianPhoneNumber(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.isBlank()) {
			return false;
		}
		return INDIA_PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
	}

	@Autowired
	private OTPStoreRepository otpRepo;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private CommunicationService communicationService;

	@Override
	public ResponseDTO generateOtp(OtpRequestDTO otpRequestDTO) {
		try {
			logger.info(" Service impl Received generateOtp request for phone: {} and purpose: {}",
					otpRequestDTO.getPhone(), otpRequestDTO.getPurpose());
			String identifier = otpRequestDTO.getPhone();
			String purpose = otpRequestDTO.getPurpose();
			String ipAddress = otpRequestDTO.getIpAddress();
			String deviceId = otpRequestDTO.getDeviceId();
			MsgOtpResponse msgOtpResponse = null;
			if (purpose == null || purpose.isEmpty()) {
				purpose = Constants.MOBILE_NO_VERIFICATION;
			}
			String phonenowithCountryCode = Constants.COUNTRY_CODE + identifier;
			OTPStoreEO existingOtp = null;

			List<OTPStoreEO> otplist = otpRepo.findByIdentifierAndStatusAndPurpose(identifier,
					Constants.OTP_STATUS_ACTIVE, purpose);
			for (OTPStoreEO existing : otplist) {
				LocalDateTime expireTime = existing.getExpiresAt();
				LocalDateTime now = LocalDateTime.now();
				if (!now.isAfter(expireTime)) {

					existingOtp = existing;

				}
				else {
					// current time > expireTime, i.e. OTP request has expired
					logger.debug("Expiring existing active OTP for identifier: {} and purpose: {} (otpId: {})",
							identifier, purpose, existing.getId());
					existing.setStatus(Constants.OTP_STATUS_EXPIRED);
					otpRepo.save(existing);
				}
			}
			MsgRetryOtpResponse msgRetryOtpResponse = null;
			String responsemessage = "";
			String status = "";
			if (existingOtp != null) {
				String reqId = existingOtp.getOtpHash();
				// proceed with retry using reqId
				msgRetryOtpResponse = communicationService.retryOtp(reqId);
				if (msgRetryOtpResponse.getType().equalsIgnoreCase("success")) {
					responsemessage = Constants.OTP_GENERATION_SUCCESSFUL;
					status = Constants.SUCCESS_STATUS;
				}
				else if (msgRetryOtpResponse.getMessage().equalsIgnoreCase("retry count excceded")) {
					responsemessage = Constants.OTP_GENERATION_FAILED_MAX_RETRY_REACHED;
					status = Constants.FAILURE_STATUS;
				}
				else {
					responsemessage = Constants.OTP_GENERATION_FAILED;
					status = Constants.FAILURE_STATUS;
				}
			}
			else {
				// msgService.sendOtp(identifier);
				if (isValidIndianPhoneNumber(phonenowithCountryCode)) {
					msgOtpResponse = communicationService.sendOtp(phonenowithCountryCode);
					if (msgOtpResponse.getType().equalsIgnoreCase("success")) {
						OTPStoreEO otpStore = OTPStoreEO.builder()
							.identifier(identifier)
							.purpose(purpose)
							.otpHash(msgOtpResponse.getMessage())
							.attempts(0)
							.status(Constants.OTP_STATUS_ACTIVE)
							.expiresAt(java.time.LocalDateTime.now().plusMinutes(Constants.OTP_EXPIRED_TIME_IN_MINS))
							.ipAddress(ipAddress)
							.deviceId(deviceId)
							.build();
						otpRepo.save(otpStore);
						logger.info("Saved OTP for identifier: {} and purpose: {}", identifier, purpose);
						responsemessage = Constants.OTP_GENERATION_SUCCESSFUL;
						status = Constants.SUCCESS_STATUS;
					}
					else {
						responsemessage = Constants.OTP_GENERATION_FAILED;
						status = Constants.FAILURE_STATUS;
					}

				}
				else {
					responsemessage = Constants.OTP_GENERATION_FAILED;
					status = Constants.FAILURE_STATUS;

				}
			}
			// Expire existing active OTPs for this identifier and purpose

			/**
			 * // Generate secure 6-digit OTP String otp =
			 * PasswordUtil.generateSixDigitOtp();
			 *
			 * String hashedOtp = PasswordUtil.hashPassword(otp);
			 */
			// Save new OTP

			/**
			 * // Send OTP via NotificationService (Kafka) String smsSubject = "OTP for "
			 * + purpose; String smsMessage = "Your OTP for " + purpose + " is :" + otp;
			 * kafkaTemplate.send( Constants.CUSTOMER_EVENTS_TOPIC,
			 * ObjectMapper.buildEventObject( null, identifier, purpose, null, null,
			 * smsSubject, smsMessage, Constants.COMMUNICATION_CHANNEL_SMS) );
			 * logger.info("Sent OTP notification for identifier: {}", identifier);
			 **/

			return ResponseDTO.builder().responseStatus(status).responseMessage(responsemessage).build();
		}
		catch (Exception e) {
			logger.error("Error occurred while generating OTP: ", e);
			return ResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage(Constants.TECHNICAL_ERROR_OTP_GENERATION)
				.build();
		}
	}

	@Override
	public ResponseDTO verifyOtp(OtpVerifyRequestDTO otpVerifyRequestDTO) {
		ResponseDTO responseDTO = new ResponseDTO();
		MsgOtpVerifyResponse msgOtpVerifyResponse = null;
		try {
			logger.info("Received verifyOtp request for phone: {} and purpose: {}", otpVerifyRequestDTO.getPhone(),
					otpVerifyRequestDTO.getPurpose());
			String identifier = otpVerifyRequestDTO.getPhone();
			String purpose = otpVerifyRequestDTO.getPurpose();
			String userOtp = otpVerifyRequestDTO.getOtp();
			List<OTPStoreEO> otpList = otpRepo.findByIdentifierAndStatusAndPurpose(identifier,
					Constants.OTP_STATUS_ACTIVE, purpose);
			if (otpList != null && !otpList.isEmpty()) {
				boolean validOtp = false;
				boolean notExpired = true;
				OTPStoreEO otpStore1 = null;
				String verifyotpresponse = null;
				for (OTPStoreEO otpStore : otpList) {
					// validOtp =
					// PasswordUtil.hashPassword(userOtp).equals(otpStore.getOtpHash());
					// notExpired =
					// otpStore.getExpiresAt().isAfter(java.time.LocalDateTime.now());
					msgOtpVerifyResponse = communicationService.verifyOtp(otpStore.getOtpHash(), userOtp);
					validOtp = msgOtpVerifyResponse.getType().equalsIgnoreCase("success");
					verifyotpresponse = msgOtpVerifyResponse.getMessage();
					logger.debug("Checking OTP with otpId: {} - valid: {}, notExpired: {}", otpStore.getId(), validOtp,
							notExpired);
					if (validOtp) {
						otpStore1 = otpStore;
						break; // Exit loop if a valid and not expired OTP is found
					}
				}
				logger.debug("OTP validation for identifier: {} - valid: {}, notExpired: {}", identifier, validOtp,
						notExpired);
				if (validOtp && otpStore1 != null) {

					String result = ("alreadyverified");
					if (msgOtpVerifyResponse.getToken() != null) {
						result = msgOtpVerifyResponse.getToken().length() > 50
								? msgOtpVerifyResponse.getToken().substring(0, 50) : msgOtpVerifyResponse.getToken();
					}
					otpStore1.setVerificationToken(result);
					otpStore1.setVerifiedAt(java.time.LocalDateTime.now());

					logger.info("OTP verified successfully for identifier: {}", identifier);
					if (msgOtpVerifyResponse.getType().equalsIgnoreCase("success")
							|| (msgOtpVerifyResponse.getCode() != null && msgOtpVerifyResponse.getCode() == 703)) {
						responseDTO.setResponseStatus(Constants.SUCCESS_STATUS);
						responseDTO.setResponseMessage(Constants.OTP_VERIFICATION_SUCCESS);
						otpStore1.setStatus(Constants.OTP_STATUS_VERIFIED);
					}
					else {
						responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
						responseDTO.setResponseMessage(Constants.OTP_VERIFICATION_FAILED);
					}

					otpRepo.save(otpStore1);

				}
				else if (verifyotpresponse.equalsIgnoreCase("invalid otp")
						|| verifyotpresponse.equalsIgnoreCase("OTP does not match")) {
					logger.debug("Invalid or expired OTP for identifier: {}", identifier);
					responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
					responseDTO.setResponseMessage(Constants.OTP_VERIFICATION_FAILED_INVALIED_OTP);
				}
				else if (verifyotpresponse
					.equalsIgnoreCase("Maximum retry count exceeded. Please try again after 15 minutes")) {
					responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
					responseDTO.setResponseMessage(verifyotpresponse);
				}
				else {
					logger.debug("Invalid or expired OTP for identifier: {}", identifier);
					responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
					responseDTO.setResponseMessage(Constants.OTP_VERIFICATION_FAILED);

				}
			}
			else {
				logger.warn("No active OTP found for identifier: {} and purpose: {}", identifier, purpose);
				responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
				responseDTO.setResponseMessage(Constants.OTP_VERIFICATION_FAILED);
			}
		}
		catch (Exception e) {
			logger.error("Error occurred while verifying OTP: ", e);
			responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
			responseDTO.setResponseMessage(Constants.TECHNICAL_ERROR_OTP_VERIFICATION);
		}
		return responseDTO;
	}

}
