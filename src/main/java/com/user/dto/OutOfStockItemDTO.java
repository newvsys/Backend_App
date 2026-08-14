package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A single row of the "Out of Stock Report" — a product / product variant that either
 * has NO inventory record at all, or has zero (or negative) available quantity across
 * every warehouse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutOfStockItemDTO {

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

	// ── Stock ─────────────────────────────────────────────────────────────
	/** {@code true} if at least one InventoryEO record exists for this variant. */
	private Boolean hasInventoryRecord;

	/** Sum of availableQty across every warehouse's inventory record (0 if none exist). */
	private Long totalAvailableQty;

	/** Always {@code "OUT_OF_STOCK"} — kept for consistency with the stock-report API. */
	private String stockStatus;

}

