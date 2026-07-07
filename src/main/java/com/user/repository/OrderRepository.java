package com.user.repository;

import com.user.model.OrderEO;
import com.user.model.CustomerEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEO, Long>, JpaSpecificationExecutor<OrderEO> {

	Optional<OrderEO> findByOrderNumber(String orderNumber);

	List<OrderEO> findByCustomer(CustomerEO customer);

	List<OrderEO> findByCustomerAndOrderNumber(CustomerEO customer, String orderNumber);

}
