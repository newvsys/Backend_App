package com.user.repository;

import com.user.model.ReasonMasterEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReasonMasterRepository extends JpaRepository<ReasonMasterEO, Long> {

	List<ReasonMasterEO> findByType(String type);

	Optional<ReasonMasterEO> findByReasonCode(String reasonCode);

	List<ReasonMasterEO> findAllByStatus(String status);

	List<ReasonMasterEO> findAllByStatusAndType(String status, String type);

	Optional<ReasonMasterEO> findByIdAndStatus(Long id, String status);

}
