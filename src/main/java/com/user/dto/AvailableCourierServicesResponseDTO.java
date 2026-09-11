package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response payload for the "available courier services" API, which returns every
 * courier that services the requested pickup/delivery pair, EXCLUDING any courier
 * whose courier_company_id is present in
 * {@link com.user.utility.Constants#BLOCKLISTED_COURIER_COMPANY_IDS}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableCourierServicesResponseDTO {

	private String responseStatus;

	private String responseMessage;

	private int totalCount;

	/**
	 * The courier company id currently assigned to the order's shipment
	 * (populated from {@link com.user.model.ShippingEO#getCourierCompanyId()} when
	 * an existing shipment record is available). Null if no shipment exists yet
	 * or no courier has been assigned.
	 */
	private Integer currentlyUsedCourierId;

	private List<CourierServiceDTO> courierServices;

}

