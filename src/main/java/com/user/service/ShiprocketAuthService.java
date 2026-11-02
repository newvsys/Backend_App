package com.user.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class ShiprocketAuthService {

	private static final Logger logger = LoggerFactory.getLogger(ShiprocketAuthService.class);

	@Value("${shiprocket.email}")
	private String email;

	@Value("${shiprocket.password}")
	private String password;

	@Value("${shiprocket.base-url}")
	private String baseUrl;

	private final RestTemplate restTemplate;

	private String cachedToken;

	private LocalDateTime tokenExpiry;

	public ShiprocketAuthService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	/**
	 * Returns a valid Shiprocket auth token, logging in again if the cached token is
	 * missing or past its refresh window.
	 */
	public String getToken() {
		if (cachedToken == null || tokenExpiry == null || LocalDateTime.now().isAfter(tokenExpiry)) {
			refreshToken();
		}
		return cachedToken;
	}

	/**
	 * Clears the cached token so the next {@link #getToken()} call forces a fresh login.
	 * Call this after receiving a 401 from any Shiprocket API so the stale token is not
	 * reused for the remaining ~23h cache window.
	 */
	public void invalidateToken() {
		logger.warn("Invalidating cached Shiprocket token after an authentication failure");
		this.cachedToken = null;
		this.tokenExpiry = null;
	}

	/**
	 * Forces a fresh login and returns the new token.
	 */
	public String forceRefreshToken() {
		refreshToken();
		return cachedToken;
	}

	private void refreshToken() {
		String url = baseUrl + "/auth/login";

		Map<String, String> body = new HashMap<>();
		body.put("email", email);
		body.put("password", password);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
		try {
			ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

			String token = response.getBody() != null ? (String) response.getBody().get("token") : null;
			if (token == null || token.isBlank()) {
				this.cachedToken = null;
				this.tokenExpiry = null;
				logger.error("Shiprocket login succeeded (HTTP {}) but no token was returned. "
						+ "Check shiprocket.email / shiprocket.password.", response.getStatusCode());
				throw new RestClientException("Shiprocket authentication failed: no token in login response");
			}

			this.cachedToken = token;
			this.tokenExpiry = LocalDateTime.now().plusHours(23); // refresh before 24hr expiry
			logger.info("Obtained new Shiprocket auth token (valid ~24h) for account {}", email);
		}
		catch (RestClientException ex) {
			// Clear any stale state so we don't serve a half-updated token.
			this.cachedToken = null;
			this.tokenExpiry = null;
			logger.error("Shiprocket login failed for account {} at {}: {}", email, url, ex.getMessage());
			throw ex;
		}
	}

}