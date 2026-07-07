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
public class ProdVarDTO {

	private Integer variantId;

	private String skuCode;

	private String packSize;

	private String uom;

	private String containerType;

	private BigDecimal mrp;

	private BigDecimal sellingPrice;

	private String status;

	private Long productId;

	private BigDecimal length;

	private BigDecimal breadth;

	private BigDecimal height;

	private BigDecimal weight;

	private List<ProductImageDTO> productImages;

	private List<ProductAttributeDTO> attributes;

	private String videoUrl;

}