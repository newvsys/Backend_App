package com.user.repository;

import com.user.model.UserEO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<UserEO, Long> {

	List<UserEO> findByFirstNameOrEmailOrPhone(String firstName, String email, String phone);

	UserEO findByPhone(String phone);

	List<UserEO> findByPhoneAndStatus(String phone, String status);

	List<UserEO> findByEmailAndStatus(String phone, String status);

	// Admin: paginated list with optional status filter
	Page<UserEO> findByStatus(String status, Pageable pageable);

	// Admin: search by name, email, or phone (case-insensitive) with optional status.
	// searchPattern must already be lowercase with % wildcards, e.g. "%john%", or null to
	// skip.
	// LOWER() is applied only to DB columns (never to the parameter) to avoid PostgreSQL
	// bytea inference.
	@Query("SELECT u FROM UserEO u WHERE " + "(CAST(:status AS string) IS NULL OR u.status = :status) AND "
			+ "(CAST(:searchPattern AS string) IS NULL OR " + "  LOWER(u.firstName) LIKE :searchPattern OR "
			+ "  LOWER(u.email) LIKE :searchPattern OR " + "  u.phone LIKE :searchPattern)")
	Page<UserEO> searchUsers(@Param("searchPattern") String searchPattern, @Param("status") String status,
			Pageable pageable);

}
