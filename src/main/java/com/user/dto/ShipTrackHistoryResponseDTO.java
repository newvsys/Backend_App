package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipTrackHistoryResponseDTO {

	private String shippingTrackId;

	private List<ShipTrackHistoryDTO> history;

	private String responseMessage;

	private String responseStatus;

}