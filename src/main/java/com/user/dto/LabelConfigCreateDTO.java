package com.user.dto;

import lombok.Data;

/**
 * Request DTO for creating or updating a Label Configuration master record.
 */
@Data
public class LabelConfigCreateDTO {

	/** Unique human-readable name, e.g. "2x2 Standard", "4x4 Thermal". */
	private String configName;

	private String description;

	/**
	 * Label width in inches (e.g. 2.0, 4.0). Converted to points internally (1 inch = 72
	 * pt).
	 */
	private Float labelWidthInches;

	/** Label height in inches (e.g. 2.0, 6.0). */
	private Float labelHeightInches;

	/** Print a logo at the top of each label. Requires logoPath to be set. */
	private Boolean showLogo;

	/** Absolute filesystem path to the logo image (PNG or JPG). */
	private String logoPath;

	private Boolean showBrandName;

	private Boolean showProductName;

	private Boolean showVariantDetails;

	private Boolean showNetQuantity;

	private Boolean showMfgDate;

	private Boolean showExpDate;

	private Boolean showBatchNo;

	private Boolean showMrp;

	private Boolean showBarcode;

	/**
	 * FSSAI (Food Safety and Standards Authority of India) license number to be printed
	 * on every label (e.g. "12345678901234"). Leave null / blank to omit the field even
	 * when showFssaiCode is true.
	 */
	private String fssaiCode;

	/** Whether to display the FSSAI license number on the label. Defaults to true. */
	private Boolean showFssaiCode;

	/**
	 * Number of fields to place on each row of the label. 1 = single column (default), 2
	 * = two fields per row, etc.
	 */
	private Integer columnsPerRow;

	/**
	 * When true, this config becomes the system default. Any previously-default config
	 * will be automatically demoted.
	 */
	private Boolean isDefault;

	/** ACTIVE or INACTIVE. Defaults to ACTIVE if null. */
	private String status;

	private String createdBy;

	private String updatedBy;

}
