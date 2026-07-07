package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UomCreateDTO {

	private String uomCode;

	private String uomName;

	private String uomType;

	private String baseUomFlag;

	private String decimalAllowed;

	private String description;

	private String createdBy;

}
