package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestoreInventoryResponseDTO {

	private String responseStatus;

	private String responseMessage;

	private String barcode;

	private String previousStatus;

	private Long inventoryId;

	private Integer availableQtyBefore;

	private Integer availableQtyAfter;

	private Long transactionId;

}
