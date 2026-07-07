package com.user.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProdVarUpdateDTO {

	private Long variantId;

	private String skuCode;

	private String packSize;

	private String uom;

	private String containerType;

	private BigDecimal mrp;

	private BigDecimal sellingPrice;

	private String status;

	private BigDecimal length;

	private BigDecimal breadth;

	private BigDecimal height;

	private BigDecimal weight;

}