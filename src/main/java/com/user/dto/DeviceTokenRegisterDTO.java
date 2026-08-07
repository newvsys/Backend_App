package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload used by the Admin web app to register/unregister an FCM browser token so it can
 * receive push notifications (e.g. new order alerts).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceTokenRegisterDTO {

	private String token;

	// Optional - id of the admin user registering the token.
	private Long userId;

	// e.g. "admin". Defaults to "admin" when not supplied.
	private String role;

	// e.g. "WEB". Defaults to "WEB" when not supplied.
	private String platform;

}

