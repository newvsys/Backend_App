package com.user.repository;

import com.user.model.ProductEO;
import com.user.model.ProductVariantEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEO, Long> {

	List<ProductVariantEO> findByProduct(ProductEO product);

	List<ProductVariantEO> findByProductAndSellingPriceLessThanEqual(ProductEO product, Double price);

	ProductVariantEO findByIdAndStatus(Long id, String status);

	List<ProductVariantEO> findByProductAndSellingPriceLessThanEqualAndStatus(ProductEO product, Double price,
			String status);

	@Query("SELECT v FROM ProductVariantEO v WHERE v.product = :product AND v.sellingPrice >= :minPrice AND v.sellingPrice <= :maxPrice AND v.status = :status")
	List<ProductVariantEO> findByProductAndPriceRangeAndStatus(@Param("product") ProductEO product,
			@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice, @Param("status") String status);

	// ── Batch queries (eliminate N+1 in search) ─────────────────────────────────

	/**
	 * Fetches all active variants for a set of products in one query, ordered by
	 * selling price ASC so the cheapest variant is always first.
	 */
	@Query("SELECT v FROM ProductVariantEO v WHERE v.product IN :products AND v.status = :status ORDER BY v.sellingPrice ASC NULLS LAST")
	List<ProductVariantEO> findByProductInAndStatus(@Param("products") Collection<ProductEO> products,
			@Param("status") String status);

	/**
	 * Same as above but with a price-range filter.
	 */
	@Query("SELECT v FROM ProductVariantEO v WHERE v.product IN :products AND v.sellingPrice >= :minPrice AND v.sellingPrice <= :maxPrice AND v.status = :status ORDER BY v.sellingPrice ASC NULLS LAST")
	List<ProductVariantEO> findByProductInAndPriceRangeAndStatus(@Param("products") Collection<ProductEO> products,
			@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice, @Param("status") String status);

	/**
	 * Same as above but with an upper-price filter only.
	 */
	@Query("SELECT v FROM ProductVariantEO v WHERE v.product IN :products AND v.sellingPrice <= :maxPrice AND v.status = :status ORDER BY v.sellingPrice ASC NULLS LAST")
	List<ProductVariantEO> findByProductInAndSellingPriceLessThanEqualAndStatus(
			@Param("products") Collection<ProductEO> products, @Param("maxPrice") Double maxPrice,
			@Param("status") String status);

	/**
	 * Powers the "Out of Stock Report" — returns every active product variant that is
	 * out of stock, i.e. it has NO {@code InventoryEO} record with {@code availableQty >
	 * 0} in ANY warehouse (either no inventory record exists at all, or every existing
	 * record has {@code availableQty <= 0}). All filters are optional (pass {@code null}
	 * to skip a filter).
	 */
	@Query("SELECT v FROM ProductVariantEO v " + "JOIN FETCH v.product p " + "JOIN FETCH p.category c "
			+ "WHERE v.status = 'A' " + "AND (:categoryId IS NULL OR c.id = :categoryId) "
			+ "AND (:productId IS NULL OR p.id = :productId) " + "AND (:productVarId IS NULL OR v.id = :productVarId) "
			+ "AND v.id NOT IN (SELECT i.productVariant.id FROM InventoryEO i WHERE i.availableQty > 0) "
			+ "ORDER BY c.name ASC, p.name ASC, v.skuCode ASC")
	List<ProductVariantEO> findOutOfStockVariants(@Param("categoryId") Integer categoryId,
			@Param("productId") Integer productId, @Param("productVarId") Integer productVarId);

}
