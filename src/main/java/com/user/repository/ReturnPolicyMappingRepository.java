package com.user.repository;

import com.user.model.ReturnPolicyMappingEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnPolicyMappingRepository extends JpaRepository<ReturnPolicyMappingEO, Long> {

	List<ReturnPolicyMappingEO> findByEntityIdAndEntityType(Long entityId, String entityType);

	/** Batch-fetch all mappings for a set of entity IDs and a single entity type. */
	List<ReturnPolicyMappingEO> findByEntityIdInAndEntityType(List<Long> entityIds, String entityType);

	/** All mappings filtered by entityType only. */
	List<ReturnPolicyMappingEO> findByEntityType(String entityType);

	/** All mappings filtered by entityId only. */
	List<ReturnPolicyMappingEO> findByEntityId(Long entityId);

}
