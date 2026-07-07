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
public class CartonListResponseDTO {

	private List<ResponseCreateCartonDTO> cartons;

	private String responseStatus;

	private String responseMessage;

}
