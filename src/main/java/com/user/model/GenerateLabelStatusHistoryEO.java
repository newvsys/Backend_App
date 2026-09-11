package com.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks every change of {@link ShippingEO#getGenerateLabelStatus()} (Shiprocket
 * "GENERATE_LABEL" step outcome: SUCCESS / FAILED / SKIPPED) over time for a shipment.
 */
@Entity
@Table(name = "generate_label_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateLabelStatusHistoryEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id", nullable = false)
	private ShippingEO shipment;

	@Column(name = "status", length = 50)
	private String status;

	@Column(name = "remarks", length = 1000)
	private String remarks;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}

}

