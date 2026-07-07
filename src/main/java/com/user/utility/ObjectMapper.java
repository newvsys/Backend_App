package com.user.utility;

import com.user.communication.event.Event;
import com.user.dto.UserCreateDTO;
import com.user.model.UserEO;

public class ObjectMapper {

	public static UserEO userCreateDTOToUserEO(UserCreateDTO dto) {
		UserEO.UserEOBuilder builder = UserEO.builder();
		if (dto.getEmail() != null) {
			builder.email(dto.getEmail());
		}
		if (dto.getPhone() != null) {
			builder.phone(dto.getPhone());
		}
		if (dto.getFirstName() != null) {
			builder.firstName(dto.getFirstName());
		}
		if (dto.getPassword() != null) {
			builder.passwordHash(PasswordUtil.hashPassword(dto.getPassword()));
		}
		builder.role(Constants.ROLE_USER);
		builder.status(Constants.STATUS_ACTIVE);
		builder.createdAt(java.time.OffsetDateTime.now());
		builder.createdBy(dto.getPhone());
		return builder.build();
	}

	public static Event buildEventObject(String email, String mobile, String purpose, String emailSubject,
			String emailMessage, String smsSubject, String smsMessage, String channel) {
		Event.EventBuilder builder = Event.builder();
		if (email != null)
			builder.email(email);
		if (mobile != null)
			builder.mobile(mobile);
		if (purpose != null)
			builder.purpose(purpose);
		if (emailSubject != null)
			builder.emailSubject(emailSubject);
		if (emailMessage != null)
			builder.emailMessage(emailMessage);
		if (smsSubject != null)
			builder.smsSubject(smsSubject);
		if (smsMessage != null)
			builder.smsMessage(smsMessage);
		if (channel != null)
			builder.channel(channel);
		return builder.build();
	}

}
