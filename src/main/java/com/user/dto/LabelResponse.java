package com.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class LabelResponse {

	@JsonProperty("label_created")
	private Integer labelCreated; // 1 = success, 0 = failed

	@JsonProperty("label_url")
	private String labelUrl;

	@JsonProperty("not_created")
	private List<Long> notCreated;

	@JsonProperty("response")
	private List<LabelDetail> response;

	@Data
	public static class LabelDetail {

		@JsonProperty("shipment_id")
		private Long shipmentId;

		@JsonProperty("awb_code")
		private String awbCode;

		@JsonProperty("label_url")
		private String labelUrl;

	}

}
