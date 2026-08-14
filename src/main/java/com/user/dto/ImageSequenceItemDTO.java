package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single entry in an image-sequence update request — pairs an existing
 * {@code ProductImageEO.id} with the new {@code displayOrder} value it
 * should have.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageSequenceItemDTO {

	/** ID of an existing product image (must belong to the target variant). */
	private Long imageId;

	/** New display/sort order for this image (lower = shown first). */
	private Integer displayOrder;

}

