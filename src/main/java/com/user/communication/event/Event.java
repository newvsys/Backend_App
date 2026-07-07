package com.user.communication.event;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

	private String email;

	private String mobile;

	private String purpose;

	private String emailSubject;

	private String emailMessage;

	private String smsSubject;

	private String smsMessage;

	private String channel;

	private EmailDetails emailDetails;

	private SmsDetails smsDetails;

	/**
	 * Override the MSG91 template id for this specific event. If null, falls back to the
	 * default config value.
	 */
	private String templateId;

}
