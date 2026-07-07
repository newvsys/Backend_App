package com.user.repository;

import com.user.model.OTPStoreEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OTPStoreRepository extends JpaRepository<OTPStoreEO, Long> {

	List<OTPStoreEO> findByIdentifierAndStatusAndPurpose(String identifier, String status, String purpose);

	Optional<OTPStoreEO> findByIdentifierAndStatusAndPurposeAndOtpHash(String identifier, String status, String purpose,
			String otpHash);

}
