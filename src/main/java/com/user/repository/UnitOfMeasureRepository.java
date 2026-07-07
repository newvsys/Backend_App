package com.user.repository;

import com.user.model.UnitOfMeasureEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasureEO, Long> {

	Optional<UnitOfMeasureEO> findByUomIdAndStatus(Long uomId, String status);

	List<UnitOfMeasureEO> findAllByStatus(String status);

	boolean existsByUomCodeAndStatus(String uomCode, String status);

}
