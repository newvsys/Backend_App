package com.user.repository;

import com.user.model.InventoryEO;
import com.user.model.ProductVariantEO;
import com.user.model.WarehouseEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface InventoryRepository extends JpaRepository<InventoryEO, Long> {

	InventoryEO findByProductVariant(ProductVariantEO productVariantEO);

	/**
	 * Batch lookup — eliminates N+1 queries when loading inventory for multiple variants.
	 */
	List<InventoryEO> findByProductVariantIn(Collection<ProductVariantEO> variants);

	InventoryEO findByProductVariantAndAvailableQtyGreaterThanEqual(ProductVariantEO productVariantEO,
			Integer availableQty);

	InventoryEO findByProductVariantAndStatus(ProductVariantEO productVariantEO, String status);

	InventoryEO findByProductVariantAndWarehouse(ProductVariantEO productVariant, WarehouseEO warehouse);

	List<InventoryEO> findByWarehouse(WarehouseEO warehouse);

	List<InventoryEO> findByWarehouse_WarehouseId(Long warehouseId);

	InventoryEO findByProductVariant_Id(Long productVarId);

	/**
	 * Returns ALL inventory records for a given product variant (one record per warehouse
	 * that holds that variant).
	 */
	List<InventoryEO> findAllByProductVariant_Id(Long productVarId);

	/**
	 * Batch variant of the above — fetches inventory for ALL the given variant IDs in a
	 * single query, eliminating N separate queries when checking multiple variants. JOIN
	 * FETCH ensures the productVariant proxy is already populated (avoids lazy-load on
	 * map key extraction).
	 */
	@Query("SELECT i FROM InventoryEO i JOIN FETCH i.productVariant WHERE i.productVariant.id IN :variantIds")
	List<InventoryEO> findAllByProductVariantIdIn(@Param("variantIds") Collection<Long> variantIds);

	/**
	 * Same as above but only returns records where availableQty >= 1 (in-stock filter).
	 */
	@Query("SELECT i FROM InventoryEO i JOIN FETCH i.productVariant WHERE i.productVariant.id IN :variantIds AND i.availableQty >= 1")
	List<InventoryEO> findAllByProductVariantIdInAndInStock(@Param("variantIds") Collection<Long> variantIds);

}
