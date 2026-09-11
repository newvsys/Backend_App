package com.user.repository;

import com.user.model.ShippingEO;
import com.user.model.TrackShipmentStatusHistoryEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackShipmentStatusHistoryRepository extends JpaRepository<TrackShipmentStatusHistoryEO, Long> {

	List<TrackShipmentStatusHistoryEO> findByShipmentOrderByCreatedAtAsc(ShippingEO shipment);

}

