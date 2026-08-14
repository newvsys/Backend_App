package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for {@code PUT /api/products/productsVariant/{variantId}/images/sequence}.
 * <p>
 * Carries the full desired ordering (imageId → displayOrder) for the images
 * belonging to a single product variant. All listed image IDs must already
 * exist and belong to the given variant.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageSequenceUpdateDTO {

	/** List of image-id/displayOrder pairs to apply. */
	private List<ImageSequenceItemDTO> images;

}

