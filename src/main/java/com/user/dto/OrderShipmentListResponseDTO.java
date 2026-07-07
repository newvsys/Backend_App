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
public class OrderShipmentListResponseDTO {

	private int totalCount;

	private List<OrderShipmentDetailDTO> orders;

}
