package com.user.dto;

import lombok.Data;

/**
 * Request DTO for the inventory count refresh API. All fields are optional — supply one
 * or more filters to limit scope; leave all null to refresh every inventory record in the
 * system.
 */
@Data
public class InventoryRefreshRequestDTO {

	/**
	 * Refresh only this specific inventory record (most precise filter).
	 */
	private Long inventoryId;

	/**
	 * Refresh all inventory records belonging to this product variant.
	 */
	private Long productVarId;

	/**
	 * Refresh all inventory records belonging to this warehouse.
	 */
	private Long warehouseId;

	/**
	 * User / system identifier recorded in the audit trail (remarks). Defaults to
	 * "SYSTEM" if not provided.
	 */
	private String refreshedBy;

}
