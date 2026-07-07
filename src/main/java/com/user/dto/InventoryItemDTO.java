package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemDTO {

	private Long id;

	private String barcode;

	private String batchNo;

	private String status;

	private LocalDate mfd;

	private LocalDate bestBefore;

	private LocalDate expiryDate;

	private OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;

}
