package com.user.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.user.dto.DeviceTokenRegisterDTO;
import com.user.model.CartonEO;
import com.user.model.CustomerEO;
import com.user.model.DeviceTokenEO;
import com.user.model.OrderEO;
import com.user.model.ShippingEO;
import com.user.repository.DeviceTokenRepository;
import com.user.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends Firebase Cloud Messaging (FCM) push notifications to the Admin web app, e.g. to
 * alert admins in real time when a new order is created.
 *
 * Sending is best-effort: if Firebase isn't initialized (disabled / missing credentials)
 * or the actual send call fails, the error is logged and swallowed so it never breaks the
 * order-creation flow.
 */
@Service
public class PushNotificationServiceImpl implements PushNotificationService {

	private static final Logger logger = LoggerFactory.getLogger(PushNotificationServiceImpl.class);

	@Autowired
	private DeviceTokenRepository deviceTokenRepository;

	@Value("${firebase.enabled:false}")
	private boolean firebaseEnabled;

	private boolean isFirebaseReady() {
		return firebaseEnabled && !FirebaseApp.getApps().isEmpty();
	}

	@Override
	public void registerToken(DeviceTokenRegisterDTO request) {
		if (request == null || request.getToken() == null || request.getToken().trim().isEmpty()) {
			throw new IllegalArgumentException("Device token is required");
		}

		String token = request.getToken().trim();
		DeviceTokenEO deviceToken = deviceTokenRepository.findByToken(token).orElseGet(DeviceTokenEO::new);

		OffsetDateTime now = OffsetDateTime.now();
		boolean isNew = deviceToken.getId() == null;

		deviceToken.setToken(token);
		deviceToken.setUserId(request.getUserId());
		deviceToken.setRole(request.getRole() != null ? request.getRole() : Constants.ROLE_ADMIN);
		deviceToken.setPlatform(request.getPlatform() != null ? request.getPlatform() : "WEB");
		deviceToken.setActive(true);
		deviceToken.setUpdatedAt(now);
		if (isNew) {
			deviceToken.setCreatedAt(now);
		}

		deviceTokenRepository.save(deviceToken);
		logger.info("Registered FCM device token for role={}, userId={}", deviceToken.getRole(),
				deviceToken.getUserId());
	}

	@Override
	public void unregisterToken(String token) {
		if (token == null || token.trim().isEmpty()) {
			return;
		}
		deviceTokenRepository.findByToken(token.trim()).ifPresent(deviceToken -> {
			deviceToken.setActive(false);
			deviceToken.setUpdatedAt(OffsetDateTime.now());
			deviceTokenRepository.save(deviceToken);
			logger.info("Unregistered FCM device token id={}", deviceToken.getId());
		});
	}

	@Override
	public void notifyAdminsNewOrder(OrderEO order) {
		if (order == null) {
			return;
		}
		if (!isFirebaseReady()) {
			logger.debug("Firebase not initialized/enabled. Skipping new-order push notification for order={}",
					order.getOrderNumber());
			return;
		}

		List<DeviceTokenEO> tokens = deviceTokenRepository.findByRoleAndActiveTrue(Constants.ROLE_ADMIN);
		if (tokens.isEmpty()) {
			logger.debug("No active admin device tokens found. Skipping new-order push notification for order={}",
					order.getOrderNumber());
			return;
		}

		String customerName = "Customer";
		CustomerEO customer = order.getCustomer();
		if (customer != null && customer.getFirstName() != null && !customer.getFirstName().trim().isEmpty()) {
			customerName = customer.getFirstName();
		}

		BigDecimal amount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
		String title = "New Order Received";
		String body = String.format("Order #%s placed by %s - %s %s", order.getOrderNumber(), customerName,
				order.getCurrency() != null ? order.getCurrency() : Constants.PAYMENT_CURRENCY, amount.toPlainString());

		Map<String, String> data = new HashMap<>();
		data.put("type", Constants.ORDER_EVENT_TYPE_CREATED);
		data.put("orderNumber", order.getOrderNumber() != null ? order.getOrderNumber() : "");
		data.put("orderId", order.getOrderId() != null ? String.valueOf(order.getOrderId()) : "");
		data.put("amount", amount.toPlainString());

		for (DeviceTokenEO deviceToken : tokens) {
			sendToToken(deviceToken, title, body, data);
		}
	}

