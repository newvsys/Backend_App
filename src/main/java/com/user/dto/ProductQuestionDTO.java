package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductQuestionDTO {

	private Long id;

	private Integer productId;

	private Integer customerId;

	private String customerName;

	private String questionText;

	private String status;

	private List<ProductAnswerDTO> answers;

	private OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;

}
