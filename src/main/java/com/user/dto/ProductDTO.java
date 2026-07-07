package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {

	private Integer id;

	private Integer productId;

	private String title;

	private String description;

	private BigDecimal price;

	private BigDecimal mrp;

	private String currency;

	private String category;

	private String sku;

	private String mainImage;

	private String slug;

	private Integer inStock;

	private Integer stock;

	private List<ProductAttributeDTO> attributes;

	private String isReturnable;

	private ReturnPolicyDetailDTO returnPolicy;

	private List<ProductDTO> productvarlist;

	private String videoUrl;

}