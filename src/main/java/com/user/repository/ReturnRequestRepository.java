package com.user.repository;

import com.user.model.OrderEO;
import com.user.model.ReturnRequestEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequestEO, Long> {

	Optional<ReturnRequestEO> findByReturnId(String returnId);

	List<ReturnRequestEO> findByOrder(OrderEO order);

	List<ReturnRequestEO> findByOrderOrderNumber(String orderNumber);

	List<ReturnRequestEO> findByStatus(String status);

	List<ReturnRequestEO> findByOrderOrderNumberAndStatus(String orderNumber, String status);

}
