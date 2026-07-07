package com.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AwbRequest {

	@JsonProperty("shipment_id")
	private Integer shipmentId;

	@JsonProperty("courier_id")
	private Integer courierId; // optional — omit to let Shiprocket auto-assign

}
