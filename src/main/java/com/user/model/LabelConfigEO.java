package com.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * MASTER table for label print configurations. Each row defines label dimensions, which
 * fields to show, and whether to print a logo.
 *
 * Table: label_config
 */
@Entity
@Table(name = "label_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabelConfigEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * Unique human-readable name for this configuration, e.g. "2x2 Standard", "4x4
	 * Thermal".
	 */
	@Column(name = "config_name", nullable = false, unique = true, length = 100)
	private String configName;

	@Column(name = "description", length = 500)
	private String description;

	/** Label width in inches (converted to pt = inches × 72). e.g. 2.0 → 144 pt. */
	@Column(name = "label_width_inches", nullable = false)
	private Float labelWidthInches;

	/** Label height in inches. e.g. 2.0 → 144 pt, 4.0 → 288 pt. */
	@Column(name = "label_height_inches", nullable = false)
	private Float labelHeightInches;

	/** Whether to print the brand logo at the top of the label. */
	@Column(name = "show_logo", nullable = false)
	private Boolean showLogo;

	/**
	 * Absolute filesystem path to the logo image file. Used only when showLogo = true.
	 * Supports PNG / JPG.
	 */
	@Column(name = "logo_path", length = 500)
	private String logoPath;

	@Column(name = "show_brand_name", nullable = false)
	private Boolean showBrandName;

	@Column(name = "show_product_name", nullable = false)
	private Boolean showProductName;

	@Column(name = "show_variant_details", nullable = false)
	private Boolean showVariantDetails;

	@Column(name = "show_net_quantity", nullable = false)
	private Boolean showNetQuantity;

	@Column(name = "show_mfg_date", nullable = false)
	private Boolean showMfgDate;

	@Column(name = "show_exp_date", nullable = false)
	private Boolean showExpDate;

	@Column(name = "show_batch_no", nullable = false)
	private Boolean showBatchNo;

	@Column(name = "show_mrp", nullable = false)
	private Boolean showMrp;

	@Column(name = "show_barcode", nullable = false)
	private Boolean showBarcode;

	/**
	 * FSSAI (Food Safety and Standards Authority of India) license number to be printed
	 * on every label (e.g. "FSSAI Lic. No: 12345678901234"). Leave blank / null to omit.
	 */
	@Column(name = "fssai_code", length = 100)
	private String fssaiCode;

	/** Whether to print the FSSAI license number on the label. */
	@Column(name = "show_fssai_code", nullable = false, columnDefinition = "boolean DEFAULT true")
	private Boolean showFssaiCode;

	/**
	 * When true this config is auto-selected when no labelConfigId is supplied in the
	 * /labels/print request. Only one config should be the default at a time.
	 */
	@Column(name = "is_default", nullable = false)
	private Boolean isDefault;

	/**
	 * Number of fields to place on each row of the label. 1 = single column (default), 2
	 * = two fields per row, 3 = three, etc.
	 */
	@Column(name = "columns_per_row", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 1")
	private Integer columnsPerRow;

	/** ACTIVE or INACTIVE. Inactive configs cannot be selected. */
	@Column(name = "status", length = 20)
	private String status;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "created_by", length = 50)
	private String createdBy;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "updated_by", length = 50)
	private String updatedBy;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		if (status == null)
			status = "ACTIVE";
		if (showLogo == null)
			showLogo = false;
		if (showBrandName == null)
			showBrandName = true;
		if (showProductName == null)
			showProductName = true;
		if (showVariantDetails == null)
			showVariantDetails = true;
		if (showNetQuantity == null)
			showNetQuantity = true;
		if (showMfgDate == null)
			showMfgDate = true;
		if (showExpDate == null)
			showExpDate = true;
		if (showBatchNo == null)
			showBatchNo = true;
		if (showMrp == null)
			showMrp = true;
		if (showBarcode == null)
			showBarcode = true;
		if (showFssaiCode == null)
			showFssaiCode = true;
		if (isDefault == null)
			isDefault = false;
		if (columnsPerRow == null)
			columnsPerRow = 1;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

}
