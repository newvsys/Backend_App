package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO wrapping a list of flash / marquee messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashMessageListResponseDTO {

	private String responseStatus;

	private String responseMessage;

	private Integer totalCount;

	private List<FlashMessageResponseDTO> flashMessages;

}

