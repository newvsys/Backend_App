package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for the "get return policy by category" API. Returns category metadata
 * alongside its effective return policy (with conditions). {@code returnPolicy} is
 * {@code null} when no mapping exists for that category.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryReturnPolicyResponseDTO {

	/** Internal category ID. */
	private Long categoryId;

	/** Category display name. */
	private String categoryName;

	/**
	 * Indicates which level the policy was resolved at: {@code "CATEGORY"},
	 * {@code "PRODUCTS"}, {@code "GLOBAL"}, or {@code null} (not found).
	 */
	private String resolvedVia;

	/**
	 * Effective return policy mapped to this category. {@code null} when no mapping
	 * exists at any level.
	 */
	private ReturnPolicyDetailDTO returnPolicy;

	/**
	 * List of products in this category that have a PRODUCTS-level return policy mapping.
	 * Populated only when {@code resolvedVia = "PRODUCTS"}. Empty for CATEGORY or GLOBAL
	 * level resolutions.
	 */
	private List<MappedProductDTO> mappedProducts;

}
