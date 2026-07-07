package com.user.repository;

import com.user.model.FlashMessageEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlashMessageRepository extends JpaRepository<FlashMessageEO, Long> {

	/** Find all messages with the given status, ordered by priority asc then id asc. */
	List<FlashMessageEO> findByStatusOrderByPriorityAscIdAsc(String status);

	/** Find all messages ordered by priority asc then id asc. */
	List<FlashMessageEO> findAllByOrderByPriorityAscIdAsc();

}

