package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAnswerCreateDTO {

	private Long questionId;

	private String answeredBy;

	private String answerText;

	/** true = admin/seller answer, false = community/customer answer */
	private Boolean isAdminAnswer;

}
