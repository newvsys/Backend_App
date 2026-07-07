package com.user.repository;

import com.user.model.OrderEO;
import com.user.model.PaymentEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEO, Long> {

	// Adjust "order" if the field name in PaymentEO is different
	Optional<PaymentEO> findByOrder(OrderEO order);

	PaymentEO findByPaymentProviderOrderId(String paymentProviderOrderId);

}
