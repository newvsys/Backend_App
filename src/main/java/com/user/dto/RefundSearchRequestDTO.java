package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundSearchRequestDTO {

	private List<String> status;

	private LocalDateTime createdFrom;

	private LocalDateTime createdTo;

	private String orderNumber;

}
