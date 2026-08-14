package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A single variant's worth of image-sequence updates, used as one entry
 * inside a {@link BulkImageSequenceUpdateDTO} request. Pairs a
 * {@code variantId} with the list of image-id/displayOrder pairs that
 * belong to it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantImageSequenceDTO {

	/** ID of the product variant these images belong to. */
	private Long variantId;

	/** List of image-id/displayOrder pairs to apply for this variant. */
	private List<ImageSequenceItemDTO> images;

}

