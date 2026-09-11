package com.user.repository;

import com.user.model.RequestPickupStatusHistoryEO;
import com.user.model.ShippingEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestPickupStatusHistoryRepository extends JpaRepository<RequestPickupStatusHistoryEO, Long> {

	List<RequestPickupStatusHistoryEO> findByShipmentOrderByCreatedAtAsc(ShippingEO shipment);

}

