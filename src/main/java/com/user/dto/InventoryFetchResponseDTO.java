package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryFetchResponseDTO {

	private String responseStatus;

	private String responseMessage;

	private Integer totalRecords;

	private List<InventoryInfoDTO> inventories;

}
