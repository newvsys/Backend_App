package com.user.repository;

import com.user.model.ReturnPolicyConditionEO;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReturnPolicyConditionRepository extends JpaRepository<ReturnPolicyConditionEO, Long> {

	List<ReturnPolicyConditionEO> findByPolicyId(Long policyId);

}
