package com.user.repository;

import com.user.model.ProductEO;
import com.user.model.ProductQuestionEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductQuestionRepository extends JpaRepository<ProductQuestionEO, Long> {

	List<ProductQuestionEO> findByProductOrderByCreatedAtDesc(ProductEO product);

	List<ProductQuestionEO> findByProductAndStatusOrderByCreatedAtDesc(ProductEO product, String status);

	List<ProductQuestionEO> findByStatusOrderByCreatedAtDesc(String status);

}
