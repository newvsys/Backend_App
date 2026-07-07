package com.user.dto;

import lombok.Data;

/**
 * Response DTO for the label-printing API.
 */
@Data
public class LabelPrintResponseDTO {

	/** "SUCCESS" or "FAILURE" */
	private String status;

	/** Human-readable message */
	private String message;

	/** Relative URL to the generated PDF, e.g. "/labels/labels_1718000000000.pdf" */
	private String pdfUrl;

	/** Number of individual item labels included in the PDF */
	private int labelCount;

}
