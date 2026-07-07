package com.user.service;

import com.user.dto.InventoryDTO;
import com.user.dto.InventoryFetchResponseDTO;
import com.user.dto.InventoryRefreshRequestDTO;
import com.user.dto.InventoryRefreshResponseDTO;
import com.user.dto.InventoryUpdateDatesDTO;
import com.user.dto.InventoryUpdateDatesResponseDTO;
import com.user.dto.LoadInventoryRequestDTO;
import com.user.dto.LoadInventoryResponseDTO;
import com.user.dto.RemoveInventoryRequestDTO;
import com.user.dto.RemoveInventoryResponseDTO;
import com.user.dto.RestoreInventoryRequestDTO;
import com.user.dto.RestoreInventoryResponseDTO;

public interface InventoryService {

	/**
	 * Load (add) stock for a product variant into a warehouse.
	 * <p>
	 * <ul>
	 * <li>Creates or updates the {@code InventoryEO} record.</li>
	 * <li>Generates a unique batch number for this load.</li>
	 * <li>Records one {@code InventoryTransactionEO} of type {@code LOAD_INVENTORY}.</li>
	 * <li>Inserts one {@code InventoryDetailsEO} row per unit with a unique barcode.</li>
	 * </ul>
	 */
	LoadInventoryResponseDTO loadInventory(LoadInventoryRequestDTO request);

	/**
	 * Restore a single inventory unit identified by its barcode.
	 * <p>
	 * <ul>
	 * <li>Sets {@code InventoryDetailsEO.status} back to {@code AVAILABLE}.</li>
	 * <li>Increments {@code InventoryEO.availableQty} by 1.</li>
	 * <li>Records one {@code InventoryTransactionEO} of type
	 * {@code RESTORE_INVENTORY}.</li>
	 * </ul>
	 */
	RestoreInventoryResponseDTO restoreInventory(RestoreInventoryRequestDTO request);

	/**
	 * Remove a single inventory unit identified by its barcode.
	 * <p>
	 * <ul>
	 * <li>Sets {@code InventoryDetailsEO.status} to {@code I} (Inactive).</li>
	 * <li>Decrements {@code InventoryEO.availableQty} by 1.</li>
	 * <li>Records one {@code InventoryTransactionEO} of type
	 * {@code REMOVE_INVENTORY}.</li>
	 * </ul>
	 */
	RemoveInventoryResponseDTO removeInventory(RemoveInventoryRequestDTO request);

	/**
	 * Fetch inventory details with optional filters. At least one filter must be
	 * supplied.
	 * @param productVarId filter by product variant ID (optional)
	 * @param warehouseId filter by warehouse ID (optional)
	 * @param barcode filter by a specific item barcode (optional)
	 */
	InventoryFetchResponseDTO fetchInventory(Long productVarId, Long warehouseId, String barcode);

	/**
	 * Fetch the active inventory record for a given product variant ID.
	 */
	InventoryDTO getActiveInventoryByVariantId(Long variantId);

	/**
	 * Update MFG / EXP / best-before dates on existing inventory detail rows.
	 * <p>
	 * If {@code request.getBatchNo()} is provided, all items in that batch are updated.
	 * Otherwise {@code request.getBarcode()} is used to update a single unit. At least
	 * one of {@code mfd}, {@code expiryDate}, or {@code bestBefore} must be supplied.
	 */
	InventoryUpdateDatesResponseDTO updateInventoryDates(InventoryUpdateDatesDTO request);

	/**
	 * Refresh {@code totalQty} and {@code availableQty} on the inventory record(s) to
	 * match the actual count of rows in the {@code inventory_details} table.
	 * <ul>
	 * <li>{@code totalQty} = count of ALL detail rows for the inventory record.</li>
	 * <li>{@code availableQty} = count of detail rows with {@code status = 'A'}.</li>
	 * </ul>
	 * One {@code InventoryTransactionEO} of type {@code STOCK_REFRESH} is recorded per
	 * inventory record that is refreshed.
	 * @param request optional filters (inventoryId / productVarId / warehouseId). Leave
	 * all null to refresh every inventory record.
	 */
	InventoryRefreshResponseDTO refreshInventoryCounts(InventoryRefreshRequestDTO request);

}
