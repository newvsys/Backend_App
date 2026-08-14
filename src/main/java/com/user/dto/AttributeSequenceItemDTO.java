package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single entry in a bulk attribute-sequence update request — pairs an
 * existing {@code ProductAttributeEO.id} with the new {@code displayOrder}
 * value it should have.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeSequenceItemDTO {

	/** ID of an existing product attribute (must belong to the target variant). */
	private Long attributeId;

	/** New display/sort order for this attribute (lower = shown first). */
	private Integer displayOrder;

}

