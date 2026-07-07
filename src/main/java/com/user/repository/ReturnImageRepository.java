package com.user.repository;

import com.user.model.ReturnImageEO;
import com.user.model.ReturnRequestEO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnImageRepository extends JpaRepository<ReturnImageEO, Long> {

	List<ReturnImageEO> findByReturnRequest(ReturnRequestEO returnRequest);

}
