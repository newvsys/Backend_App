package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UomUpdateDTO {

	private String uomCode;

	private String uomName;

	private String uomType;

	private String baseUomFlag;

	private String decimalAllowed;

	private String status;

	private String description;

	private String updatedBy;

}
