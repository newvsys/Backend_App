package com.user.communication.service;

import org.springframework.web.client.HttpClientErrorException;
import com.user.communication.event.Event;
import com.user.communication.model.CommunicationLogEO;
import com.user.communication.repository.NotificationLogRepository;
import com.user.dto.MsgOtpResponse;
import com.user.dto.MsgOtpVerifyResponse;
import com.user.dto.MsgRetryOtpResponse;
import com.user.utility.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class CommunicationService {

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private NotificationLogRepository notificationLogRepository;

	@Value("${msg91.authkey}")
	private String authKey;

	@Value("${msg91.tokenAuth}")
	private String tokenAuth;

	@Value("${msg91.widget-id}")
	private String widgetId;

	@Value("${msg91.sender}")
	private String sender;

	@Value("${msg91.email.url}")
	private String emailUrl;

	@Value("${msg91.from.email}")
	private String fromEmail;

	@Value("${msg91.from.name}")
	private String fromName;

	@Value("${mail.service.provider}")
	private String mailServiceProvider;

	@Value("${msg91.url:https://api.msg91.com/api/sendhttp.php}")
	private String msg91Url;

	@Value("${msg91.base-url:https://control.msg91.com/api/v5/widget}")
	private String msg91BaseUrl;

	@Value("${msg91.email.template_id}")
	private String emailTemplateId;

	public void sendSms(Event event) {
		try {
			String message = event.getSmsMessage();

			String url = msg91Url + "?authkey=" + authKey + "&mobiles=" + event.getMobile() + "&message="
					+ URLEncoder.encode(message, StandardCharsets.UTF_8) + "&sender=" + sender + "&route=4";
			RestTemplate restTemplate = new RestTemplate();
			CommunicationLogEO log = new CommunicationLogEO();
			log.setPurpose(event.getPurpose());
			log.setChannel(event.getChannel());
			log.setRecipient(event.getMobile());
			log.setMessage(message);
			log.setStatus(Constants.COMMUNICATION_STATUS_SENT);
			notificationLogRepository.save(log);
			// restTemplate.getForObject(url, String.class);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public MsgOtpResponse sendOtp(String mobileNumber) {
		RestTemplate restTemplate = new RestTemplate();
		MsgOtpResponse otpResponse = null;
		try {
			String url = msg91BaseUrl + "/sendOtp";
			ObjectMapper mapper = new ObjectMapper();

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("authkey", authKey);

			Map<String, Object> request = new HashMap<>();
			request.put("widgetId", widgetId);
			request.put("identifier", mobileNumber);

			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
			CommunicationLogEO log = new CommunicationLogEO();
			log.setPurpose("OTP generate");
			log.setChannel("OTP");
			log.setRecipient(mobileNumber);
			log.setMessage(response.getBody().toString());
			log.setStatus(Constants.COMMUNICATION_STATUS_SENT);
			notificationLogRepository.save(log);
			otpResponse = mapper.readValue(response.getBody(), MsgOtpResponse.class);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to parse OTP response from MSG91", e);
			// or return an error response object, depending on your error-handling style
		}
		return otpResponse;
	}

	public MsgRetryOtpResponse retryOtp(String reqId) {
		String url = msg91BaseUrl + "/retryOtp";
		RestTemplate restTemplate = new RestTemplate();
		MsgRetryOtpResponse retryOtpResponse = null;
		if (reqId == null || reqId.isBlank()) {

			throw new IllegalArgumentException("reqId is required to retry OTP");
		}

		Map<String, Object> body = new HashMap<>();
		body.put("widgetId", widgetId);
		body.put("tokenAuth", tokenAuth);
		body.put("reqId", reqId);
		body.put("retryChannel", Constants.OTP_RETRY_CHANNEL);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

		try {
			ResponseEntity<MsgRetryOtpResponse> response = restTemplate.postForEntity(url, entity,
					MsgRetryOtpResponse.class);
			CommunicationLogEO log = new CommunicationLogEO();
			log.setPurpose("OTP resend");
			log.setChannel("OTP");
			log.setRecipient(reqId);
			log.setMessage(response.getBody().toString());
			log.setStatus(Constants.COMMUNICATION_STATUS_SENT);
			notificationLogRepository.save(log);
			return response.getBody();

		}
		catch (HttpClientErrorException ex) {
			// MSG91 returns 4xx with a JSON error body - log and rethrow as needed
			throw new RuntimeException("MSG91 retry OTP failed: " + ex.getResponseBodyAsString(), ex);
		}
	}

	public MsgOtpVerifyResponse verifyOtp(String reqId, String otp) {
		RestTemplate restTemplate = new RestTemplate();
		MsgOtpVerifyResponse otpVerifyResponse = null;
		try {

			String url = msg91BaseUrl + "/verifyOtp";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("authkey", authKey);

			Map<String, Object> request = new HashMap<>();
			request.put("widgetId", widgetId);
			request.put("reqId", reqId);
			request.put("otp", otp);

			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
			CommunicationLogEO log = new CommunicationLogEO();
			log.setPurpose("OTP verify");
			log.setChannel("OTP");
			log.setRecipient(reqId);
			log.setMessage(response.getBody().toString());
			log.setStatus(Constants.COMMUNICATION_STATUS_SENT);
			notificationLogRepository.save(log);
			otpVerifyResponse = new ObjectMapper().readValue(response.getBody(), MsgOtpVerifyResponse.class);

		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to parse OTP response from MSG91", e);
			// or return an error response object, depending on your error-handling style
		}
		return otpVerifyResponse;
	}

	public void sendEmail(Event event) {
		if (mailServiceProvider.equals("MSG91")) {
			try {
				RestTemplate restTemplate = new RestTemplate();

				com.user.communication.event.EmailDetails details = event.getEmailDetails();
				Map<String, String> variables = new HashMap<>();
				if (details != null) {
					// Order Status Update template variables
					variables.put("customer_name", nullToEmpty(details.getCustomerName()));
					variables.put("order_id", nullToEmpty(details.getOrderId()));
					variables.put("order_status", nullToEmpty(details.getOrderStatus()));
					variables.put("tracking_number", nullToEmpty(details.getTrackingNumber()));
					variables.put("delivery_date", nullToEmpty(details.getExpectedDelivery()));
					variables.put("tracking_url", nullToEmpty(details.getTrackingUrl()));
					// Order Cancel / Return template variables
					variables.put("update_type", nullToEmpty(details.getUpdateType()));
					variables.put("status", nullToEmpty(details.getStatus()));
					variables.put("amount", nullToEmpty(details.getAmount()));
					variables.put("message", nullToEmpty(details.getMessage()));
					variables.put("refund_days", nullToEmpty(details.getRefundDays()));
				}
				// ── Build to array ────────────────────────────────────────────────
				Map<String, String> toEntry = new HashMap<>();
				toEntry.put("email", event.getEmail());
				if (details != null && details.getCustomerName() != null) {
					toEntry.put("name", details.getCustomerName());
				}

				// ── MSG91 v5: each recipient entry has "to" + "variables" ─────────
				Map<String, Object> recipientEntry = new HashMap<>();
				recipientEntry.put("to", new Object[] { toEntry });
				recipientEntry.put("variables", variables);

				// ── Build from object ─────────────────────────────────────────────
				Map<String, String> fromObj = new HashMap<>();
				fromObj.put("email", fromEmail);
				fromObj.put("name", fromName);

				// ── Use templateId from event if provided, else fall back to default
				// config ──
				String resolvedTemplateId = (event.getTemplateId() != null && !event.getTemplateId().isBlank())
						? event.getTemplateId() : emailTemplateId;

				// ── Assemble request body ─────────────────────────────────────────
				Map<String, Object> body = new HashMap<>();
				body.put("recipients", new Object[] { recipientEntry });
				body.put("from", fromObj);
				body.put("domain", Constants.MSG91_EMAIL_DOMAIN);
				body.put("template_id", resolvedTemplateId);

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				headers.set("authkey", authKey);

				HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
				restTemplate.postForEntity(emailUrl, request, String.class);

			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			// Fallback: SMTP via JavaMailSender
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(event.getEmail());
			message.setSubject(event.getEmailSubject());
			message.setText(event.getEmailMessage());
			mailSender.send(message);
		}

		CommunicationLogEO log = new CommunicationLogEO();
		log.setPurpose(event.getPurpose());
		log.setChannel(event.getChannel());
		log.setRecipient(event.getEmail());
		log.setMessage(event.getEmailMessage());
		log.setStatus(Constants.COMMUNICATION_STATUS_SENT);
		notificationLogRepository.save(log);
	}

	private String nullToEmpty(String value) {
		return value != null ? value : "";
	}

}
