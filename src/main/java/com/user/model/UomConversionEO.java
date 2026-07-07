package com.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "uom_conversion",
		uniqueConstraints = @UniqueConstraint(name = "uq_uom_conversion_from_to",
				columnNames = { "from_uom_id", "to_uom_id" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UomConversionEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "conversion_id")
	private Long conversionId;

	/**
	 * The source UOM for this conversion rule.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "from_uom_id", nullable = false, foreignKey = @ForeignKey(name = "fk_uom_conversion_from"))
	private UnitOfMeasureEO fromUom;

	/**
	 * The target UOM for this conversion rule.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "to_uom_id", nullable = false, foreignKey = @ForeignKey(name = "fk_uom_conversion_to"))
	private UnitOfMeasureEO toUom;

	/**
	 * Multiply a quantity in fromUom by this factor to obtain the equivalent quantity in
	 * toUom. e.g. 1 KG -> 1000 G => conversionFactor = 1000.000000
	 */
	@Column(name = "conversion_factor", nullable = false, precision = 18, scale = 6)
	private BigDecimal conversionFactor;

	/**
	 * Lifecycle status: 'ACTIVE' or 'INACTIVE'.
	 */
	@Column(name = "status", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
	private String status;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		if (status == null) {
			status = "ACTIVE";
		}
	}

}
