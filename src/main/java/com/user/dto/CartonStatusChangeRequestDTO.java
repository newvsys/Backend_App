package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartonStatusChangeRequestDTO {

	/**
	 * New status for the carton. 'A' = Active, 'I' = Inactive (soft-delete).
	 */
	private String status;

	/** User / identifier who is changing the status. */
	private String who;

}
