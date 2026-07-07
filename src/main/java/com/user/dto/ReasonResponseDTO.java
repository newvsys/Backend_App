package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReasonResponseDTO {

	private Long id;

	private String reasonCode;

	private String reasonDescription;

	private String type;

	private String status;

	private String responseStatus;

	private String responseMessage;

}
