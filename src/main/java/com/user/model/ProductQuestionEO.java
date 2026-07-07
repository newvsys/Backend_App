package com.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "product_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductQuestionEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEO product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id")
	private CustomerEO customer;

	@Column(name = "question_text", columnDefinition = "text", nullable = false)
	private String questionText;

	/** PENDING / ANSWERED / CLOSED */
	@Column(length = 20)
	@Builder.Default
	private String status = "PENDING";

	@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private List<ProductAnswerEO> answers;

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
		if (this.createdAt == null)
			this.createdAt = now;
		if (this.updatedAt == null)
			this.updatedAt = now;
		if (this.createdBy == null || this.createdBy.isBlank())
			this.createdBy = "SYSTEM";
		if (this.updatedBy == null || this.updatedBy.isBlank())
			this.updatedBy = this.createdBy;
		if (this.status == null)
			this.status = "PENDING";
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
		if (this.updatedBy == null || this.updatedBy.isBlank())
			this.updatedBy = "SYSTEM";
	}

}
