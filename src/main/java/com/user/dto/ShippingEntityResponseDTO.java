package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Response DTO for shipping-related endpoints.
 * Used for responses from GET and POST shipping endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingEntityResponseDTO {

	private String responseStatus; // SUCCESS / FAILURE

	private String responseMessage;

	private List<ShippingDetailDTO> data;

	private ShippingDetailDTO shipping;

	private Integer count;
}

