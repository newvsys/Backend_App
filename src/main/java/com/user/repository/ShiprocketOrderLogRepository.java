package com.user.repository;

import com.user.model.ShiprocketOrderLogEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShiprocketOrderLogRepository extends JpaRepository<ShiprocketOrderLogEO, Long> {

	List<ShiprocketOrderLogEO> findByShipmentId(Long shipmentId);

	List<ShiprocketOrderLogEO> findByOrderId(Long orderId);

	Optional<ShiprocketOrderLogEO> findFirstByShipmentIdOrderByCreatedAtDesc(Long shipmentId);

	/** Latest FAILED step log for a shipment — used to surface a detailed failure reason to the UI. */
	Optional<ShiprocketOrderLogEO> findFirstByShipmentIdAndStatusOrderByCreatedAtDesc(Long shipmentId, String status);

}
