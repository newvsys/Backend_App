package com.user.repository;

import com.user.model.InventoryDetailsEO;
import com.user.model.InventoryEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryDetailsRepository extends JpaRepository<InventoryDetailsEO, Long> {

	List<InventoryDetailsEO> findByInventory(InventoryEO inventory);

	List<InventoryDetailsEO> findByInventoryAndBatchNo(InventoryEO inventory, String batchNo);

	List<InventoryDetailsEO> findByBatchNo(String batchNo);

	InventoryDetailsEO findByBarcode(String barcode);

	List<InventoryDetailsEO> findByBarcodeIn(List<String> barcodes);

	List<InventoryDetailsEO> findByInventory_Id(Long inventoryId);

	/**
	 * Count all detail rows for a given inventory record (all statuses). Used to derive
	 * the refreshed totalQty.
	 */
	long countByInventory(InventoryEO inventory);

	/**
	 * Count detail rows for a given inventory record filtered by status. Pass status="A"
	 * to derive the refreshed availableQty.
	 */
	long countByInventoryAndStatus(InventoryEO inventory, String status);

}
