package com.user.controller;

import com.user.dto.DeviceTokenRegisterDTO;
import com.user.dto.ResponseDTO;
import com.user.service.PushNotificationService;
import com.user.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints used by the Admin web app to register/unregister Firebase Cloud Messaging
 * (FCM) browser tokens so it can receive push notifications (e.g. new order alerts).
 */
@RestController
@RequestMapping("/push")
public class NotificationController {

	private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

	@Autowired
	private PushNotificationService pushNotificationService;

	@PostMapping("/register-token")
	public ResponseEntity<ResponseDTO> registerToken(@RequestBody DeviceTokenRegisterDTO request) {
		ResponseDTO response = new ResponseDTO();
		try {
			if (request == null || request.getToken() == null || request.getToken().trim().isEmpty()) {
				response.setResponseMessage("Device token is required");
				response.setResponseStatus(Constants.FAILURE_STATUS);
				return ResponseEntity.badRequest().body(response);
			}
			pushNotificationService.registerToken(request);
			response.setResponseMessage("Device token registered successfully");
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error registering FCM device token", e);
			response.setResponseMessage("Failed to register device token");
			response.setResponseStatus(Constants.FAILURE_STATUS);
			return ResponseEntity.status(500).body(response);
		}
	}

	@PostMapping("/unregister-token")
	public ResponseEntity<ResponseDTO> unregisterToken(@RequestBody Map<String, String> request) {
		ResponseDTO response = new ResponseDTO();
		try {
			String token = request != null ? request.get("token") : null;
			if (token == null || token.trim().isEmpty()) {
				response.setResponseMessage("Device token is required");
				response.setResponseStatus(Constants.FAILURE_STATUS);
				return ResponseEntity.badRequest().body(response);
			}
			pushNotificationService.unregisterToken(token);
			response.setResponseMessage("Device token unregistered successfully");
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error unregistering FCM device token", e);
			response.setResponseMessage("Failed to unregister device token");
			response.setResponseStatus(Constants.FAILURE_STATUS);
			return ResponseEntity.status(500).body(response);
		}
	}

}

