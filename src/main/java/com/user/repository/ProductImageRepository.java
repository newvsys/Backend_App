package com.user.repository;

import com.user.model.ProductImageEO;
import com.user.model.ProductVariantEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImageEO, Long> {

	List<ProductImageEO> findByProductVar(ProductVariantEO productVariant);

	/**
	 * Batch lookup — eliminates N+1 queries when loading images for multiple variants at
	 * once.
	 */
	List<ProductImageEO> findByProductVarIn(Collection<ProductVariantEO> variants);

}
