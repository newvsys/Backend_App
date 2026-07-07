package com.user.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "return_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnImageEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "return_id", nullable = false)
	private ReturnRequestEO returnRequest;

	@Column(name = "image_url", nullable = false, length = 500)
	private String imageUrl;

	@Column(name = "image_type", length = 50)
	private String imageType; // DAMAGE, PACKAGE, LABEL

}
