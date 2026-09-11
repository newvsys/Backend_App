package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A single line item as required by Shiprocket's "Create Order" API
 * (`order_items[]`).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiprocketOrderItemPayloadDTO {

	private String name;

	private String sku;

	private Integer units;

	private BigDecimal sellingPrice;

	private Double discount;

	private Integer tax;

	private String hsn;

}

