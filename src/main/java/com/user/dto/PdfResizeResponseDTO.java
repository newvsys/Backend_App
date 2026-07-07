package com.user.dto;

import lombok.Data;

/**
 * Response DTO for the PDF resize API.
 */
@Data
public class PdfResizeResponseDTO {

	/** "SUCCESS" or "FAILURE" */
	private String status;

	/** Human-readable message */
	private String message;

	/**
	 * Relative URL to the resized PDF stored in the public folder. Example:
	 * /labels/resized_1718000000000.pdf
	 */
	private String pdfUrl;

	/** Number of pages in the resized PDF */
	private int pageCount;

}
