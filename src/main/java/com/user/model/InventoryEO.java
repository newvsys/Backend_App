package com.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory",
        indexes = {
                @Index(name = "idx_inventory_product_variant_id", columnList = "product_variant_id"),
                @Index(name = "idx_inventory_variant_qty",
                        columnList = "product_variant_id, available_qty")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_variant_id", nullable = false)
	private ProductVariantEO productVariant;

	@Column(nullable = false)
	private Integer totalQty;

	@Column(nullable = false)
	private Integer availableQty;

	private Integer reservedQty;

	@Column(name = "quantity_reserved", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
	@Builder.Default
	private Integer quantityReserved = 0;

	@Column(name = "reorder_level", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
	@Builder.Default
	private Integer reorderLevel = 0;

	@Column(name = "safety_stock", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
	@Builder.Default
	private Integer safetyStock = 0;

	private String whid;

	@Column(name = "status")
	private String status = "A";

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private WarehouseEO warehouse;

	@OneToMany(mappedBy = "inventory", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<InventoryDetailsEO> inventoryDetails = new ArrayList<>();

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
