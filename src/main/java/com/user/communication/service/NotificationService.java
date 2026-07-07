package com.user.communication.service;

import com.user.utility.Constants;
import org.springframework.stereotype.Service;
import com.user.communication.event.Event;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class NotificationService {

	@Autowired
	private CommunicationService communicationService;

	public void processEvent(Event event) {

		if (Constants.COMMUNICATION_CHANNEL_EMAIL.equals(event.getChannel())) {
			communicationService.sendEmail(event);

		}
		else if (Constants.COMMUNICATION_CHANNEL_SMS.equals(event.getChannel())) {
			communicationService.sendSms(event);

		}
		else if (Constants.COMMUNICATION_CHANNEL_BOTH.equals(event.getChannel())) {
			communicationService.sendSms(event);
			communicationService.sendEmail(event);
		}
	}

}
