package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for variant-aware serviceability check. Accepts a delivery postal code and
 * one or more product variant IDs. The service resolves the warehouse(s) for each
 * variant, then checks Shiprocket serviceability for every (warehouse → delivery) pair.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantServiceabilityRequestDTO {

	/**
	 * Destination postal code where the order should be delivered. (REQUIRED)
	 */
	private String deliveryPostcode;

	/**
	 * One or more product variant IDs to check serviceability for. (REQUIRED)
	 */
	private List<Long> productVariantIds;

}
