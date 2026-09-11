package com.user.repository;

import com.user.model.ShiprocketOrderStatusHistoryEO;
import com.user.model.ShippingEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiprocketOrderStatusHistoryRepository extends JpaRepository<ShiprocketOrderStatusHistoryEO, Long> {

	List<ShiprocketOrderStatusHistoryEO> findByShipmentOrderByCreatedAtAsc(ShippingEO shipment);

}

