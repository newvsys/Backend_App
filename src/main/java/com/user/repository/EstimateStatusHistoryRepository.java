package com.user.repository;

import com.user.model.EstimateStatusHistoryEO;
import com.user.model.ShippingEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstimateStatusHistoryRepository extends JpaRepository<EstimateStatusHistoryEO, Long> {

	List<EstimateStatusHistoryEO> findByShipmentOrderByCreatedAtAsc(ShippingEO shipment);

}

