package com.user.dto;

import lombok.Data;

/**
 * Request DTO for the label-printing API.
 *
 * Rules: - brandName : mandatory - batchNo : optional — takes first preference over
 * barcodes - barcodes : optional — comma-separated barcode values (used only when batchNo
 * is absent) At least one of batchNo or barcodes must be provided.
 */
@Data
public class LabelPrintRequestDTO {

	/** Brand name to print on every label (mandatory). */
	private String brandName;

	/** Batch / lot number — first preference for item resolution. */
	private String batchNo;

	/**
	 * One or more inventory barcode values, comma-separated. Used only when batchNo is
	 * not supplied. e.g. "BC001,BC002,BC003"
	 */
	private String barcodes;

	/**
	 * Optional: ID of a label_config master record. When provided, the label dimensions,
	 * field visibility, and logo setting from that config are applied to every label in
	 * the generated PDF. When omitted, the system uses the config flagged as
	 * isDefault=true. If no default config exists, built-in 2"×2" defaults are used.
	 */
	private Long labelConfigId;

}
