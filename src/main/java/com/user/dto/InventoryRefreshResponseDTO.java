package com.user.dto;

import lombok.Data;

import java.util.List;

/**
 * Response DTO returned by the inventory count refresh API.
 */
@Data
public class InventoryRefreshResponseDTO {

	private String status;

	private String message;

	/** Number of inventory records successfully refreshed. */
	private int refreshedCount;

	/** Per-record refresh results. */
	private List<InventoryRefreshDetailDTO> details;

	// ── Inner detail DTO ────────────────────────────────────────────────

	@Data
	public static class InventoryRefreshDetailDTO {

		private Long inventoryId;

		private Long productVariantId;

		private String productVariantName;

		private Long warehouseId;

		private String warehouseName;

		/** Available qty as it was before the refresh. */
		private int previousAvailableQty;

		/** Total qty as it was before the refresh. */
		private int previousTotalQty;

		/** New available qty — count of InventoryDetails rows with status='A'. */
		private int newAvailableQty;

		/** New total qty — count of all InventoryDetails rows for this inventory. */
		private int newTotalQty;

		/** Net change in available qty (newAvailableQty - previousAvailableQty). */
		private int availableQtyDelta;

		/** ID of the STOCK_REFRESH transaction recorded for this inventory. */
		private Long transactionId;

	}

}
