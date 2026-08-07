package com.user.communication.listener;

import com.user.communication.service.NotificationService;
import com.user.service.OrderService;
import com.user.service.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * EventListener – events are processed via direct method calls (no message broker).
 * This class is kept for reference and can be removed if no longer needed.
 */
@Component
public class EventListener {

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private ShippingService shippingService;

	@Autowired
	private OrderService orderService;

}
