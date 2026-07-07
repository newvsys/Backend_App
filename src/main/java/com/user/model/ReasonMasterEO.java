package com.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reason_master")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReasonMasterEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "reason_code", nullable = false, unique = true)
	private String reasonCode;

	@Column(name = "reason_description", nullable = false)
	private String reasonDescription;

	@Column(name = "status", nullable = false)
	private String status;

	@Column(name = "type", nullable = false)
	private String type; // e.g., "CANCELLATION", "RETURN", "EXCHANGE"

}
