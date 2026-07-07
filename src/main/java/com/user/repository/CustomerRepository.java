package com.user.repository;

import com.user.model.CustomerEO;
import com.user.model.UserEO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<CustomerEO, Integer> {

	Optional<CustomerEO> findByUser(UserEO user);

	/**
	 * Single-query optimisation for get-customer-details: joins customers → users (status
	 * check) and LEFT JOIN FETCH addresses, replacing three separate round-trips with one
	 * SQL statement.
	 *
	 * Pass {@code Constants.STATUS_ACTIVE} ("A") as the status argument.
	 */
	@Query("SELECT c FROM CustomerEO c LEFT JOIN FETCH c.addresses JOIN c.user u WHERE u.id = :userId AND u.status = :status")
	Optional<CustomerEO> findActiveWithAddressesByUserId(@Param("userId") Long userId, @Param("status") String status);

	// Admin: paginated with optional status filter
	Page<CustomerEO> findByStatus(String status, Pageable pageable);

	// Admin: search by name, email, or mobile.
	// searchPattern must already be lowercase with % wildcards, e.g. "%john%", or null to
	// skip.
	// LOWER() is applied only to DB columns (never to the parameter) to avoid PostgreSQL
	// bytea inference.
	@Query("SELECT c FROM CustomerEO c WHERE " + "(CAST(:status AS string) IS NULL OR c.status = :status) AND "
			+ "(CAST(:searchPattern AS string) IS NULL OR " + "  LOWER(c.firstName) LIKE :searchPattern OR "
			+ "  LOWER(c.email) LIKE :searchPattern OR " + "  c.mobileNumber LIKE :searchPattern)")
	Page<CustomerEO> searchCustomers(@Param("searchPattern") String searchPattern, @Param("status") String status,
			Pageable pageable);

}
