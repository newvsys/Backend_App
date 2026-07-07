package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadInventoryRequestDTO {

	/** Product variant to load stock for */
	private Long productVarId;

	/** Number of units to add */
	private Integer qty;

	/** Warehouse code (whid) */
	private String whid;

	// ── Per-item details (optional, same for the whole batch) ──────────────────

	/** Manufactured date */
	private LocalDate mfd;

	/** Best before use date */
	private LocalDate bestBefore;

	/** Expiry date */
	private LocalDate expiryDate;

}
