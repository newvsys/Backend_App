package com.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.user.dto.CategoryDTO;
import com.user.model.ProductCategoriesEO;

import java.util.List;

public interface ProductCatRepository extends JpaRepository<ProductCategoriesEO, Long> {

	List<ProductCategoriesEO> findByName(String name);

	List<ProductCategoriesEO> findByStatus(String status);

	List<ProductCategoriesEO> findByNameAndStatus(String name, String status);

	List<ProductCategoriesEO> findByNameContainingIgnoreCaseAndStatus(String name, String status);

	@Query("SELECT c FROM ProductCategoriesEO c WHERE (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND c.status = :status")
	List<ProductCategoriesEO> findByNameOrDescriptionAndStatus(@Param("query") String query,
			@Param("status") String status);

	/**
	 * Projection query: fetches only the 5 columns required for CategoryDTO,
	 * skipping audit columns (createdAt, updatedAt, createdBy, updatedBy).
	 * Results are ordered by name for consistent client rendering.
	 */
	@Query("SELECT new com.user.dto.CategoryDTO(c.id, c.name, c.description, c.href, c.src) "
			+ "FROM ProductCategoriesEO c WHERE c.status = 'A' ORDER BY c.name ASC")
	List<CategoryDTO> findActiveCategories();

}
