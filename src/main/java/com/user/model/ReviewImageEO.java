package com.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "review_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImageEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id", nullable = false)
	private ProductReviewEO review;

	@Column(name = "image_url", columnDefinition = "text", nullable = false)
	private String imageUrl;

	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		if (this.createdAt == null)
			this.createdAt = OffsetDateTime.now();
	}

}
