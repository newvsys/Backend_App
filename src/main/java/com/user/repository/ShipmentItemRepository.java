package com.user.repository;

import com.user.model.ShipmentItemEO;
import com.user.model.ShippingEO;
import com.user.model.OrderItemEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentItemRepository extends JpaRepository<ShipmentItemEO, Long> {

	List<ShipmentItemEO> findByShipment(ShippingEO shipment);

	List<ShipmentItemEO> findByOrderItem(OrderItemEO orderItem);

}
