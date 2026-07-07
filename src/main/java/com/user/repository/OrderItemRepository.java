package com.user.repository;

import com.user.model.OrderEO;
import com.user.model.OrderItemEO;
import com.user.model.ProductVariantEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItemEO, Long> {

	List<OrderItemEO> findByOrder(OrderEO order);

	List<OrderItemEO> findByOrderAndProductVarNameContaining(OrderEO order, String productVarName);

	List<OrderItemEO> findByOrderAndProductVar(OrderEO order, ProductVariantEO productVar);

}
