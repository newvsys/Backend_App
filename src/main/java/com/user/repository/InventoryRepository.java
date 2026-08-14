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
	@Query("SELECT i FROM InventoryEO i JOIN FETCH i.productVariant pv WHERE pv.id IN :variantIds AND i.availableQty >= 1")
	List<InventoryEO> findAllByProductVariantIdInAndInStock(@Param("variantIds") Collection<Long> variantIds);

	/**
	 * Powers the "Current Stock Report" — returns {@code [ProductVariantEO, InventoryEO]}
	 * pairs joined with product, category, and warehouse, with all filters optional (pass
	 * {@code null} to skip a filter). Used to generate/view current stock by product and
	 * product variant, grouped by warehouse.
	 * <p>
	 * The join to {@code InventoryEO} is a LEFT JOIN so a product variant that has no
	 * inventory record at all can still be matched (its {@code InventoryEO} element in
	 * the returned pair will be {@code null}). To avoid flooding the default (unfiltered)
	 * report with the entire catalog, these "no inventory record" rows are ONLY included
	 * when {@code includeZeroStock} is {@code true} — i.e. when the caller's
	 * {@code availableQty} filter expression would match {@code 0} (a missing inventory
	 * record is treated as {@code availableQty = 0}). Without any availableQty filter,
	 * results stay restricted to variants that actually have an inventory record
	 * (original behavior).
	 * <p>
	 * Exactly ONE of {@code availableQtyEq}, {@code availableQtyGt},
	 * {@code availableQtyGte}, {@code availableQtyLt}, {@code availableQtyLte} should be
	 * non-null at a time (parsed from the caller's {@code availableQty} filter
	 * expression, e.g. {@code ">10"}, {@code "<=5"}, or a plain number for exact match).
	 * All may be {@code null} to skip available-quantity filtering entirely.
	 */
	@Query("SELECT pv, i FROM ProductVariantEO pv " + "JOIN FETCH pv.product p " + "JOIN FETCH p.category c "
			+ "LEFT JOIN InventoryEO i ON i.productVariant = pv " + "LEFT JOIN FETCH i.warehouse w "
			+ "WHERE (:warehouseId IS NULL OR w.warehouseId = :warehouseId) "
			+ "AND (:categoryId IS NULL OR c.id = :categoryId) " + "AND (:productId IS NULL OR p.id = :productId) "
			+ "AND (:productVarId IS NULL OR pv.id = :productVarId) "
			+ "AND (:status IS NULL OR i.id IS NULL OR i.status = :status) "
			+ "AND (:availableQtyEq IS NULL OR COALESCE(i.availableQty, 0) = :availableQtyEq) "
			+ "AND (:availableQtyGt IS NULL OR COALESCE(i.availableQty, 0) > :availableQtyGt) "
			+ "AND (:availableQtyGte IS NULL OR COALESCE(i.availableQty, 0) >= :availableQtyGte) "
			+ "AND (:availableQtyLt IS NULL OR COALESCE(i.availableQty, 0) < :availableQtyLt) "
			+ "AND (:availableQtyLte IS NULL OR COALESCE(i.availableQty, 0) <= :availableQtyLte) "
			+ "AND (i.id IS NOT NULL OR :includeZeroStock = true) "
			+ "ORDER BY c.name ASC, p.name ASC, pv.skuCode ASC")
	List<Object[]> findStockReport(@Param("warehouseId") Long warehouseId, @Param("categoryId") Integer categoryId,
			@Param("productId") Integer productId, @Param("productVarId") Integer productVarId,
			@Param("status") String status, @Param("availableQtyEq") Integer availableQtyEq,
			@Param("availableQtyGt") Integer availableQtyGt, @Param("availableQtyGte") Integer availableQtyGte,
			@Param("availableQtyLt") Integer availableQtyLt, @Param("availableQtyLte") Integer availableQtyLte,
			@Param("includeZeroStock") boolean includeZeroStock);

}
