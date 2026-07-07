package com.user.controller;

import com.user.dto.*;
import com.user.service.DeliveryChargeService;
import com.user.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST API for managing delivery charge rules.
 *
 * Base path: /api/delivery-charges
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐ │ Admin /
 * Management Endpoints │ │ POST /api/delivery-charges – create a rule │ │ PUT
 * /api/delivery-charges/{id} – update a rule │ │ DELETE /api/delivery-charges/{id} –
 * deactivate (soft-delete) │ │ GET /api/delivery-charges/{id} – get rule by id │ │ GET
 * /api/delivery-charges – list rules (?status=A/I) │ │ │ │ Customer / Checkout Endpoint │
 * │ GET /api/delivery-charges/calculate?orderAmount=500 – calculate │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/delivery-charges")
public class DeliveryChargeController {

	private static final Logger logger = LoggerFactory.getLogger(DeliveryChargeController.class);

	@Autowired
	private DeliveryChargeService deliveryChargeService;

	// ─── Create ───────────────────────────────────────────────────────────────

	/**
	 * POST /api/delivery-charges Create a new delivery charge rule.
	 *
	 * Request body example: { "ruleName" : "Free Delivery Above ₹500", "minOrderAmount" :
	 * 500, "maxOrderAmount" : null, "deliveryCharge" : 0, "priority" : 1, "description" :
	 * "Orders above ₹500 qualify for free delivery", "status" : "A", "createdBy" :
	 * "admin" }
	 */
	@PostMapping
	public ResponseEntity<?> createDeliveryCharge(@RequestBody DeliveryChargeCreateDTO createDTO) {
		logger.info("POST /api/delivery-charges - createDeliveryCharge: {}", createDTO);

		if (createDTO == null) {
			return ResponseEntity.badRequest().body(buildError("Request body must not be null"));
		}
		if (createDTO.getRuleName() == null || createDTO.getRuleName().isBlank()) {
			return ResponseEntity.badRequest().body(buildError("ruleName is required"));
		}
		if (createDTO.getDeliveryCharge() == null) {
			return ResponseEntity.badRequest().body(buildError("deliveryCharge is required"));
		}
		if (createDTO.getDeliveryCharge().compareTo(BigDecimal.ZERO) < 0) {
			return ResponseEntity.badRequest().body(buildError("deliveryCharge must be >= 0"));
		}

		try {
			DeliveryChargeResponseDTO response = deliveryChargeService.createDeliveryCharge(createDTO);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error creating delivery charge rule: {}", e.getMessage(), e);
			return ResponseEntity.status(500)
				.body(buildError("An error occurred while creating the delivery charge rule"));
		}
	}

	// ─── Update ───────────────────────────────────────────────────────────────

	/**
	 * PUT /api/delivery-charges/{id} Update an existing delivery charge rule. Only
	 * provided (non-null) fields are updated.
	 */
	@PutMapping("/{id}")
	public ResponseEntity<?> updateDeliveryCharge(@PathVariable("id") Long id,
			@RequestBody DeliveryChargeUpdateDTO updateDTO) {
		logger.info("PUT /api/delivery-charges/{} - updateDeliveryCharge", id);

		if (id == null) {
			return ResponseEntity.badRequest().body(buildError("id is required"));
		}
		if (updateDTO == null) {
			return ResponseEntity.badRequest().body(buildError("Request body must not be null"));
		}
		if (updateDTO.getDeliveryCharge() != null && updateDTO.getDeliveryCharge().compareTo(BigDecimal.ZERO) < 0) {
			return ResponseEntity.badRequest().body(buildError("deliveryCharge must be >= 0"));
		}

		try {
			DeliveryChargeResponseDTO response = deliveryChargeService.updateDeliveryCharge(id, updateDTO);
			return ResponseEntity.ok(response);
		}
		catch (RuntimeException e) {
			logger.error("Delivery charge rule not found id={}: {}", id, e.getMessage());
			return ResponseEntity.status(404).body(buildError(e.getMessage()));
		}
		catch (Exception e) {
			logger.error("Error updating delivery charge rule id={}: {}", id, e.getMessage(), e);
			return ResponseEntity.status(500)
				.body(buildError("An error occurred while updating the delivery charge rule"));
		}
	}

	// ─── Soft-delete ──────────────────────────────────────────────────────────

