package com.user.repository;

import com.user.model.WarehouseEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<WarehouseEO, Long> {

	Optional<WarehouseEO> findByWarehouseCodeIgnoreCaseAndStatus(String warehouseCode, String status);

	List<WarehouseEO> findAllByStatus(String status);

	Optional<WarehouseEO> findByWarehouseIdAndStatus(Long warehouseId, String status);

	Optional<WarehouseEO> findByWarehouseNameIgnoreCaseAndStatus(String warehouseName, String status);

}
