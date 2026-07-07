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

}
