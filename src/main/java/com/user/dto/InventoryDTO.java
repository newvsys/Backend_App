package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDTO {

	private Integer inventoryId;

	private Integer productVarId;

	private Integer totalQty;

	private Integer availableQty;

	private String whid;

}