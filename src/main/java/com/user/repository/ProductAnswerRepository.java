package com.user.repository;

import com.user.model.ProductAnswerEO;
import com.user.model.ProductQuestionEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductAnswerRepository extends JpaRepository<ProductAnswerEO, Long> {

	List<ProductAnswerEO> findByQuestionOrderByCreatedAtAsc(ProductQuestionEO question);

}
