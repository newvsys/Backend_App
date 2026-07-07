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

import java.time.OffsetDateTime;

@Entity
@Table(name = "products",
        indexes = {
                @Index(name = "idx_products_status", columnList = "status"),
                @Index(name = "idx_products_category_id", columnList = "category_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false)
	private ProductCategoriesEO category;

	@Column(length = 255, nullable = false)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Column(length = 100, unique = true)
	private String slug;

	@Column(name = "status")
	private String status = "A";

	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@Column(name = "created_by", updatable = false)
	private String createdBy;

	@Column(name = "updated_by")
	private String updatedBy;

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

}
