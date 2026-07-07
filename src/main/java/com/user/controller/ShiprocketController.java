package com.user.controller;

import com.user.dto.*;
import com.user.model.ShippingEO;
import com.user.repository.ShippingRepository;
import com.user.service.ShippingService;
import com.user.service.ShiprocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/shipping")
public class ShiprocketController {

	private static final Logger logger = LoggerFactory.getLogger(ShiprocketController.class);

	private static final DateTimeFormatter SCAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final ShiprocketService shiprocketService;

	@Autowired
	private ShippingService shippingService;

	@Autowired
	private ShippingRepository shippingRepository;

	public ShiprocketController(ShiprocketService shiprocketService) {
		this.shiprocketService = shiprocketService;
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Existing endpoints
	// ──────────────────────────────────────────────────────────────────────────

	// Called when customer places an order
	@PostMapping("/create-order")
	public ResponseEntity<Map> createOrder(@RequestBody Map<String, Object> orderData) {
		return ResponseEntity.ok(shiprocketService.createOrder(orderData));
	}

	// Check delivery availability & rates by pincode (simple)
	@GetMapping("/serviceability")
	public ResponseEntity<Map> checkServiceability(@RequestParam String delivery,
			@RequestParam(required = false) String warehouseName) {
		return ResponseEntity.ok(shiprocketService.checkServiceability(delivery, warehouseName));
	}

	/**
	 * POST /api/shipping/serviceability Full serviceability check with all
	 * Shiprocket-supported parameters. Mandatory: pickupPostcode (defaults to warehouse
	 * pincode if omitted), deliveryPostcode.
	 */
	@PostMapping("/serviceabilityWithAllParams")
	public ResponseEntity<Map> checkServiceAvailability(@RequestBody ServiceabilityRequestDTO request) {
		logger.info("checkServiceAvailability called: deliveryPostcode={}, weight={}, cod={}",
				request != null ? request.getDeliveryPostcode() : null, request != null ? request.getWeight() : null,
				request != null ? request.getCod() : null);

		if (request == null || request.getDeliveryPostcode() == null) {
			return ResponseEntity.badRequest().build();
		}
		Map result = shiprocketService.checkServiceAvailability(request);
		return ResponseEntity.ok(result);
	}

	/**
	 * POST /api/shipping/serviceability/by-variants
	 *
	 * <p>
	 * Checks Shiprocket delivery serviceability for one or more product variants.
	 *
	 * <p>
	 * For each variant the service:
	 * <ol>
	 * <li>Finds all warehouses that hold stock for the variant (via inventory
	 * records).</li>
	 * <li>Uses each warehouse's postal code as the pickup postcode.</li>
	 * <li>Calls the Shiprocket serviceability API for every (warehouse →
	 * deliveryPostcode) pair.</li>
	 * <li>Returns {@code serviceable: true} only when ALL pairs return ≥ 1 available
	 * courier.</li>
	 * </ol>
	 * @param request Contains {@code deliveryPostcode} (required) and
	 * {@code productVariantIds} (required, ≥ 1 ID).
	 * @return {@link VariantServiceabilityResponseDTO} with per-variant / per-warehouse
	 * breakdown.
	 */
	@PostMapping("/serviceability/by-variants")
	public ResponseEntity<VariantServiceabilityResponseDTO> checkServiceabilityByVariants(
			@RequestBody VariantServiceabilityRequestDTO request) {

		logger.info("checkServiceabilityByVariants called: deliveryPostcode={}, variantIds={}",
				request != null ? request.getDeliveryPostcode() : null,
				request != null ? request.getProductVariantIds() : null);

		if (request == null || request.getDeliveryPostcode() == null || request.getDeliveryPostcode().isBlank()) {
			return ResponseEntity.badRequest()
				.body(VariantServiceabilityResponseDTO.builder()
					.serviceable(false)
					.message("deliveryPostcode is required")
					.build());
		}

		if (request.getProductVariantIds() == null || request.getProductVariantIds().isEmpty()) {
			return ResponseEntity.badRequest()
				.body(VariantServiceabilityResponseDTO.builder()
					.serviceable(false)
					.message("productVariantIds must contain at least one ID")
					.build());
		}

		VariantServiceabilityResponseDTO response = shiprocketService.checkVariantServiceability(request);
		return ResponseEntity.ok(response);
	}

	// Track a shipment (raw Shiprocket live data only)
	@GetMapping("/track/{awb}")
	public ResponseEntity<Map> track(@PathVariable String awb) {
		return ResponseEntity.ok(shiprocketService.trackShipment(awb));
	}

	/**
	 * GET /api/shipping/track-shipment/{awbCode} Returns combined internal DB shipment
	 * details + local tracking history + live Shiprocket tracking.
	 */
	@GetMapping("/track-shipment/{awbCode}")
	public ResponseEntity<TrackShipmentResponseDTO> trackShipment(@PathVariable String awbCode) {
		logger.info("trackShipment API called for awbCode={}", awbCode);
		TrackShipmentResponseDTO response = shippingService.trackShipment(awbCode);
		return ResponseEntity.ok(response);
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Shiprocket Webhook
	// POST /api/shipping/webhook
	// Shiprocket calls this endpoint whenever a shipment status changes.
	// ──────────────────────────────────────────────────────────────────────────

	/**
	 * Receives Shiprocket shipment status-change webhooks.
	 *
	 * <p>
	 * Shiprocket sends a JSON payload containing the AWB code, current status, and an
	 * ordered list of scan events. This handler:
	 * <ol>
	 * <li>Validates the payload (AWB + status required).</li>
	 * <li>Looks up the internal {@code ShippingEO} by AWB code.</li>
	 * <li>Extracts location and event-time from the latest scan event.</li>
	 * <li>Delegates to {@link ShippingService#shipmentStatusUpdate} which updates
	 * shipment status, tracking history, order status, and triggers refund/return flows
	 * as needed.</li>
	 * </ol>
	 *
	 * <p>
	 * Always returns HTTP 200 so Shiprocket does not retry on business-logic errors.
	 */
	@PostMapping("/webhook")
	public ResponseEntity<ShipStatusUpdateResponseDTO> handleWebhook(@RequestBody ShiprocketWebhookDTO payload) {

		logger.info("Received Shiprocket webhook: awb={}, status={}, shipmentId={}",
				payload != null ? payload.getAwb() : null, payload != null ? payload.getCurrentStatus() : null,
				payload != null ? payload.getShipmentId() : null);

		ShipStatusUpdateResponseDTO response = new ShipStatusUpdateResponseDTO();

		// ── 1. Basic validation ──────────────────────────────────────────────
		if (payload == null) {
			logger.warn("Shiprocket webhook: empty payload received");
			response.setStatus("FAILURE");
			response.setStatusMessage("Empty payload");
			return ResponseEntity.ok(response); // always 200 to prevent Shiprocket
												// retries
		}

		if (payload.getAwb() == null || payload.getAwb().isBlank()) {
			logger.warn("Shiprocket webhook: missing AWB in payload");
			response.setStatus("FAILURE");
			response.setStatusMessage("AWB is required");
			return ResponseEntity.ok(response);
		}

		if (payload.getCurrentStatus() == null || payload.getCurrentStatus().isBlank()) {
			logger.warn("Shiprocket webhook: missing current_status for awb={}", payload.getAwb());
			response.setStatus("FAILURE");
			response.setStatusMessage("current_status is required");
			return ResponseEntity.ok(response);
		}

		// ── 2. Look up ShippingEO by AWB ─────────────────────────────────────
		Optional<ShippingEO> shippingOpt = shippingRepository.findByAwb(payload.getAwb());
		if (shippingOpt.isEmpty()) {
			logger.warn("Shiprocket webhook: no ShippingEO found for awb={}", payload.getAwb());
			response.setStatus("FAILURE");
			response.setStatusMessage("No shipment found for AWB: " + payload.getAwb());
			return ResponseEntity.ok(response);
		}

		ShippingEO shippingEO = shippingOpt.get();
		String internalTrackingNumber = shippingEO.getTrackingNumber();

		// ── 3. Extract location & event-time from the latest scan ────────────
		String location = "";
		LocalDateTime eventTime = LocalDateTime.now();

		List<ShiprocketWebhookDTO.ScanEvent> scans = payload.getScans();
		if (scans != null && !scans.isEmpty()) {
			// Shiprocket delivers scans newest-first; take index 0 as the latest
			ShiprocketWebhookDTO.ScanEvent latestScan = scans.get(0);

			if (latestScan.getLocation() != null && !latestScan.getLocation().isBlank()) {
				location = latestScan.getLocation();
			}

			if (latestScan.getDate() != null && !latestScan.getDate().isBlank()) {
				try {
					eventTime = LocalDateTime.parse(latestScan.getDate(), SCAN_DATE_FORMATTER);
				}
				catch (DateTimeParseException e) {
					logger.warn("Shiprocket webhook: could not parse scan date '{}', using now()",
							latestScan.getDate());
				}
			}
		}

		// ── 4. Build the internal update request and delegate ────────────────
		ShipStatusUpdateRequestDTO updateRequest = ShipStatusUpdateRequestDTO.builder()
			.trackingNumber(internalTrackingNumber)
			.status(payload.getCurrentStatus())
			.location(location)
			.remarks(payload.getCurrentStatus())
			.eventTime(eventTime)
			.build();

		logger.info("Delegating webhook status update: trackingNumber={}, status={}, location={}",
				internalTrackingNumber, payload.getCurrentStatus(), location);

		response = shippingService.shipmentStatusUpdate(updateRequest);

		logger.info("Shiprocket webhook processed: awb={}, result status={}", payload.getAwb(), response.getStatus());
		return ResponseEntity.ok(response);
	}

}
