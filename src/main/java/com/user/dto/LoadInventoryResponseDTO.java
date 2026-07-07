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
public class LoadInventoryResponseDTO {

	private String responseStatus;

	private String responseMessage;

	private String batchNo;

	private Long batchId;

	private Long inventoryId;

	private Integer quantityLoaded;

	private Integer totalAvailableQty;

	/** One unique barcode per physical unit loaded */
	private List<String> barcodes;

}
