package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for {@code PUT /api/products/productsVariant/images/sequence/bulk}.
 * <p>
 * Carries the desired image ordering for one or more product variants in a
 * single API call. Each entry's {@code imageId}s must already exist and
 * belong to the corresponding {@code variantId}; if any entry across any
 * variant fails validation the whole update is rolled back.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkImageSequenceUpdateDTO {

	/** List of per-variant image-sequence updates to apply. */
	private List<VariantImageSequenceDTO> variants;

}

