package com.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AwbResponse {

	@JsonProperty("awb_assign_status")
	private Integer awbAssignStatus; // 1 = success, 0 = failed

	@JsonProperty("response")
	private AwbDetail response;

	@Data
	public static class AwbDetail {

		// Shiprocket actual shape: response → data → awb_code / courier_* / etd
		@JsonProperty("data")
		private AwbData data;

		// Fallback: some versions put fields directly at response level
		@JsonProperty("awb_code")
		private String awbCode;

		@JsonProperty("courier_company_id")
		private Integer courierCompanyId;

		@JsonProperty("courier_name")
		private String courierName;

		@JsonProperty("shipment_id")
		private Long shipmentId;

		@JsonProperty("assigned_date_time")
		private String assignedDateTime;

		@JsonProperty("etd")
		private String etd;

		/** Convenience: returns awb_code from data if present, else top-level. */
		public String getResolvedAwbCode() {
			return data != null && data.getAwbCode() != null ? data.getAwbCode() : awbCode;
		}

		public Integer getResolvedCourierCompanyId() {
			return data != null && data.getCourierCompanyId() != null ? data.getCourierCompanyId() : courierCompanyId;
		}

		public String getResolvedCourierName() {
			return data != null && data.getCourierName() != null ? data.getCourierName() : courierName;
		}

		public String getResolvedEtd() {
			return data != null && data.getEtd() != null ? data.getEtd() : etd;
		}

	}

	@Data
	public static class AwbData {

		@JsonProperty("awb_code")
		private String awbCode;

		@JsonProperty("courier_company_id")
		private Integer courierCompanyId;

		@JsonProperty("courier_name")
		private String courierName;

		@JsonProperty("shipment_id")
		private Long shipmentId;

		@JsonProperty("assigned_date_time")
		private String assignedDateTime;

		@JsonProperty("cod_charges")
		private Double codCharges;

		@JsonProperty("applied_weight")
		private Double appliedWeight;

		@JsonProperty("etd")
		private String etd;

	}

}
