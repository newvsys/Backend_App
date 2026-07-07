package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product details included in the "get return policy by category" response when the
 * policy is resolved at the PRODUCTS level. Each entry represents one product in the
 * category that has a return-policy mapping.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MappedProductDTO {

	/** Internal product ID. */
	private Long productId;

	/** Product display name. */
	private String productName;

	/** Product slug. */
	private String productSlug;

	/** Priority of this product's mapping (higher = more specific). */
	private Integer mappingPriority;

	/** The return policy mapped to this specific product. */
	private ReturnPolicyDetailDTO returnPolicy;

}
