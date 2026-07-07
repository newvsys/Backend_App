package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryInfoDTO {

	private Long inventoryId;

	private Long productVarId;

	private String productVariantName;

	private Long warehouseId;

	private String warehouseCode;

	private String warehouseName;

	private Integer totalQty;

	private Integer availableQty;

	private Integer reservedQty;

	private Integer quantityReserved;

	private Integer reorderLevel;

	private Integer safetyStock;

	private String status;

	private List<InventoryItemDTO> items;

}
