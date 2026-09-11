package com.user.repository;

import com.user.model.GenerateAwbStatusHistoryEO;
import com.user.model.ShippingEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GenerateAwbStatusHistoryRepository extends JpaRepository<GenerateAwbStatusHistoryEO, Long> {

	List<GenerateAwbStatusHistoryEO> findByShipmentOrderByCreatedAtAsc(ShippingEO shipment);

}

