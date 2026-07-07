package com.user.repository;

import com.user.model.ShiprocketOrderLogEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiprocketOrderLogRepository extends JpaRepository<ShiprocketOrderLogEO, Long> {

	List<ShiprocketOrderLogEO> findByShipmentId(Long shipmentId);

	List<ShiprocketOrderLogEO> findByOrderId(Long orderId);

}
