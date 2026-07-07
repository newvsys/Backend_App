package com.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PickupResponse {

	// Shiprocket actual shape: top-level → response → { pickup_id, pickup_scheduled_date,
	// pickup_token_number, ... }
	@JsonProperty("response")
	private PickupDetail response;

	@JsonProperty("status")
	private Integer status; // 1 = success

	/**
	 * Convenience accessors that resolve from nested response first, then top-level
	 * fallbacks.
	 */
	public Long getResolvedPickupId() {
		return response != null && response.getPickupId() != null ? response.getPickupId() : null;
	}

	public String getResolvedPickupScheduledDate() {
		return response != null && response.getPickupScheduledDate() != null ? response.getPickupScheduledDate() : null;
	}

	public String getResolvedPickupTokenNumber() {
		return response != null && response.getPickupTokenNumber() != null ? response.getPickupTokenNumber() : null;
	}

	// Keep top-level fields as fallback (some API versions return flat structure)
	@JsonProperty("pickup_id")
	private Long pickupId;

	@JsonProperty("pickup_scheduled_date")
	private String pickupScheduledDate;

	@JsonProperty("pickup_token_number")
	private String pickupTokenNumber;

	@JsonProperty("already_picked_up")
	private List<Long> alreadyPickedUp;

	@JsonProperty("pickup_generated_date")
	private String pickupGeneratedDate;

	@Data
	public static class PickupDetail {

		@JsonProperty("pickup_id")
		private Long pickupId;

		@JsonProperty("pickup_scheduled_date")
		private String pickupScheduledDate;

		@JsonProperty("pickup_token_number")
		private String pickupTokenNumber;

		@JsonProperty("pickup_generated_date")
		private String pickupGeneratedDate;

		@JsonProperty("appointment_date")
		private String appointmentDate;

	}

}
