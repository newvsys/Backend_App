package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for the "Out of Stock Report" API — GET /api/inventory/out-of-stock.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutOfStockResponseDTO {

	private String responseStatus;

	private String responseMessage;

	/** Total number of out-of-stock product variants returned. */
	private Integer totalRecords;

	private List<OutOfStockItemDTO> items;

}

