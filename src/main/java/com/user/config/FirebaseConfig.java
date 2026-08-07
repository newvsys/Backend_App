package com.user.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Initializes the Firebase Admin SDK so that {@link com.google.firebase.messaging.FirebaseMessaging}
 * can be used to send push notifications (e.g. to the Admin web app when a new order is
 * created).
 *
 * Initialization is best-effort: if the feature is disabled or the service-account file is
 * missing/invalid, the app still starts normally and push notifications are simply skipped
 * (see {@link com.user.service.PushNotificationServiceImpl}).
 */
@Configuration
public class FirebaseConfig {

	private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

	private final ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Value("${firebase.enabled:false}")
	private boolean firebaseEnabled;

	@Value("${firebase.service-account-file:classpath:firebase-service-account.json}")
	private String serviceAccountFile;

	@PostConstruct
	public void initialize() {
		if (!firebaseEnabled) {
			logger.info("Firebase push notifications are disabled (firebase.enabled=false).");
			return;
		}

		if (!FirebaseApp.getApps().isEmpty()) {
			logger.info("FirebaseApp already initialized. Skipping re-initialization.");
			return;
		}

		try {
			Resource resource = resourceLoader.getResource(serviceAccountFile);
			if (!resource.exists()) {
				logger.warn(
						"Firebase service-account file not found at '{}'. Push notifications will be disabled.",
						serviceAccountFile);
				return;
			}

			try (InputStream serviceAccountStream = resource.getInputStream()) {
				FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
					.build();
				FirebaseApp.initializeApp(options);
				logger.info("FirebaseApp initialized successfully. Push notifications are enabled.");
			}
		}
		catch (IOException e) {
			logger.error("Failed to initialize FirebaseApp. Push notifications will be disabled. Error: {}",
					e.getMessage(), e);
		}
	}

}

