package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewListResponseDTO {

	private Integer productId;

	private Double averageRating;

	private Long totalReviews;

	/** Distribution: key = star (1-5), value = count */
	private Map<Integer, Long> ratingDistribution;

	private List<ProductReviewDTO> reviews;

}
