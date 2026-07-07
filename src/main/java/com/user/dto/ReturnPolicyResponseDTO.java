package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnPolicyResponseDTO {

	private Long id;

	private String name;

	private String description;

	private Integer returnWindowDays;

	private Boolean isReturnable;

	private String refundType;

	private String returnMethod;

}
