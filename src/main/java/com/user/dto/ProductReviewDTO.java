package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewDTO {

	private Long id;

	private Integer productId;

	private String productName;

	private Integer productVariantId;

	private Integer customerId;

	private String customerName;

	private Integer rating;

	private String title;

	private String reviewText;

	private String status;

	private List<String> imageUrls;

	private OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;

}
