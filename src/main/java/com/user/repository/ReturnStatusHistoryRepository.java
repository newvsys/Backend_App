package com.user.repository;

import com.user.model.ReturnRequestEO;
import com.user.model.ReturnStatusHistoryEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnStatusHistoryRepository extends JpaRepository<ReturnStatusHistoryEO, Long> {

	List<ReturnStatusHistoryEO> findByReturnRequest(ReturnRequestEO returnRequest);

}
