package com.user.repository;

import com.user.model.ProductEO;
import com.user.model.ProductReviewEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReviewEO, Long> {

	List<ProductReviewEO> findByProductOrderByCreatedAtDesc(ProductEO product);

	List<ProductReviewEO> findByProductAndStatusOrderByCreatedAtDesc(ProductEO product, String status);

	List<ProductReviewEO> findByStatusOrderByCreatedAtDesc(String status);

	@Query("SELECT AVG(r.rating) FROM ProductReviewEO r WHERE r.product = :product AND r.status = 'APPROVED'")
	Double findAverageRatingByProduct(@Param("product") ProductEO product);

	@Query("SELECT COUNT(r) FROM ProductReviewEO r WHERE r.product = :product AND r.status = 'APPROVED'")
	Long countApprovedByProduct(@Param("product") ProductEO product);

	boolean existsByProductAndCustomer_CustomerId(ProductEO product, Integer customerId);

}
