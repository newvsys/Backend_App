package com.user.repository;

import com.user.model.CourierSelectionLogEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourierSelectionLogRepository extends JpaRepository<CourierSelectionLogEO, Long> {

	/** All candidates evaluated for a given internal shipment. */
	List<CourierSelectionLogEO> findByShipmentIdOrderByRankAsc(Long shipmentId);

	/** All candidates evaluated for a given order. */
	List<CourierSelectionLogEO> findByOrderIdOrderByShipmentIdAscRankAsc(Long orderId);

	/** Find the selected (used) courier record for a shipment. */
	java.util.Optional<CourierSelectionLogEO> findByShipmentIdAndIsSelectedTrue(Long shipmentId);

}
