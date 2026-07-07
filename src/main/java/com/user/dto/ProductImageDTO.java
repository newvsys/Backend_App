package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageDTO {

	private Integer id;

	private String imageID;

	private String productID;

	private String image;

	private String productName;

	private String imagePath;

	private String isMainImage;

}