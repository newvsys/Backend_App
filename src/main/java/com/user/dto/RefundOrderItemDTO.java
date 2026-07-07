package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundOrderItemDTO {

	private String productName;

	private String skuCode;

	private Integer quantity;

	private BigDecimal unitPrice;

	private BigDecimal totalPrice;

}
