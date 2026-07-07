package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReasonRequestDTO {

	private String reasonCode;

	private String reasonDescription;

	private String type; // e.g., "CANCELLATION", "RETURN", "EXCHANGE"

}
