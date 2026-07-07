package com.user.repository;

import com.user.model.DeliveryChargeEO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryChargeRepository extends JpaRepository<DeliveryChargeEO, Long> {

	List<DeliveryChargeEO> findAllByOrderByPriorityAsc();

	List<DeliveryChargeEO> findByStatusOrderByPriorityAsc(String status);

	/**
	 * Find the best matching active delivery charge rule for a given order subtotal.
	 * Returns the rule with the lowest priority number that covers the given amount.
	 */
	@Query("SELECT d FROM DeliveryChargeEO d " + "WHERE d.status = 'A' " + "AND d.minOrderAmount <= :orderAmount "
			+ "AND (d.maxOrderAmount IS NULL OR d.maxOrderAmount >= :orderAmount) " + "ORDER BY d.priority ASC")
	List<DeliveryChargeEO> findMatchingRules(@Param("orderAmount") BigDecimal orderAmount);

	/**
	 * Optimised variant — pass {@code PageRequest.of(0, 1)} to fetch only the single
	 * top-priority match at the database level instead of loading all matching rows.
	 */
	@Query("SELECT d FROM DeliveryChargeEO d " + "WHERE d.status = 'A' " + "AND d.minOrderAmount <= :orderAmount "
			+ "AND (d.maxOrderAmount IS NULL OR d.maxOrderAmount >= :orderAmount) " + "ORDER BY d.priority ASC")
	List<DeliveryChargeEO> findMatchingRules(@Param("orderAmount") BigDecimal orderAmount, Pageable pageable);

	boolean existsByRuleNameAndStatus(String ruleName, String status);

}
