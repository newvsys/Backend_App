package com.user.service;

import com.user.dto.*;

import java.math.BigDecimal;

public interface DeliveryChargeService {

	/**
	 * Create a new delivery charge rule.
	 */
	DeliveryChargeResponseDTO createDeliveryCharge(DeliveryChargeCreateDTO createDTO);

	/**
	 * Update an existing delivery charge rule.
	 */
	DeliveryChargeResponseDTO updateDeliveryCharge(Long id, DeliveryChargeUpdateDTO updateDTO);

	/**
	 * Soft-delete (set status = 'I') a delivery charge rule.
	 */
	ResponseDTO deleteDeliveryCharge(Long id);

	/**
	 * Get a single delivery charge rule by id.
	 */
	DeliveryChargeResponseDTO getDeliveryChargeById(Long id);

	/**
	 * Get all delivery charge rules, optionally filtered by status ('A'/'I').
	 */
	DeliveryChargeListResponseDTO getAllDeliveryCharges(String status);

	/**
	 * Calculate the delivery charge applicable for a given order subtotal. Picks the
	 * first matching active rule sorted by priority (lowest first).
	 */
	DeliveryChargeCalculateResponseDTO calculateDeliveryCharge(BigDecimal orderAmount);

}
