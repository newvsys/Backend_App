package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for {@code PUT /api/products/productsVariant/{variantId}/attributes/sequence}.
 * <p>
 * Carries the full desired ordering (attributeId → displayOrder) for the attributes
 * belonging to a single product variant. All listed attribute IDs must already exist
 * and belong to the given variant.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeSequenceUpdateDTO {

	/** List of attribute-id/displayOrder pairs to apply. */
	private List<AttributeSequenceItemDTO> attributes;

}

