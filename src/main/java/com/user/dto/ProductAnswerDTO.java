package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAnswerDTO {

	private Long id;

	private Long questionId;

	private String answeredBy;

	private String answerText;

	private Boolean isAdminAnswer;

	private OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;

}
