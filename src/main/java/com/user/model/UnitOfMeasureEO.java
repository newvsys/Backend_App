package com.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "unit_of_measure")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitOfMeasureEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "uom_id")
	private Long uomId;

	/**
	 * Short unique code for the UOM, e.g. "KG", "PCS", "MTR".
	 */
	@Column(name = "uom_code", nullable = false, unique = true, length = 20)
	private String uomCode;

	/**
	 * Full descriptive name, e.g. "Kilogram", "Pieces", "Metre".
	 */
	@Column(name = "uom_name", nullable = false, length = 100)
	private String uomName;

	/**
	 * Category / dimension of the UOM, e.g. "WEIGHT", "LENGTH", "VOLUME", "COUNT".
	 */
	@Column(name = "uom_type", nullable = false, length = 50)
	private String uomType;

	/**
	 * 'Y' if this UOM is the base unit for its type; 'N' otherwise.
	 */
	@Column(name = "base_uom_flag", length = 1, columnDefinition = "CHAR(1)")
	private String baseUomFlag;

	/**
	 * 'Y' if fractional quantities are permitted; 'N' for whole-number-only UOMs.
	 */
	@Column(name = "decimal_allowed", length = 1, columnDefinition = "CHAR(1)")
	private String decimalAllowed;

	/**
	 * Lifecycle status: 'ACTIVE' or 'INACTIVE'.
	 */
	@Column(name = "status", length = 20)
	private String status;

	@Column(name = "description", length = 255)
	private String description;

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
		if (status == null) {
			status = "ACTIVE";
		}
		if (baseUomFlag == null) {
			baseUomFlag = "N";
		}
		if (decimalAllowed == null) {
			decimalAllowed = "N";
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

}
