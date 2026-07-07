package com.user.repository;

import com.user.model.ProductCategoriesEO;
import com.user.model.ProductEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEO, Long> {

	List<ProductEO> findByCategory(ProductCategoriesEO category);

	ProductEO findBySlug(String slug);

	List<ProductEO> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);

	ProductEO findByIdAndStatus(Long id, String status);

	List<ProductEO> findByCategoryAndStatus(ProductCategoriesEO category, String status);

	List<ProductEO> findByStatus(String status);

	List<ProductEO> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndStatus(String name,
			String description, String status);

	// Native queries avoid Hibernate adding "escape ''" which is invalid in PostgreSQL
	// (escape char must be 1 char)
	// :queryPattern and :stemPattern must be passed as "%value%" from the service layer
	@Query(value = "SELECT * FROM products WHERE (LOWER(name) LIKE :queryPattern OR LOWER(description) LIKE :queryPattern OR LOWER(name) LIKE :stemPattern OR LOWER(description) LIKE :stemPattern) AND status = :status",
			nativeQuery = true)
	List<ProductEO> findByNameOrDescriptionAndStatus(@Param("queryPattern") String queryPattern,
			@Param("stemPattern") String stemPattern, @Param("status") String status);

	@Query(value = "SELECT * FROM products WHERE category_id IN (:categoryIds) AND status = :status",
			nativeQuery = true)
	List<ProductEO> findByCategoryIdsAndStatus(@Param("categoryIds") List<Long> categoryIds,
			@Param("status") String status);

	@Query(value = "SELECT * FROM products WHERE category_id IN (:categoryIds) AND (LOWER(name) LIKE :queryPattern OR LOWER(description) LIKE :queryPattern OR LOWER(name) LIKE :stemPattern OR LOWER(description) LIKE :stemPattern) AND status = :status",
			nativeQuery = true)
	List<ProductEO> findByCategoryIdsAndNameOrDescriptionAndStatus(@Param("categoryIds") List<Long> categoryIds,
			@Param("queryPattern") String queryPattern, @Param("stemPattern") String stemPattern,
			@Param("status") String status);

}