	/**
	 * DELETE /api/delivery-charges/{id} Deactivates (soft-deletes) a delivery charge rule
	 * by setting status = 'I'.
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteDeliveryCharge(@PathVariable("id") Long id) {
		logger.info("DELETE /api/delivery-charges/{} - deleteDeliveryCharge", id);

		if (id == null) {
			return ResponseEntity.badRequest().body(buildError("id is required"));
		}

		try {
			ResponseDTO response = deliveryChargeService.deleteDeliveryCharge(id);
			return ResponseEntity.ok(response);
		}
		catch (RuntimeException e) {
			logger.error("Delivery charge rule not found id={}: {}", id, e.getMessage());
			return ResponseEntity.status(404).body(buildError(e.getMessage()));
		}
		catch (Exception e) {
			logger.error("Error deleting delivery charge rule id={}: {}", id, e.getMessage(), e);
			return ResponseEntity.status(500)
				.body(buildError("An error occurred while deleting the delivery charge rule"));
		}
	}

	// ─── Get by id ────────────────────────────────────────────────────────────

	/**
	 * GET /api/delivery-charges/{id} Retrieve a single delivery charge rule by its id.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<?> getDeliveryChargeById(@PathVariable("id") Long id) {
		logger.info("GET /api/delivery-charges/{} - getDeliveryChargeById", id);

		if (id == null) {
			return ResponseEntity.badRequest().body(buildError("id is required"));
		}

		try {
			DeliveryChargeResponseDTO response = deliveryChargeService.getDeliveryChargeById(id);
			return ResponseEntity.ok(response);
		}
		catch (RuntimeException e) {
			logger.error("Delivery charge rule not found id={}: {}", id, e.getMessage());
			return ResponseEntity.status(404).body(buildError(e.getMessage()));
		}
		catch (Exception e) {
			logger.error("Error fetching delivery charge rule id={}: {}", id, e.getMessage(), e);
			return ResponseEntity.status(500)
				.body(buildError("An error occurred while fetching the delivery charge rule"));
		}
	}

	// ─── Get all ──────────────────────────────────────────────────────────────

	/**
	 * GET /api/delivery-charges List all delivery charge rules, optionally filtered by
	 * status.
	 *
	 * Query param: status = A (active) | I (inactive) | (omit for all)
	 */
	@GetMapping
	public ResponseEntity<DeliveryChargeListResponseDTO> getAllDeliveryCharges(
			@RequestParam(value = "status", required = false) String status) {
		logger.info("GET /api/delivery-charges - getAllDeliveryCharges, status={}", status);

		try {
			DeliveryChargeListResponseDTO response = deliveryChargeService.getAllDeliveryCharges(status);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error fetching delivery charge rules: {}", e.getMessage(), e);
			DeliveryChargeListResponseDTO errorResp = DeliveryChargeListResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("An error occurred while fetching delivery charge rules")
				.build();
			return ResponseEntity.status(500).body(errorResp);
		}
	}

	// ─── Calculate ────────────────────────────────────────────────────────────

	/**
	 * GET /api/delivery-charges/calculate?orderAmount=499.00 Returns the applicable
	 * delivery charge for a given order subtotal.
	 *
	 * This is the endpoint the checkout / cart summary screen should call to display the
	 * correct shipping fee before placing an order.
	 */
	@GetMapping("/calculate")
	public ResponseEntity<DeliveryChargeCalculateResponseDTO> calculateDeliveryCharge(
			@RequestParam("orderAmount") BigDecimal orderAmount) {
		logger.info("GET /api/delivery-charges/calculate - orderAmount={}", orderAmount);

		if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) < 0) {
			DeliveryChargeCalculateResponseDTO errorResp = DeliveryChargeCalculateResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("orderAmount must be a non-negative value")
				.build();
			return ResponseEntity.badRequest().body(errorResp);
		}

		try {
			DeliveryChargeCalculateResponseDTO response = deliveryChargeService.calculateDeliveryCharge(orderAmount);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error calculating delivery charge for orderAmount={}: {}", orderAmount, e.getMessage(), e);
			DeliveryChargeCalculateResponseDTO errorResp = DeliveryChargeCalculateResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("An error occurred while calculating the delivery charge")
				.build();
			return ResponseEntity.status(500).body(errorResp);
		}
	}

	// ─── helper ───────────────────────────────────────────────────────────────

	private ResponseDTO buildError(String message) {
		return ResponseDTO.builder().responseStatus(Constants.FAILURE_STATUS).responseMessage(message).build();
	}

}
