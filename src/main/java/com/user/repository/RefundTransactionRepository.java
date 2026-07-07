package com.user.repository;

import com.user.model.RefundTransactionEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefundTransactionRepository extends JpaRepository<RefundTransactionEO, Long> {

	Optional<RefundTransactionEO> findByRefundReference(String refundReference);

	@Query("""
			SELECT r
			FROM RefundTransactionEO r
			WHERE (:statuses IS NULL OR r.status IN :statuses)
			  AND (:orderId IS NULL OR r.orderId = :orderId)
			ORDER BY r.createdAt DESC
			""")
	List<RefundTransactionEO> findRefundsWithoutDates(@Param("statuses") List<String> statuses,
			@Param("orderId") Long orderId);

	@Query("""
			SELECT r
			FROM RefundTransactionEO r
			WHERE (:statuses IS NULL OR r.status IN :statuses)
			  AND r.createdAt >= :createdFrom
			  AND (:orderId IS NULL OR r.orderId = :orderId)
			ORDER BY r.createdAt DESC
			""")
	List<RefundTransactionEO> findRefundsFromDate(@Param("statuses") List<String> statuses,
			@Param("createdFrom") LocalDateTime createdFrom, @Param("orderId") Long orderId);

	@Query("""
			SELECT r
			FROM RefundTransactionEO r
			WHERE (:statuses IS NULL OR r.status IN :statuses)
			  AND r.createdAt <= :createdTo
			  AND (:orderId IS NULL OR r.orderId = :orderId)
			ORDER BY r.createdAt DESC
			""")
	List<RefundTransactionEO> findRefundsToDate(@Param("statuses") List<String> statuses,
			@Param("createdTo") LocalDateTime createdTo, @Param("orderId") Long orderId);

	@Query("""
			SELECT r
			FROM RefundTransactionEO r
			WHERE (:statuses IS NULL OR r.status IN :statuses)
			  AND r.createdAt >= :createdFrom
			  AND r.createdAt <= :createdTo
			  AND (:orderId IS NULL OR r.orderId = :orderId)
			ORDER BY r.createdAt DESC
			""")
	List<RefundTransactionEO> findRefundsBetweenDates(@Param("statuses") List<String> statuses,
			@Param("createdFrom") LocalDateTime createdFrom, @Param("createdTo") LocalDateTime createdTo,
			@Param("orderId") Long orderId);

}
