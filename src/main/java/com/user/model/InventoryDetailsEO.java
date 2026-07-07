package com.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDetailsEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	/**
	 * Reference to the parent inventory record (product variant + warehouse). Multiple
	 * detail records (one per unit) share the same inventory parent.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inventory_id", nullable = false)
	private InventoryEO inventory;

	/**
	 * Batch number — same value for all units received in the same batch. e.g. 50 items
	 * added together share one batch_no.
	 */
	@Column(name = "batch_no", nullable = false, length = 100)
	private String batchNo;

	/**
	 * Unique barcode for each individual physical item/unit.
	 */
	@Column(name = "barcode", nullable = false, unique = true, length = 150)
	private String barcode;

	/**
	 * Manufactured date (MFD).
	 */
	@Column(name = "mfd")
	private LocalDate mfd;

	/**
	 * Best before use date.
	 */
	@Column(name = "best_before")
	private LocalDate bestBefore;

	/**
	 * Expiry / expiration date.
	 */
	@Column(name = "expiry_date")
	private LocalDate expiryDate;

	/**
	 * Item status: AVAILABLE, RESERVED, SOLD, DAMAGED, EXPIRED.
	 */
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private String status = "A";

	@Column(name = "created_at", nullable = false, updatable = false)
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
		if (this.createdBy == null || this.createdBy.trim().isEmpty()) {
			this.createdBy = "SYSTEM";
		}
		if (this.updatedBy == null || this.updatedBy.trim().isEmpty()) {
			this.updatedBy = this.createdBy;
		}
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
		if (this.updatedBy == null || this.updatedBy.trim().isEmpty()) {
			this.updatedBy = "SYSTEM";
		}
	}

}
