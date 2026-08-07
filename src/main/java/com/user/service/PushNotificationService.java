package com.user.service;

import com.user.dto.DeviceTokenRegisterDTO;
import com.user.model.OrderEO;

/**
 * Handles Firebase Cloud Messaging (FCM) push notifications sent to the Admin web app,
 * e.g. alerting admins in real time when a new order is placed.
 */
public interface PushNotificationService {

	/**
	 * Registers (or re-activates) a browser/device FCM token so it starts receiving push
	 * notifications.
	 */
	void registerToken(DeviceTokenRegisterDTO request);

	/**
	 * Deactivates a previously registered FCM token (e.g. on admin logout).
	 */
	void unregisterToken(String token);

	/**
	 * Sends a "new order" push notification to all registered admin devices.
	 * Best-effort: any failure is logged and swallowed so it never breaks order creation.
	 */
	void notifyAdminsNewOrder(OrderEO order);

}

