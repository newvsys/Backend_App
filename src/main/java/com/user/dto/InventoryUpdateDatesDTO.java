package com.user.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Request body for updating MFG / EXP / best-before dates on existing inventory detail
 * rows.
 *
 * Supply EITHER {@code batchNo} (updates every item in that batch) OR {@code barcode}
 * (updates one specific unit). If both are given, {@code batchNo} wins.
 */
@Data
public class InventoryUpdateDatesDTO {

	/** Batch number — all items in this batch are updated. */
	private String batchNo;

	/** Single-item barcode — only this unit is updated (used when batchNo is absent). */
	private String barcode;

	/** Manufactured / production date. */
	private LocalDate mfd;

	/** Best-before date (optional). */
	private LocalDate bestBefore;

	/** Expiry / expiration date. */
	private LocalDate expiryDate;

}
