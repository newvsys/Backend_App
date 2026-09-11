package com.user.repository;

import com.user.model.GenerateLabelStatusHistoryEO;
import com.user.model.ShippingEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GenerateLabelStatusHistoryRepository extends JpaRepository<GenerateLabelStatusHistoryEO, Long> {

	List<GenerateLabelStatusHistoryEO> findByShipmentOrderByCreatedAtAsc(ShippingEO shipment);

}

