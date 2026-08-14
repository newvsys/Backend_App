package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for the "Current Stock Report" API — GET /api/inventory/stock-report.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReportResponseDTO {

	private String responseStatus;

	private String responseMessage;

	/** Total number of (product-variant, warehouse) rows returned. */
	private Integer totalRecords;

	/** Sum of availableQty across all returned rows. */
	private Long totalAvailableQty;

	/** Sum of totalQty across all returned rows. */
	private Long totalStockQty;

	private List<StockReportItemDTO> reportItems;

}

