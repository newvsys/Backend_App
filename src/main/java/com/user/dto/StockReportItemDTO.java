package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A single row of the "Current Stock Report" — combines category, product, product
 * variant, warehouse, and inventory (stock) details for reporting/export.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReportItemDTO {

	// ── Category ─────────────────────────────────────────────────────────
	private Integer categoryId;

	private String categoryName;

	// ── Product ───────────────────────────────────────────────────────────
	private Integer productId;

	private String productName;

	private String productSlug;

	private String productStatus;

	// ── Product Variant ───────────────────────────────────────────────────
	private Integer variantId;

	private String skuCode;

	private String packSize;

	private String uom;

	private BigDecimal mrp;

	private BigDecimal sellingPrice;

	private String currency;

	private String variantStatus;

	// ── Warehouse ─────────────────────────────────────────────────────────
	private Long warehouseId;

	private String warehouseCode;

	private String warehouseName;

	private String warehouseCity;

	// ── Inventory / Stock ─────────────────────────────────────────────────
	private Integer inventoryId;

	private Integer totalQty;

	private Integer availableQty;

	private Integer reservedQty;

	private Integer quantityReserved;

	private Integer reorderLevel;

	private Integer safetyStock;

	private String inventoryStatus;

	/** Derived: OUT_OF_STOCK / LOW_STOCK / IN_STOCK based on availableQty vs reorderLevel. */
	private String stockStatus;

}

