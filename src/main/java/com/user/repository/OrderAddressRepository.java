package com.user.repository;

import com.user.model.OrderAddressEO;
import com.user.model.OrderEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderAddressRepository extends JpaRepository<OrderAddressEO, Integer> {

	Optional<OrderAddressEO> findByOrder(OrderEO order);

	List<OrderAddressEO> findAllByOrder(OrderEO order);

}
