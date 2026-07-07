package com.user.repository;

import com.user.model.ShippingEO;
import com.user.model.OrderEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShippingRepository extends JpaRepository<ShippingEO, Long> {

	List<ShippingEO> findByOrder(OrderEO order);

	ShippingEO findByTrackingNumber(String trackingNumber);

	List<ShippingEO> findByShipmentStatus(String shipmentStatus);

	Optional<ShippingEO> findByShipShipmentId(Integer shipShipmentId);

	Optional<ShippingEO> findByAwb(String awb);

	@Query("SELECT s FROM ShippingEO s WHERE (:status IS NULL OR s.shipmentStatus = :status) ORDER BY s.createdAt DESC")
	List<ShippingEO> findAllByOptionalStatus(@Param("status") String status);

	@Query("SELECT s FROM ShippingEO s JOIN s.order o WHERE (:status IS NULL OR s.shipmentStatus = :status) AND (:orderNumber IS NULL OR o.orderNumber = :orderNumber) ORDER BY s.createdAt DESC")
	List<ShippingEO> findAllByOptionalStatusAndOrderNumber(@Param("status") String status,
			@Param("orderNumber") String orderNumber);

}
