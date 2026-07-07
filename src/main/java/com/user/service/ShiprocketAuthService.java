package com.user.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ShiprocketAuthService {

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

	public String getToken() {
		if (cachedToken == null || LocalDateTime.now().isAfter(tokenExpiry)) {
			refreshToken();
		}
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
		ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

		cachedToken = (String) response.getBody().get("token");
		tokenExpiry = LocalDateTime.now().plusHours(23); // refresh before 24hr expiry
	}

}