package com.user.service;

import com.user.dto.InventoryDTO;
import com.user.dto.InventoryFetchResponseDTO;
import com.user.dto.InventoryInfoDTO;
import com.user.dto.InventoryItemDTO;
import com.user.dto.InventoryRefreshRequestDTO;
import com.user.dto.InventoryRefreshResponseDTO;
import com.user.dto.InventoryRefreshResponseDTO.InventoryRefreshDetailDTO;
import com.user.dto.InventoryUpdateDatesDTO;
import com.user.dto.InventoryUpdateDatesResponseDTO;
import com.user.dto.LoadInventoryRequestDTO;
import com.user.dto.LoadInventoryResponseDTO;
import com.user.dto.RemoveInventoryRequestDTO;
import com.user.dto.RemoveInventoryResponseDTO;
import com.user.dto.RestoreInventoryRequestDTO;
import com.user.dto.RestoreInventoryResponseDTO;
import com.user.model.InventoryDetailsEO;
import com.user.model.InventoryEO;
import com.user.model.InventoryTransactionEO;
import com.user.model.ProductVariantEO;
import com.user.model.WarehouseEO;
import com.user.repository.InventoryDetailsRepository;
import com.user.repository.InventoryRepository;
import com.user.repository.InventoryTransactionRepository;
import com.user.repository.ProductVariantRepository;
import com.user.repository.WarehouseRepository;
import com.user.utility.Constants;
import com.user.utility.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

	private static final Logger logger = LoggerFactory.getLogger(InventoryServiceImpl.class);

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private InventoryDetailsRepository inventoryDetailsRepository;

	@Autowired
	private InventoryTransactionRepository inventoryTransactionRepository;

	@Autowired
	private ProductVariantRepository productVariantRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Override
	@Transactional
	public LoadInventoryResponseDTO loadInventory(LoadInventoryRequestDTO request) {
		LoadInventoryResponseDTO response = new LoadInventoryResponseDTO();

		try {
			// ── 1. Validate input ──────────────────────────────────────────────
			if (request.getProductVarId() == null || request.getQty() == null || request.getWhid() == null
					|| request.getWhid().isBlank()) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("productVarId, qty and whid are required.");
				return response;
			}
			if (request.getQty() <= 0) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("qty must be greater than zero.");
				return response;
			}

			// ── 2. Resolve ProductVariant ──────────────────────────────────────
			ProductVariantEO variant = productVariantRepository.findByIdAndStatus(request.getProductVarId(),
					Constants.STATUS_ACTIVE);
			if (variant == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Product variant not found or inactive: " + request.getProductVarId());
				return response;
			}

			// ── 3. Resolve Warehouse ───────────────────────────────────────────
			Optional<WarehouseEO> warehouseOpt = warehouseRepository
				.findByWarehouseCodeIgnoreCaseAndStatus(request.getWhid(), Constants.STATUS_ACTIVE);
			if (warehouseOpt.isEmpty()) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Warehouse not found or inactive: " + request.getWhid());
				return response;
			}
			WarehouseEO warehouse = warehouseOpt.get();

			// ── 4. Create or update InventoryEO ───────────────────────────────
			InventoryEO inventory = inventoryRepository.findByProductVariantAndWarehouse(variant, warehouse);

			int quantityBefore;
			if (inventory == null) {
				// First time loading stock for this variant+warehouse
				inventory = new InventoryEO();
				inventory.setProductVariant(variant);
				inventory.setWarehouse(warehouse);
				inventory.setWhid(request.getWhid());
				inventory.setTotalQty(0);
				inventory.setAvailableQty(0);
				inventory.setStatus(Constants.STATUS_ACTIVE);
				quantityBefore = 0;
			}
			else {
				quantityBefore = inventory.getAvailableQty() != null ? inventory.getAvailableQty() : 0;
			}

			int quantityAfter = quantityBefore + request.getQty();
			inventory.setTotalQty((inventory.getTotalQty() != null ? inventory.getTotalQty() : 0) + request.getQty());
			inventory.setAvailableQty(quantityAfter);
			inventory = inventoryRepository.save(inventory);
			logger.info("Inventory saved — id={}, availableQty={}", inventory.getId(), inventory.getAvailableQty());

			// ── 5. Generate batch number ───────────────────────────────────────
			long batchId = System.currentTimeMillis();
			String batchNo = "BATCH-" + request.getProductVarId() + "-" + batchId;

			// ── 6. Record InventoryTransaction ────────────────────────────────
			InventoryTransactionEO transaction = InventoryTransactionEO.builder()
				.inventory(inventory)
				.variant(variant)
				.warehouse(warehouse)
				.transactionType("LOAD_INVENTORY")
				.referenceType("BATCH")
				.referenceId(batchId)
				.quantity(request.getQty())
				.quantityBefore(quantityBefore)
				.quantityAfter(quantityAfter)
				.remarks(batchNo)
				.build();
			inventoryTransactionRepository.save(transaction);
			logger.info("Inventory transaction recorded — batchNo={}, qty={}", batchNo, request.getQty());

			// ── 7. Generate per-unit barcodes and InventoryDetails rows ────────
			List<InventoryDetailsEO> detailsList = new ArrayList<>();
			List<String> barcodes = new ArrayList<>();

			for (int i = 1; i <= request.getQty(); i++) {
				String barcode = "BC-" + request.getProductVarId() + "-" + batchId + "-" + i;
				barcodes.add(barcode);

				InventoryDetailsEO detail = InventoryDetailsEO.builder()
					.inventory(inventory)
					.batchNo(batchNo)
					.barcode(barcode)
					.mfd(request.getMfd())
					.bestBefore(request.getBestBefore())
					.expiryDate(request.getExpiryDate())
					.status("A")
					.build();
				detailsList.add(detail);
			}
			inventoryDetailsRepository.saveAll(detailsList);
			logger.info("Inventory details inserted — {} records for batchNo={}", detailsList.size(), batchNo);

			// ── 8. Build response ──────────────────────────────────────────────
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Inventory loaded successfully.");
			response.setBatchNo(batchNo);
			response.setBatchId(batchId);
			response.setInventoryId(Long.valueOf(inventory.getId()));
			response.setQuantityLoaded(request.getQty());
			response.setTotalAvailableQty(quantityAfter);
			response.setBarcodes(barcodes);

		}
		catch (Exception e) {
			logger.error("Error loading inventory", e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to load inventory: " + e.getMessage());
		}

		return response;
	}

	@Override
	@Transactional
	public RestoreInventoryResponseDTO restoreInventory(RestoreInventoryRequestDTO request) {
		RestoreInventoryResponseDTO response = new RestoreInventoryResponseDTO();

		try {
			// ── 1. Validate input ──────────────────────────────────────────────
			if (request.getBarcode() == null || request.getBarcode().isBlank()) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Barcode is required.");
				return response;
			}

			// ── 2. Find InventoryDetailsEO by barcode ──────────────────────────
			InventoryDetailsEO detail = inventoryDetailsRepository.findByBarcode(request.getBarcode());
			if (detail == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("No inventory item found for barcode: " + request.getBarcode());
				return response;
			}

			String previousStatus = detail.getStatus();

			// ── 3. Guard: skip if already AVAILABLE ───────────────────────────
			if ("A".equalsIgnoreCase(previousStatus)) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Item is already AVAILABLE. No changes made.");
				response.setBarcode(request.getBarcode());
				response.setPreviousStatus(previousStatus);
				return response;
			}

			// ── 4. Restore item status to AVAILABLE ───────────────────────────
			detail.setStatus("A");
			inventoryDetailsRepository.save(detail);
			logger.info("Barcode {} status restored from {} to AVAILABLE", request.getBarcode(), previousStatus);

			// ── 5. Update InventoryEO — increment availableQty and totalQty by 1 ──
			InventoryEO inventory = detail.getInventory();
			int quantityBefore = inventory.getAvailableQty() != null ? inventory.getAvailableQty() : 0;
			int quantityAfter = quantityBefore + 1;
			inventory.setAvailableQty(quantityAfter);
			inventory.setTotalQty((inventory.getTotalQty() != null ? inventory.getTotalQty() : 0) + 1);
			inventory = inventoryRepository.save(inventory);
			logger.info("Inventory id={} availableQty/totalQty updated {} -> {}", inventory.getId(), quantityBefore,
					quantityAfter);

			// ── 6. Record InventoryTransaction ────────────────────────────────
			InventoryTransactionEO transaction = InventoryTransactionEO.builder()
				.inventory(inventory)
				.variant(inventory.getProductVariant())
				.warehouse(inventory.getWarehouse())
				.transactionType("RESTORE_INVENTORY")
				.referenceType("BARCODE")
				.quantity(1)
				.quantityBefore(quantityBefore)
				.quantityAfter(quantityAfter)
				.remarks("Restored barcode: " + request.getBarcode() + " | prev status: " + previousStatus)
				.build();
			InventoryTransactionEO savedTx = inventoryTransactionRepository.save(transaction);
			logger.info("Restore transaction recorded — transactionId={}", savedTx.getTransactionId());

			// ── 7. Build response ──────────────────────────────────────────────
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Inventory item restored successfully.");
			response.setBarcode(request.getBarcode());
			response.setPreviousStatus(previousStatus);
			response.setInventoryId(Long.valueOf(inventory.getId()));
			response.setAvailableQtyBefore(quantityBefore);
			response.setAvailableQtyAfter(quantityAfter);
			response.setTransactionId(savedTx.getTransactionId());

		}
		catch (Exception e) {
			logger.error("Error restoring inventory for barcode: {}", request.getBarcode(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to restore inventory: " + e.getMessage());
		}

		return response;
	}

	@Override
	@Transactional
	public RemoveInventoryResponseDTO removeInventory(RemoveInventoryRequestDTO request) {
		RemoveInventoryResponseDTO response = new RemoveInventoryResponseDTO();

		try {
			// ── 1. Validate input ──────────────────────────────────────────────
			if (request.getBarcode() == null || request.getBarcode().isBlank()) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Barcode is required.");
				return response;
			}

			// ── 2. Find InventoryDetailsEO by barcode ──────────────────────────
			InventoryDetailsEO detail = inventoryDetailsRepository.findByBarcode(request.getBarcode());
			if (detail == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("No inventory item found for barcode: " + request.getBarcode());
				return response;
			}

			String previousStatus = detail.getStatus();

			// ── 3. Guard: skip if already inactive ────────────────────────────
			if (Constants.STATUS_INACTIVE.equalsIgnoreCase(previousStatus)) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Item is already removed (inactive). No changes made.");
				response.setBarcode(request.getBarcode());
				response.setPreviousStatus(previousStatus);
				return response;
			}

			// ── 4. Mark item as Inactive ───────────────────────────────────────
			detail.setStatus(Constants.STATUS_INACTIVE);
			inventoryDetailsRepository.save(detail);
			logger.info("Barcode {} status changed from {} to I (Inactive)", request.getBarcode(), previousStatus);

			// Both availableQty and totalQty are managed by the order placement process.
			InventoryEO inventory = detail.getInventory();
			int quantityBefore = inventory.getAvailableQty() != null ? inventory.getAvailableQty() : 0;
			int quantityAfter = quantityBefore;
			inventory = inventoryRepository.save(inventory);
			logger.info("Inventory id={} — qty unchanged on remove (availableQty={})", inventory.getId(),
					quantityBefore);

			// ── 6. Record InventoryTransaction ────────────────────────────────
			InventoryTransactionEO transaction = InventoryTransactionEO.builder()
				.inventory(inventory)
				.variant(inventory.getProductVariant())
				.warehouse(inventory.getWarehouse())
				.transactionType("REMOVE_INVENTORY")
				.referenceType("BARCODE")
				.quantity(1)
				.quantityBefore(quantityBefore)
				.quantityAfter(quantityAfter)
				.remarks("Removed barcode: " + request.getBarcode() + " | prev status: " + previousStatus)
				.build();
			InventoryTransactionEO savedTx = inventoryTransactionRepository.save(transaction);
			logger.info("Remove transaction recorded — transactionId={}", savedTx.getTransactionId());

			// ── 7. Build response ──────────────────────────────────────────────
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Inventory item removed successfully.");
			response.setBarcode(request.getBarcode());
			response.setPreviousStatus(previousStatus);
			response.setInventoryId(Long.valueOf(inventory.getId()));
			response.setAvailableQtyBefore(quantityBefore);
			response.setAvailableQtyAfter(quantityAfter);
			response.setTransactionId(savedTx.getTransactionId());

		}
		catch (Exception e) {
			logger.error("Error removing inventory for barcode: {}", request.getBarcode(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to remove inventory: " + e.getMessage());
		}

		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public InventoryFetchResponseDTO fetchInventory(Long productVarId, Long warehouseId, String barcode) {
		InventoryFetchResponseDTO response = new InventoryFetchResponseDTO();

		try {
			// Require at least one filter
			if (productVarId == null && warehouseId == null && (barcode == null || barcode.isBlank())) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("At least one filter (productVarId, warehouseId or barcode) is required.");
				return response;
			}

			List<InventoryEO> inventoryList = new ArrayList<>();

			// ── Filter by barcode (most specific) ─────────────────────────────
			if (barcode != null && !barcode.isBlank()) {
				InventoryDetailsEO detail = inventoryDetailsRepository.findByBarcode(barcode);
				if (detail == null) {
					response.setResponseStatus(Constants.FAILURE_STATUS);
					response.setResponseMessage("No item found for barcode: " + barcode);
					return response;
				}
				// Build response for just this one item
				InventoryEO inv = detail.getInventory();
				InventoryInfoDTO info = mapToInventoryInfo(inv, List.of(mapToItemDTO(detail)));
				response.setResponseStatus(Constants.SUCCESS_STATUS);
				response.setResponseMessage("Inventory fetched successfully.");
				response.setTotalRecords(1);
				response.setInventories(List.of(info));
				return response;
			}

			// ── Filter by productVarId + optional warehouseId ──────────────────
			if (productVarId != null) {
				InventoryEO inv;
				if (warehouseId != null) {
					// Both filters: narrow to specific inventory record
					List<InventoryEO> byWarehouse = inventoryRepository.findByWarehouse_WarehouseId(warehouseId);
					inv = byWarehouse.stream()
						.filter(i -> i.getProductVariant() != null
								&& productVarId.equals(Long.valueOf(i.getProductVariant().getId())))
						.findFirst()
						.orElse(null);
				}
				else {
					inv = inventoryRepository.findByProductVariant_Id(productVarId);
				}
				if (inv != null) {
					inventoryList.add(inv);
				}
			}
			else {
				// Only warehouseId provided
				inventoryList = inventoryRepository.findByWarehouse_WarehouseId(warehouseId);
			}

			if (inventoryList.isEmpty()) {
				response.setResponseStatus(Constants.SUCCESS_STATUS);
				response.setResponseMessage("No inventory records found for the given filters.");
				response.setTotalRecords(0);
				response.setInventories(new ArrayList<>());
				return response;
			}

			// ── Map each inventory record with all its detail items ────────────
			List<InventoryInfoDTO> infoDTOs = new ArrayList<>();
			for (InventoryEO inv : inventoryList) {
				List<InventoryDetailsEO> details = inventoryDetailsRepository
					.findByInventory_Id(inv.getId().longValue());
				List<InventoryItemDTO> itemDTOs = details.stream().map(this::mapToItemDTO).collect(Collectors.toList());
				infoDTOs.add(mapToInventoryInfo(inv, itemDTOs));
			}

			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Inventory fetched successfully.");
			response.setTotalRecords(infoDTOs.size());
			response.setInventories(infoDTOs);

		}
		catch (Exception e) {
			logger.error("Error fetching inventory", e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to fetch inventory: " + e.getMessage());
		}

		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public InventoryDTO getActiveInventoryByVariantId(Long variantId) {
		try {
			ProductVariantEO variant = productVariantRepository.findById(variantId).orElse(null);
			if (variant == null) {
				logger.error("Product variant not found for id: {}", variantId);
				return null;
			}
			InventoryEO inventory = inventoryRepository.findByProductVariantAndStatus(variant, Constants.STATUS_ACTIVE);
			if (inventory == null) {
				logger.warn("No active inventory found for variantId={}", variantId);
				return null;
			}
			return UserMapper.toInventoryDTO(inventory);
		}
		catch (Exception e) {
			logger.error("Error fetching active inventory for variantId={}: {}", variantId, e.getMessage(), e);
			return null;
		}
	}

	// ── Private mapping helpers ────────────────────────────────────────────────

	private InventoryInfoDTO mapToInventoryInfo(InventoryEO inv, List<InventoryItemDTO> items) {
		return InventoryInfoDTO.builder()
			.inventoryId(inv.getId() != null ? Long.valueOf(inv.getId()) : null)
			.productVarId(inv.getProductVariant() != null ? Long.valueOf(inv.getProductVariant().getId()) : null)
			.productVariantName(inv.getProductVariant() != null ? inv.getProductVariant().getSkuCode() : null)
			.warehouseId(inv.getWarehouse() != null ? inv.getWarehouse().getWarehouseId() : null)
			.warehouseCode(inv.getWarehouse() != null ? inv.getWarehouse().getWarehouseCode() : null)
			.warehouseName(inv.getWarehouse() != null ? inv.getWarehouse().getWarehouseName() : null)
			.totalQty(inv.getTotalQty())
			.availableQty(inv.getAvailableQty())
			.reservedQty(inv.getReservedQty())
			.quantityReserved(inv.getQuantityReserved())
			.reorderLevel(inv.getReorderLevel())
			.safetyStock(inv.getSafetyStock())
			.status(inv.getStatus())
			.items(items)
			.build();
	}

	private InventoryItemDTO mapToItemDTO(InventoryDetailsEO detail) {
		return InventoryItemDTO.builder()
			.id(detail.getId())
			.barcode(detail.getBarcode())
			.batchNo(detail.getBatchNo())
			.status(detail.getStatus())
			.mfd(detail.getMfd())
			.bestBefore(detail.getBestBefore())
			.expiryDate(detail.getExpiryDate())
			.createdAt(detail.getCreatedAt())
			.updatedAt(detail.getUpdatedAt())
			.build();
	}

	// ─── Update MFG / EXP dates ───────────────────────────────────────────────

	@Override
	@Transactional
	public InventoryUpdateDatesResponseDTO updateInventoryDates(InventoryUpdateDatesDTO request) {
		InventoryUpdateDatesResponseDTO response = new InventoryUpdateDatesResponseDTO();

		try {
			boolean hasBatch = request.getBatchNo() != null && !request.getBatchNo().isBlank();
			boolean hasBarcode = request.getBarcode() != null && !request.getBarcode().isBlank();

			if (!hasBatch && !hasBarcode) {
				response.setStatus(Constants.FAILURE_STATUS);
				response.setMessage("Either batchNo or barcode must be provided.");
				return response;
			}

			if (request.getMfd() == null && request.getExpiryDate() == null && request.getBestBefore() == null) {
				response.setStatus(Constants.FAILURE_STATUS);
				response.setMessage("At least one of mfd, expiryDate, or bestBefore must be provided.");
				return response;
			}

			List<InventoryDetailsEO> items;
			if (hasBatch) {
				items = inventoryDetailsRepository.findByBatchNo(request.getBatchNo().trim());
				if (items.isEmpty()) {
					response.setStatus(Constants.FAILURE_STATUS);
					response.setMessage("No inventory details found for batchNo: " + request.getBatchNo().trim());
					return response;
				}
			}
			else {
				InventoryDetailsEO single = inventoryDetailsRepository.findByBarcode(request.getBarcode().trim());
				if (single == null) {
					response.setStatus(Constants.FAILURE_STATUS);
					response.setMessage("No inventory detail found for barcode: " + request.getBarcode().trim());
					return response;
				}
				items = List.of(single);
			}

			for (InventoryDetailsEO item : items) {
				if (request.getMfd() != null)
					item.setMfd(request.getMfd());
				if (request.getBestBefore() != null)
					item.setBestBefore(request.getBestBefore());
				if (request.getExpiryDate() != null)
					item.setExpiryDate(request.getExpiryDate());
			}
			inventoryDetailsRepository.saveAll(items);

			String scope = hasBatch ? "batchNo=" + request.getBatchNo() : "barcode=" + request.getBarcode();
			logger.info("Inventory dates updated — {}, count={}", scope, items.size());

			response.setStatus(Constants.SUCCESS_STATUS);
			response.setMessage("Inventory dates updated successfully.");
			response.setUpdatedCount(items.size());

		}
		catch (Exception e) {
			logger.error("Error updating inventory dates: {}", e.getMessage(), e);
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage("Failed to update inventory dates: " + e.getMessage());
		}

		return response;
	}

	// ─── Refresh Inventory Counts ─────────────────────────────────────────────

	@Override
	@Transactional
	public InventoryRefreshResponseDTO refreshInventoryCounts(InventoryRefreshRequestDTO request) {
		InventoryRefreshResponseDTO response = new InventoryRefreshResponseDTO();

		try {
			String refreshedBy = (request != null && request.getRefreshedBy() != null
					&& !request.getRefreshedBy().isBlank()) ? request.getRefreshedBy().trim() : "SYSTEM";

			// ── 1. Resolve the set of inventory records to refresh ─────────────
			List<InventoryEO> targets = new ArrayList<>();

			if (request != null && request.getInventoryId() != null) {
				// Specific inventory record by primary key
				inventoryRepository.findById(request.getInventoryId()).ifPresent(targets::add);
				if (targets.isEmpty()) {
					response.setStatus(Constants.FAILURE_STATUS);
					response.setMessage("Inventory record not found for id: " + request.getInventoryId());
					return response;
				}

			}
			else if (request != null && request.getProductVarId() != null) {
				// All records for a product variant
				targets = inventoryRepository.findAllByProductVariant_Id(request.getProductVarId());
				if (targets.isEmpty()) {
					response.setStatus(Constants.FAILURE_STATUS);
					response.setMessage("No inventory records found for productVarId: " + request.getProductVarId());
					return response;
				}

			}
			else if (request != null && request.getWarehouseId() != null) {
				// All records for a warehouse
				targets = inventoryRepository.findByWarehouse_WarehouseId(request.getWarehouseId());
				if (targets.isEmpty()) {
					response.setStatus(Constants.FAILURE_STATUS);
					response.setMessage("No inventory records found for warehouseId: " + request.getWarehouseId());
					return response;
				}

			}
			else {
				// No filter → refresh everything
				targets = inventoryRepository.findAll();
				if (targets.isEmpty()) {
					response.setStatus(Constants.SUCCESS_STATUS);
					response.setMessage("No inventory records exist in the system.");
					response.setRefreshedCount(0);
					response.setDetails(new ArrayList<>());
					return response;
				}
			}

			// ── 2. Process each inventory record ──────────────────────────────
			List<InventoryRefreshDetailDTO> details = new ArrayList<>();

			for (InventoryEO inv : targets) {
				int prevAvailable = inv.getAvailableQty() != null ? inv.getAvailableQty() : 0;
				int prevTotal = inv.getTotalQty() != null ? inv.getTotalQty() : 0;

				// Count from inventory_details table
				int newTotal = (int) inventoryDetailsRepository.countByInventory(inv);
				int newAvailable = (int) inventoryDetailsRepository.countByInventoryAndStatus(inv, "A");

				// Update the inventory header
				inv.setTotalQty(newTotal);
				inv.setAvailableQty(newAvailable);
				inv.setUpdatedBy(refreshedBy);
				InventoryEO savedInv = inventoryRepository.save(inv);

				// Record one STOCK_REFRESH transaction
				int delta = newAvailable - prevAvailable;
				String remarks = String.format(
						"Stock refresh by %s. Before: availableQty=%d, totalQty=%d. After: availableQty=%d, totalQty=%d. Delta: %+d",
						refreshedBy, prevAvailable, prevTotal, newAvailable, newTotal, delta);

				InventoryTransactionEO tx = InventoryTransactionEO.builder()
					.inventory(savedInv)
					.variant(savedInv.getProductVariant())
					.warehouse(savedInv.getWarehouse())
					.transactionType("STOCK_REFRESH")
					.referenceType("INVENTORY_REFRESH")
					.referenceId(Long.valueOf(savedInv.getId()))
					.quantity(delta)
					.quantityBefore(prevAvailable)
					.quantityAfter(newAvailable)
					.remarks(remarks)
					.build();
				InventoryTransactionEO savedTx = inventoryTransactionRepository.save(tx);

				logger.info("Inventory id={} refreshed — availableQty: {} → {}, totalQty: {} → {}, txId={}",
						savedInv.getId(), prevAvailable, newAvailable, prevTotal, newTotal, savedTx.getTransactionId());

				// Build per-record detail
				InventoryRefreshDetailDTO detail = new InventoryRefreshDetailDTO();
				detail.setInventoryId(Long.valueOf(savedInv.getId()));
				detail.setProductVariantId(savedInv.getProductVariant() != null
						? Long.valueOf(savedInv.getProductVariant().getId()) : null);
				detail.setProductVariantName(
						savedInv.getProductVariant() != null ? savedInv.getProductVariant().getSkuCode() : null);
				detail
					.setWarehouseId(savedInv.getWarehouse() != null ? savedInv.getWarehouse().getWarehouseId() : null);
				detail.setWarehouseName(
						savedInv.getWarehouse() != null ? savedInv.getWarehouse().getWarehouseName() : null);
				detail.setPreviousAvailableQty(prevAvailable);
				detail.setPreviousTotalQty(prevTotal);
				detail.setNewAvailableQty(newAvailable);
				detail.setNewTotalQty(newTotal);
				detail.setAvailableQtyDelta(delta);
				detail.setTransactionId(savedTx.getTransactionId());

				details.add(detail);
			}

			response.setStatus(Constants.SUCCESS_STATUS);
			response.setMessage("Inventory counts refreshed successfully for " + details.size() + " record(s).");
			response.setRefreshedCount(details.size());
			response.setDetails(details);

		}
		catch (Exception e) {
			logger.error("Error refreshing inventory counts: {}", e.getMessage(), e);
			response.setStatus(Constants.FAILURE_STATUS);
			response.setMessage("Failed to refresh inventory counts: " + e.getMessage());
		}

		return response;
	}

}
