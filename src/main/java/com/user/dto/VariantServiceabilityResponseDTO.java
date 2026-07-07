package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for variant-aware serviceability check.
 *
 * <ul>
 * <li>{@code serviceable = true} — every warehouse that holds stock for every requested
 * product variant can deliver to the given postcode.</li>
 * <li>{@code serviceable = false} — at least one warehouse→destination pair is not
 * serviceable (or no warehouse/postal-code data was found).</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantServiceabilityResponseDTO {

	/** Overall serviceability result across all variants and warehouses. */
	private boolean serviceable;

	/** Destination postal code that was checked. */
	private String deliveryPostcode;

	/** Optional human-readable summary message. */
	private String message;

	/** Per-variant breakdown. */
	private List<VariantDetail> variants;

	// ─────────────────────────────────────────────────────────────────────────

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class VariantDetail {

		private Long productVariantId;

		/** True only when ALL warehouses for this variant are serviceable. */
		private boolean serviceable;

		/** Per-warehouse breakdown for this variant. */
		private List<WarehouseDetail> warehouses;

	}

	// ─────────────────────────────────────────────────────────────────────────

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class WarehouseDetail {

		private Long warehouseId;

		private String warehouseName;

		private String warehousePostalCode;

		/** True when Shiprocket returned ≥ 1 available courier company. */
		private boolean serviceable;

		/** Number of available courier companies returned by Shiprocket. */
		private int availableCourierCount;

		/**
		 * True when no inventory / warehouse was found for the variant and the check was
		 * performed against the system default warehouse instead.
		 */
		private boolean usingDefaultWarehouse;

		/**
		 * Reason when not serviceable (e.g. "No postal code configured", "No couriers
		 * available").
		 */
		private String reason;

	}

}