	@Override
	public void notifyAdminsNewCarton(CartonEO carton) {
		if (carton == null) {
			return;
		}
		if (!isFirebaseReady()) {
			logger.debug("Firebase not initialized/enabled. Skipping new-carton push notification for carton={}",
					carton.getName());
			return;
		}

		List<DeviceTokenEO> tokens = deviceTokenRepository.findByRoleAndActiveTrue(Constants.ROLE_ADMIN);
		if (tokens.isEmpty()) {
			logger.debug("No active admin device tokens found. Skipping new-carton push notification for carton={}",
					carton.getName());
			return;
		}

		String title = "New Carton Auto-Created";
		String body = String.format("Carton '%s' created (%.2fx%.2fx%.2f cm, max %.2f kg) as no existing carton fit the order",
				carton.getName(), carton.getLength(), carton.getBreadth(), carton.getHeight(), carton.getMaxWeight());

		Map<String, String> data = new HashMap<>();
		data.put("type", Constants.CARTON_EVENT_TYPE_CREATED);
		data.put("cartonId", carton.getId() != null ? String.valueOf(carton.getId()) : "");
		data.put("cartonName", carton.getName() != null ? carton.getName() : "");
		data.put("length", String.valueOf(carton.getLength()));
		data.put("breadth", String.valueOf(carton.getBreadth()));
		data.put("height", String.valueOf(carton.getHeight()));
		data.put("maxWeight", String.valueOf(carton.getMaxWeight()));

		for (DeviceTokenEO deviceToken : tokens) {
			sendToToken(deviceToken, title, body, data);
		}
	}

	/**
	 * Sends a "no courier service available" push notification to all registered
	 * admin devices when {@code getBestCourierServices} could not find any eligible
	 * courier for a shipment. The Shiprocket order was still created; the shipment
	 * is flagged for manual courier assignment.
	 */
	@Override
	public void notifyAdminsNoCourierFound(ShippingEO shipping, Integer shiprocketOrderId) {
		if (shipping == null) {
			return;
		}
		if (!isFirebaseReady()) {
			logger.debug(
					"Firebase not initialized/enabled. Skipping no-courier-found push notification for shipmentId={}",
					shipping.getShipmentId());
			return;
		}

		List<DeviceTokenEO> tokens = deviceTokenRepository.findByRoleAndActiveTrue(Constants.ROLE_ADMIN);
		if (tokens.isEmpty()) {
			logger.debug(
					"No active admin device tokens found. Skipping no-courier-found push notification for shipmentId={}",
					shipping.getShipmentId());
			return;
		}

		String orderNumber = shipping.getOrder() != null ? shipping.getOrder().getOrderNumber() : "N/A";

		String title = "No Courier Service Available";
		String body = String.format(
				"Order #%s: no eligible courier found for delivery. Order created (Shiprocket order_id=%s) — please assign a courier manually.",
				orderNumber, shiprocketOrderId != null ? shiprocketOrderId : "N/A");

		Map<String, String> data = new HashMap<>();
		data.put("type", Constants.SHIPMENT_STATUS_MANUAL_PROCESSING_REQUIRED);
		data.put("shipmentId", shipping.getShipmentId() != null ? String.valueOf(shipping.getShipmentId()) : "");
		data.put("orderNumber", orderNumber != null ? orderNumber : "");
		data.put("shiprocketOrderId", shiprocketOrderId != null ? String.valueOf(shiprocketOrderId) : "");

		for (DeviceTokenEO deviceToken : tokens) {
			sendToToken(deviceToken, title, body, data);
		}
	}

	private void sendToToken(DeviceTokenEO deviceToken, String title, String body, Map<String, String> data) {
		try {
			Message message = Message.builder()
				.setToken(deviceToken.getToken())
				.setNotification(Notification.builder().setTitle(title).setBody(body).build())
				.putAllData(data)
				.build();

			String messageId = FirebaseMessaging.getInstance().send(message);
			logger.info("Sent new-order push notification. messageId={}, deviceTokenId={}", messageId,
					deviceToken.getId());
		}
		catch (FirebaseMessagingException e) {
			MessagingErrorCode errorCode = e.getMessagingErrorCode();
			logger.error("Failed to send push notification to deviceTokenId={}. errorCode={}, message={}",
					deviceToken.getId(), errorCode, e.getMessage());

			// Clean up tokens that are no longer valid so we stop retrying them.
			if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT
					|| errorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {
				deviceToken.setActive(false);
				deviceToken.setUpdatedAt(OffsetDateTime.now());
				deviceTokenRepository.save(deviceToken);
				logger.info("Deactivated invalid FCM device token id={}", deviceToken.getId());
			}
		}
		catch (Exception e) {
			logger.error("Unexpected error sending push notification to deviceTokenId={}: {}", deviceToken.getId(),
					e.getMessage(), e);
		}
	}

}

