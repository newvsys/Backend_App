package com.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.OffsetDateTime;

@Entity
@Table(name = "product_categories",
        indexes = { @Index(name = "idx_product_categories_status", columnList = "status") })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategoriesEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 100, nullable = false)
	private String name;

	@Column(length = 300, nullable = false)
	private String href;

	@Column(columnDefinition = "text")
	private String src;

	/** Cloudinary public_id for the category image (used for deletion on update). */
	@Column(name = "src_public_id", columnDefinition = "text")
	private String srcPublicId;

	@Column(columnDefinition = "text")
	private String description;

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
