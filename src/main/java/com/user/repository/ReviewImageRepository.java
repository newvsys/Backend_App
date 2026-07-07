package com.user.repository;

import com.user.model.ProductReviewEO;
import com.user.model.ReviewImageEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImageEO, Long> {

	List<ReviewImageEO> findByReview(ProductReviewEO review);

	void deleteByReview(ProductReviewEO review);

}
