package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnPolicyDetailDTO {

	private Long policyId;

	private String name;

	private String description;

	private Integer returnWindowDays;

	private Boolean isReturnable;

	private String refundType;

	private String returnMethod;

	private List<ReturnPolicyConditionResponseDTO> conditions;

}
