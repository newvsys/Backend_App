package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductQAListResponseDTO {

	private Integer productId;

	private Long totalQuestions;

	private List<ProductQuestionDTO> questions;

}
