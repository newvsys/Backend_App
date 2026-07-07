package com.user.repository;

import com.user.model.ShipmentTrackingHistoryEO;
import com.user.model.ShippingEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentTrackingHistoryRepository extends JpaRepository<ShipmentTrackingHistoryEO, Long> {

	List<ShipmentTrackingHistoryEO> findByShipment(ShippingEO shipment);

	List<ShipmentTrackingHistoryEO> findByShipmentOrderByUpdatedAtAsc(ShippingEO shipment);

}
