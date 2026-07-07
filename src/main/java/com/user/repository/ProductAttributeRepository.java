package com.user.repository;

import com.user.model.ProductAttributeEO;
import com.user.model.ProductVariantEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductAttributeRepository extends JpaRepository<ProductAttributeEO, Long> {

	List<ProductAttributeEO> findByProductVar(ProductVariantEO productVariant);

}