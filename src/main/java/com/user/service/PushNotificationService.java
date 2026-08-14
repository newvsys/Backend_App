package com.user.service;

import com.user.dto.DeviceTokenRegisterDTO;
import com.user.model.CartonEO;
import com.user.model.OrderEO;
import com.user.model.ShippingEO;

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

	/**
	 * Sends a "new carton auto-created" push notification to all registered admin
	 * devices, e.g. when {@code CartonSelectionService} could not find an existing
	 * carton that fits an order and had to dynamically create one.
	 * Best-effort: any failure is logged and swallowed so it never breaks order
	 * processing.
	 */
	void notifyAdminsNewCarton(CartonEO carton);

	/**
	 * Sends a "no courier service available" push notification to all registered
	 * admin devices, e.g. when {@code getBestCourierServices} could not find any
	 * eligible courier for a shipment's route/weight/COD combination. The Shiprocket
	 * order is still created; the shipment is flagged for manual courier
	 * assignment. Best-effort: any failure is logged and swallowed so it never
	 * breaks order processing.
	 */
	void notifyAdminsNoCourierFound(ShippingEO shipping, Integer shiprocketOrderId);

}

