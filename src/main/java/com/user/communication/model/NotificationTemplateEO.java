package com.user.communication.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "template_code", length = 50)
	private String templateCode;

	@Column(name = "channel", length = 20)
	private String channel;

	@Column(name = "subject", length = 200)
	private String subject;

	@Column(name = "body", columnDefinition = "TEXT")
	private String body;

}
