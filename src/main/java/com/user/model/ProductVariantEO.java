package com.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "product_variants",
        indexes = {
                @Index(name = "idx_product_variants_product_status_price",
                        columnList = "product_id, status, selling_price"),
                @Index(name = "idx_product_variants_product_id", columnList = "product_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEO product;

	@Column(name = "sku_code", length = 100, unique = true, nullable = false)
	private String skuCode;

	@Column(name = "pack_size", length = 50)
	private String packSize;

	@Column(name = "uom", length = 20)
	private String uom;

	@Column(name = "container_type", length = 50)
	private String containerType;

	@Column(name = "mrp", precision = 10, scale = 2)
	private BigDecimal mrp;

	@Column(name = "selling_price", precision = 10, scale = 2)
	private BigDecimal sellingPrice;

	@Column(name = "currency", length = 20)
	private String currency;

	@Column(name = "status", length = 20)
	private String status;

	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@Column(name = "created_by", updatable = false)
	private String createdBy;

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "length", precision = 10, scale = 2)
	private BigDecimal length;

	@Column(name = "breadth", precision = 10, scale = 2)
	private BigDecimal breadth;

	@Column(name = "height", precision = 10, scale = 2)
	private BigDecimal height;

	@Column(name = "weight", precision = 10, scale = 2)
	private BigDecimal weight;

	@Column(name = "video_url", length = 500)
	private String videoUrl;

	@PrePersist
	protected void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (this.createdAt == null) {
			this.createdAt = now;
		}
		if (this.updatedAt == null) {
			this.updatedAt = now;
		}
		// If service/controller sets these, we keep them. Otherwise, set safe defaults.
		if (this.createdBy == null || this.createdBy.trim().isEmpty()) {
			this.createdBy = "SYSTEM";
		}
		if (this.updatedBy == null || this.updatedBy.trim().isEmpty()) {
			this.updatedBy = this.createdBy;
		}
	}

	@PreUpdate
	protected void onUpdate() {
		OffsetDateTime now = OffsetDateTime.now();
		this.updatedAt = now;
		if (this.updatedBy == null || this.updatedBy.trim().isEmpty()) {
			this.updatedBy = "SYSTEM";
		}

	}

	public double getWeight() {
		return weight != null ? weight.doubleValue() : 0.0;
	}

	public double getVolume() {
		return (length != null ? length.doubleValue() : 0.0) * (breadth != null ? breadth.doubleValue() : 0.0)
				* (height != null ? height.doubleValue() : 0.0);
	}

}