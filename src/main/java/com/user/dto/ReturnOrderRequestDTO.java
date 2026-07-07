package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnOrderRequestDTO {

	private String orderNumber;

	private Integer userId;

	private String reasonCode;

	private String comments;

}
