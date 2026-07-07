package com.user.controller;

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
import com.user.dto.ResponseDTO;
import com.user.dto.RestoreInventoryRequestDTO;
import com.user.dto.RestoreInventoryResponseDTO;
import com.user.service.InventoryService;
import com.user.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

	private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

	@Autowired
	private InventoryService inventoryService;

	/**
	 * POST /api/inventory/load
	 * <p>
	 * Loads (adds) stock for a product variant into a warehouse. Creates / updates the
	 * inventory record, records a transaction of type LOAD_INVENTORY, and generates a
	 * unique barcode for every unit.
	 *
	 * <pre>
	 * Request body:
	 * {
	 *   "productVarId": 101,
	 *   "qty": 50,
	 *   "whid": "WH001",
	 *   "mfd": "2026-01-10",
	 *   "bestBefore": "2027-01-10",
	 *   "expiryDate": "2027-06-10"
	 * }
	 * </pre>
	 */
	@PostMapping("/load")
	public ResponseEntity<?> loadInventory(@RequestBody LoadInventoryRequestDTO request) {
		logger.info("Load inventory request — productVarId={}, qty={}, whid={}", request.getProductVarId(),
				request.getQty(), request.getWhid());
		try {
			LoadInventoryResponseDTO response = inventoryService.loadInventory(request);
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				return ResponseEntity.badRequest().body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Unexpected error in loadInventory", e);
			ResponseDTO error = new ResponseDTO();
			error.setResponseStatus(Constants.FAILURE_STATUS);
			error.setResponseMessage("Failed to load inventory: " + e.getMessage());
			return ResponseEntity.status(500).body(error);
		}
	}

	/**
	 * POST /api/inventory/restore
	 * <p>
	 * Restores a single inventory unit by barcode. Sets the item status back to
	 * AVAILABLE, increments availableQty on the parent inventory record, and records a
	 * RESTORE_INVENTORY transaction.
	 *
	 * <pre>
	 * Request body:
	 * {
	 *   "barcode": "BC-101-1718099234567-5"
	 * }
	 * </pre>
	 */
	@PostMapping("/restore")
	public ResponseEntity<?> restoreInventory(@RequestBody RestoreInventoryRequestDTO request) {
		logger.info("Restore inventory request — barcode={}", request.getBarcode());
		try {
			RestoreInventoryResponseDTO response = inventoryService.restoreInventory(request);
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				return ResponseEntity.badRequest().body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Unexpected error in restoreInventory", e);
			ResponseDTO error = new ResponseDTO();
			error.setResponseStatus(Constants.FAILURE_STATUS);
			error.setResponseMessage("Failed to restore inventory: " + e.getMessage());
			return ResponseEntity.status(500).body(error);
		}
	}

	/**
	 * POST /api/inventory/remove
	 * <p>
	 * Removes a single inventory unit by barcode. Sets the item status to {@code I}
	 * (Inactive), decrements availableQty on the parent inventory record, and records a
	 * REMOVE_INVENTORY transaction.
	 *
	 * <pre>
	 * Request body:
	 * {
	 *   "barcode": "BC-101-1718099234567-5"
	 * }
	 * </pre>
	 */
	@PostMapping("/remove")
	public ResponseEntity<?> removeInventory(@RequestBody RemoveInventoryRequestDTO request) {
		logger.info("Remove inventory request — barcode={}", request.getBarcode());
		try {
			RemoveInventoryResponseDTO response = inventoryService.removeInventory(request);
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				return ResponseEntity.badRequest().body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Unexpected error in removeInventory", e);
			ResponseDTO error = new ResponseDTO();
			error.setResponseStatus(Constants.FAILURE_STATUS);
			error.setResponseMessage("Failed to remove inventory: " + e.getMessage());
			return ResponseEntity.status(500).body(error);
		}
	}

	/**
	 * GET /api/inventory/variant/{variant_id}
	 * <p>
	 * Fetch the active inventory record for a given product variant ID.
	 */
	@GetMapping("/variant/{variant_id}")
	public ResponseEntity<?> getActiveInventoryByVariantId(@PathVariable("variant_id") Long variantId) {
		logger.info("Received getActiveInventoryByVariantId request for variantId={}", variantId);
		try {
			InventoryDTO inventory = inventoryService.getActiveInventoryByVariantId(variantId);
			if (inventory == null) {
				logger.warn("No active inventory found for variantId={}", variantId);
				ResponseDTO notFound = new ResponseDTO();
				notFound.setResponseStatus(Constants.FAILURE_STATUS);
				notFound.setResponseMessage("No active inventory found for variantId: " + variantId);
				return ResponseEntity.status(404).body(notFound);
			}
			return ResponseEntity.ok(inventory);
		}
		catch (Exception e) {
			logger.error("Error fetching active inventory for variantId={}: {}", variantId, e.getMessage(), e);
			ResponseDTO error = new ResponseDTO();
			error.setResponseStatus(Constants.FAILURE_STATUS);
			error.setResponseMessage("Failed to fetch inventory: " + e.getMessage());
			return ResponseEntity.status(500).body(error);
		}
	}

	/**
	 * GET /api/inventory/details
	 * <p>
	 * Fetch inventory records and their per-unit item details. At least one query
	 * parameter must be provided.
	 *
	 * <ul>
	 * <li>{@code productVarId} — filter by product variant ID</li>
	 * <li>{@code warehouseId} — filter by warehouse ID</li>
	 * <li>{@code barcode} — fetch the single item matching this barcode</li>
	 * </ul>
	 *
	 * Examples: <pre>
	 *   GET /api/inventory/details?productVarId=101
	 *   GET /api/inventory/details?warehouseId=2
	 *   GET /api/inventory/details?productVarId=101&warehouseId=2
	 *   GET /api/inventory/details?barcode=BC-101-1718099234567-5
	 * </pre>
	 */
	@GetMapping("/details")
	public ResponseEntity<?> fetchInventory(@RequestParam(required = false) Long productVarId,
			@RequestParam(required = false) Long warehouseId, @RequestParam(required = false) String barcode) {
		logger.info("Fetch inventory — productVarId={}, warehouseId={}, barcode={}", productVarId, warehouseId,
				barcode);
		try {
			InventoryFetchResponseDTO response = inventoryService.fetchInventory(productVarId, warehouseId, barcode);
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				return ResponseEntity.badRequest().body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Unexpected error in fetchInventory", e);
			ResponseDTO error = new ResponseDTO();
			error.setResponseStatus(Constants.FAILURE_STATUS);
			error.setResponseMessage("Failed to fetch inventory: " + e.getMessage());
			return ResponseEntity.status(500).body(error);
		}
	}

	/**
	 * PUT /api/inventory/details/dates
	 * <p>
	 * Update the MFG date, EXP date, and/or best-before date on existing inventory detail
	 * rows. Use this to fix records that were loaded without dates (which would otherwise
	 * show as "N/A" on printed labels).
	 *
	 * <p>
	 * Supply <b>either</b> {@code batchNo} (patches every unit in that batch) <b>or</b>
	 * {@code barcode} (patches one specific unit). {@code batchNo} wins if both are
	 * provided. At least one date field must be present.
	 *
	 * <pre>
	 * Request body:
	 * {
	 *   "batchNo":    "BATCH-101-1718099234567",
	 *   "mfd":        "2026-01-10",
	 *   "bestBefore": "2027-01-10",
	 *   "expiryDate": "2027-06-10"
	 * }
	 * </pre>
	 */
	@PutMapping("/details/dates")
	public ResponseEntity<?> updateInventoryDates(@RequestBody InventoryUpdateDatesDTO request) {
		logger.info("Update inventory dates — batchNo={}, barcode={}", request.getBatchNo(), request.getBarcode());
		try {
			InventoryUpdateDatesResponseDTO response = inventoryService.updateInventoryDates(request);
			if (Constants.FAILURE_STATUS.equals(response.getStatus())) {
				return ResponseEntity.badRequest().body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Unexpected error in updateInventoryDates", e);
			InventoryUpdateDatesResponseDTO error = new InventoryUpdateDatesResponseDTO();
			error.setStatus(Constants.FAILURE_STATUS);
			error.setMessage("Failed to update inventory dates: " + e.getMessage());
			return ResponseEntity.status(500).body(error);
		}
	}

	/**
	 * POST /api/inventory/refresh-counts
	 * <p>
	 * Resets {@code totalQty} and {@code availableQty} in the {@code inventory} table to
	 * match the actual stock available in the {@code inventory_details} table, and
	 * records one {@code STOCK_REFRESH} transaction per refreshed record.
	 *
	 * <ul>
	 * <li>{@code totalQty} ← count of ALL rows in inventory_details for the record.</li>
	 * <li>{@code availableQty} ← count of rows in inventory_details with
	 * {@code status = 'A'}.</li>
	 * </ul>
	 *
	 * <p>
	 * All request body fields are optional — leave all null to refresh every inventory
	 * record in the system.
	 *
	 * <pre>
	 * Request body (all optional):
	 * {
	 *   "inventoryId":  5,          // refresh only this inventory record
	 *   "productVarId": 101,        // refresh all records for this product variant
	 *   "warehouseId":  2,          // refresh all records for this warehouse
	 *   "refreshedBy":  "admin"     // audit trail identifier
	 * }
	 * </pre>
	 */
	@PostMapping("/refresh-counts")
	public ResponseEntity<?> refreshInventoryCounts(@RequestBody(required = false) InventoryRefreshRequestDTO request) {
		logger.info("Refresh inventory counts — inventoryId={}, productVarId={}, warehouseId={}",
				request != null ? request.getInventoryId() : null, request != null ? request.getProductVarId() : null,
				request != null ? request.getWarehouseId() : null);
		try {
			InventoryRefreshResponseDTO response = inventoryService.refreshInventoryCounts(request);
			if (Constants.FAILURE_STATUS.equals(response.getStatus())) {
				return ResponseEntity.badRequest().body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Unexpected error in refreshInventoryCounts", e);
			ResponseDTO error = new ResponseDTO();
			error.setResponseStatus(Constants.FAILURE_STATUS);
			error.setResponseMessage("Failed to refresh inventory counts: " + e.getMessage());
			return ResponseEntity.status(500).body(error);
		}
	}

}
