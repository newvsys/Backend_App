package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single row from the {@code shipment_items} table – i.e. one order item
 * packed into a given shipment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentItemDTO {

	private Long shipmentItemId;

	private Integer orderItemId;

	private String skuCode;

	private String productVarName;

	private Integer quantity;

	private BigDecimal unitPrice;

	private BigDecimal totalPrice;

	private LocalDateTime createdAt;

}

