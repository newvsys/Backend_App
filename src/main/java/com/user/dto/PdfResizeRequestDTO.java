package com.user.dto;

import lombok.Data;

/**
 * Request DTO for the PDF resize API.
 */
@Data
public class PdfResizeRequestDTO {

	/**
	 * Full URL of the PDF to download and resize. Example:
	 * https://sr-core-cdn.shiprocket.in/label/s/10008561/019eb10e-391e-7b3f-a08f-092de1165b20.pdf
	 */
	private String pdfUrl;

}
