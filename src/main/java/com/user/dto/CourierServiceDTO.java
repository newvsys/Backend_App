package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single available courier service option returned by Shiprocket's
 * serviceability API, after excluding any courier_company_id present in
 * {@link com.user.utility.Constants#BLOCKLISTED_COURIER_COMPANY_IDS}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierServiceDTO {

	/** Shiprocket courier_company_id — unique identifier of the courier partner. */
	private Integer courierId;

	/** Display name of the courier (e.g. "Delhivery Surface", "Xpressbees"). */
	private String courierName;

	/** Freight/shipping rate for this courier for the requested shipment. */
	private Double price;

	/** Additional COD handling charges, if applicable. */
	private Double codCharges;

	/** Any other charges levied by the courier (e.g. RTO, applicable taxes). */
	private Double otherCharges;

	/** Estimated number of days for delivery. */
	private Double estimatedDeliveryDays;

	/** Estimated delivery date/time as returned by Shiprocket (raw string, e.g. "Jan 05, 2025"). */
	private String estimatedDeliveryDate;

	/** Courier's overall rating (out of 5), if provided by Shiprocket. */
	private Double rating;

	/** Whether Cash-on-Delivery is available with this courier for the shipment. */
	private Boolean codAvailable;

	/** Whether this is an air-shipping courier option. */
	private Boolean isAir;

	/** Whether this is a surface-shipping courier option. */
	private Boolean isSurface;

}

