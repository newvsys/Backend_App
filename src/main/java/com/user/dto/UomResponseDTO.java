package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UomResponseDTO {

	private Long uomId;

	private String uomCode;

	private String uomName;

	private String uomType;

	private String baseUomFlag;

	private String decimalAllowed;

	private String status;

	private String description;

	private LocalDateTime createdAt;

	private String createdBy;

	private LocalDateTime updatedAt;

	private String updatedBy;

}
