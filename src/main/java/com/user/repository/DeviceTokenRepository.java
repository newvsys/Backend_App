package com.user.repository;

import com.user.model.DeviceTokenEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEO, Long> {

	Optional<DeviceTokenEO> findByToken(String token);

	List<DeviceTokenEO> findByActiveTrue();

	List<DeviceTokenEO> findByRoleAndActiveTrue(String role);

	void deleteByToken(String token);

}

