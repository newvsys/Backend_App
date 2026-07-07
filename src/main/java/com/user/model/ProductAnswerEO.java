package com.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "product_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAnswerEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_id", nullable = false)
	private ProductQuestionEO question;

	/** Name of the person/admin who answered */
	@Column(name = "answered_by", length = 255)
	private String answeredBy;

	@Column(name = "answer_text", columnDefinition = "text", nullable = false)
	private String answerText;

	/** true if answered by admin/seller, false if answered by customer */
	@Column(name = "is_admin_answer")
	@Builder.Default
	private Boolean isAdminAnswer = false;

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
		if (this.isAdminAnswer == null)
			this.isAdminAnswer = false;
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
		if (this.updatedBy == null || this.updatedBy.isBlank())
			this.updatedBy = "SYSTEM";
	}

}
