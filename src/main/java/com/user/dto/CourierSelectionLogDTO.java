package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representation of a single row in the courier_selection_log table. Returned as a
 * list in shipment-related API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierSelectionLogDTO {

	private Long id;

	/** Shiprocket courier_company_id */
	private Integer courierCompanyId;

	/** Human-readable courier name */
	private String courierName;

	/** Quoted shipping rate from serviceability API */
	private BigDecimal rate;

	/** Estimated delivery days */
	private Double estimatedDeliveryDays;

	/**
	 * Rank among candidates (1 = best, 2 = second-best, …). Lower rank was attempted
	 * first.
	 */
	private Integer rank;

	/**
	 * True if this courier was ultimately used (AWB generated).
	 */
	private Boolean isSelected;

	/** AWB code — set only when this courier was selected. */
	private String awbCode;

	/** Actual shipping price from the AWB response (set for the selected courier). */
	private BigDecimal shippingPrice;

	private LocalDateTime createdAt;

}
