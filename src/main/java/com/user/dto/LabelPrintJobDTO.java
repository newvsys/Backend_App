package com.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Response DTO for a single label-print job record.
 */
@Data
@Builder
public class LabelPrintJobDTO {

	/** Auto-generated job ID. Use this to reprint via /labels/jobs/{jobId}/reprint. */
	private Long jobId;

	/** Brand name used in this print job. */
	private String brandName;

	/** Batch number used to resolve items (null if job was barcode-based). */
	private String batchNo;

	/**
	 * Comma-separated barcodes supplied in the original request (null if batch-based).
	 */
	private String barcodes;

	/** All barcode values that were printed, comma-separated. */
	private String resolvedBarcodes;

	/** Number of labels in the generated PDF. */
	private int labelCount;

	/** Relative URL of the generated PDF (e.g. "/labels/labels_1718000000000.pdf"). */
	private String pdfUrl;

	/** Whether the PDF file still exists on disk. */
	private boolean pdfFileExists;

	/** "SUCCESS" or "FAILURE". */
	private String status;

	/** Error message when status = "FAILURE". */
	private String errorMessage;

	/** Timestamp when the job was created. */
	private OffsetDateTime printedAt;

	/** User / system that triggered the job. */
	private String printedBy;

	/** ID of the label configuration used (null = built-in defaults). */
	private Long labelConfigId;

	/** Name of the label configuration used (null = built-in defaults). */
	private String labelConfigName;

}
