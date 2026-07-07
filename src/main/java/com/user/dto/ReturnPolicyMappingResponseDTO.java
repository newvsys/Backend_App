package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a single return-policy-mapping record.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnPolicyMappingResponseDTO {

	/** Mapping record ID. */
	private Long id;

	/** The return policy linked by this mapping. */
	private Long policyId;

	private String policyName;

	/**
	 * Type of entity this mapping applies to. Values: {@code "PRODUCTS"},
	 * {@code "CATEGORY"}, {@code "GLOBAL"}
	 */
	private String entityType;

	/**
	 * ID of the entity (product ID, category ID, or 0 for GLOBAL).
	 */
	private Long entityId;

	/** Higher value = more specific / higher priority. Default 0. */
	private Integer priority;

}
