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

@Entity
@Table(name = "product_images",
        indexes = { @Index(name = "idx_product_images_product_var_id", columnList = "product_var_id") })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "product_var_id", referencedColumnName = "id", nullable = false)
	private ProductVariantEO productVar;

	@Column(columnDefinition = "text")
	private String image;

	@Column(columnDefinition = "text")
	private String imagePath;

	@Column(name = "status")
	private String status = "A";

	@Column(name = "is_main_image", length = 1)
	private String isMainImage = "N";

}
