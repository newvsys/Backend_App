package com.user.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Response DTO for a Label Configuration master record.
 */
@Data
@Builder
public class LabelConfigResponseDTO {

	private Long id;

	private String configName;

	private String description;

	private Float labelWidthInches;

	private Float labelHeightInches;

	private Boolean showLogo;

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
	 * FSSAI license number stored in this configuration. Printed on each label when
	 * showFssaiCode = true.
	 */
	private String fssaiCode;

	/** Whether the FSSAI license number is shown on the label. */
	private Boolean showFssaiCode;

	/** Number of fields per row on the label (1 = single column, 2 = two columns, …). */
	private Integer columnsPerRow;

	private Boolean isDefault;

	private String status;

	private LocalDateTime createdAt;

	private String createdBy;

	private LocalDateTime updatedAt;

	private String updatedBy;

}
