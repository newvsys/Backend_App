package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnPolicyConditionCreateDTO {

	private Long policyId;

	private String conditionType;

	private String conditionValue;

}
