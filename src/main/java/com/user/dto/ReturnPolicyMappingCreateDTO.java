package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnPolicyMappingCreateDTO {

	private Long policyId;

	private String entityType;

	private Long entityId;

	private Integer priority;

}
