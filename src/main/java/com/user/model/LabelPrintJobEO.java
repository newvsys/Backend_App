package com.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Persists every label-PDF generation request so jobs can be re-downloaded or re-printed
 * later.
 *
 * Table: label_print_jobs
 */
@Entity
@Table(name = "label_print_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabelPrintJobEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/** Brand name supplied in the original request (printed on every label). */
	@Column(name = "brand_name", nullable = false, length = 255)
	private String brandName;

	/**
	 * Batch number from the original request. NULL when the job was triggered by
	 * individual barcodes.
	 */
	@Column(name = "batch_no", length = 100)
	private String batchNo;

	/**
	 * Comma-separated barcode values from the original request. NULL when the job was
	 * triggered by batchNo.
	 */
	@Column(name = "barcodes", length = 2000)
	private String barcodes;

	/**
	 * All barcode values that were actually printed — comma-separated. Populated from
	 * resolved InventoryDetails rows. Used to find which job(s) printed a specific
	 * barcode.
	 */
	@Column(name = "resolved_barcodes", length = 4000)
	private String resolvedBarcodes;

	/** Number of individual labels included in the generated PDF. */
	@Column(name = "label_count", nullable = false)
	private int labelCount;

	/** Relative URL to the PDF, e.g. "/labels/labels_1718000000000.pdf". */
	@Column(name = "pdf_url", length = 500)
	private String pdfUrl;

	/** Full filesystem path to the PDF file (used for re-download existence check). */
	@Column(name = "pdf_file_path", length = 1000)
	private String pdfFilePath;

	/**
	 * ID of the label_config used for this job. NULL if the built-in defaults were used.
	 */
	@Column(name = "label_config_id")
	private Long labelConfigId;

	/** "SUCCESS" or "FAILURE". */
	@Column(name = "status", nullable = false, length = 20)
	private String status;

	/** Optional: error message when status = FAILURE. */
	@Column(name = "error_message", length = 1000)
	private String errorMessage;

	@Column(name = "printed_at", nullable = false, updatable = false)
	private OffsetDateTime printedAt;

	@Column(name = "printed_by", length = 100)
	private String printedBy;

	@PrePersist
	protected void onCreate() {
		if (this.printedAt == null) {
			this.printedAt = OffsetDateTime.now();
		}
		if (this.printedBy == null || this.printedBy.trim().isEmpty()) {
			this.printedBy = "SYSTEM";
		}
	}

}
