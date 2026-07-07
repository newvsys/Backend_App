package com.user.repository;

import com.user.model.LabelConfigEO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LabelConfigRepository extends JpaRepository<LabelConfigEO, Long> {

	/** All configs ordered by name, optionally filtered by status. */
	List<LabelConfigEO> findByStatusOrderByConfigNameAsc(String status);

	/** All configs regardless of status, ordered by name. */
	List<LabelConfigEO> findAllByOrderByConfigNameAsc();

	/** Find the currently active default config. */
	Optional<LabelConfigEO> findByIsDefaultTrueAndStatus(String status);

	/** Check if a config name already exists (case-insensitive). */
	Optional<LabelConfigEO> findByConfigNameIgnoreCase(String configName);

}
