package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewCreateDTO {

	private Integer productId;

	private Integer customerId;

	/** 1 to 5 */
	private Integer rating;

	private String title;

	private String reviewText;

	/** Optional image URLs (e.g. already uploaded) */
	private List<String> imageUrls;

}
