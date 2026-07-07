package com.user.repository;

import com.user.model.OrderCancelRequestEO;
import com.user.model.OrderEO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderCancelRequestRepository extends JpaRepository<OrderCancelRequestEO, Long> {

	java.util.List<OrderCancelRequestEO> findByOrder(OrderEO order);

}
