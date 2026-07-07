package com.user.repository;

import com.user.model.LabelPrintJobEO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LabelPrintJobRepository extends JpaRepository<LabelPrintJobEO, Long> {

	/** Find all jobs for a given batch number, newest first. */
	List<LabelPrintJobEO> findByBatchNoOrderByPrintedAtDesc(String batchNo);

	/**
	 * Find all jobs where the resolved barcode list contains the given barcode value.
	 * Uses LIKE search on the comma-separated resolvedBarcodes column.
	 */
	@Query("SELECT j FROM LabelPrintJobEO j WHERE j.resolvedBarcodes LIKE %:barcode%  ORDER BY j.printedAt DESC")
	List<LabelPrintJobEO> findByResolvedBarcodesContaining(@Param("barcode") String barcode);

	/** Paginated list of all jobs, newest first. */
	Page<LabelPrintJobEO> findAllByOrderByPrintedAtDesc(Pageable pageable);

	/** Paginated list filtered by batchNo, newest first. */
	Page<LabelPrintJobEO> findByBatchNoOrderByPrintedAtDesc(String batchNo, Pageable pageable);

}
