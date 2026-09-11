package com.user.controller;

import com.user.dto.*;
import com.user.model.CourierSelectionLogEO;
import com.user.repository.CourierSelectionLogRepository;
import com.user.service.ShippingService;
import com.user.utility.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ShippingController {

	private static final Logger logger = LoggerFactory.getLogger(ShippingController.class);

	@Autowired
	private ShippingService shippingService;

	@Autowired
	private CourierSelectionLogRepository courierSelectionLogRepository;

	@GetMapping("/shipping-history/{trackingNumber}")
	public ResponseEntity<ShipTrackHistoryResponseDTO> getShippingHistory(
			@PathVariable("trackingNumber") String trackingNumber) {
		logger.info("Received request for shipping history with trackingNumber: {}", trackingNumber);
		ShipTrackHistoryRequestDTO requestDTO = new ShipTrackHistoryRequestDTO();
		ShipTrackHistoryResponseDTO responseDTO = new ShipTrackHistoryResponseDTO();
		try {
			if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
				logger.error("Tracking number is null or empty");
				responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
				responseDTO.setResponseMessage("Tracking number must not be null or empty");
				return ResponseEntity.badRequest().body(responseDTO);
			}
			requestDTO.setTrackId(trackingNumber);
			responseDTO = shippingService.getShippingHistory(requestDTO);

			logger.info("Shipping history fetched successfully for trackingNumber: {}", trackingNumber);
			return ResponseEntity.ok(responseDTO);
		}
		catch (Exception e) {

			logger.error("Error occurred while fetching shipping history for trackingNumber {}: {}", trackingNumber,
					e.getMessage(), e);
			responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
			responseDTO.setResponseMessage("An error occurred while fetching shipping history. Please try again later");
			return ResponseEntity.status(500).body(responseDTO);
		}
	}

	@GetMapping("/shipments")
	public ResponseEntity<AllShipmentsResponseDTO> getAllShipments(
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "orderNumber", required = false) String orderNumber) {
		logger.info("Received getAllShipments request with status filter: {}, orderNumber filter: {}", status,
				orderNumber);
		AllShipmentsResponseDTO response = new AllShipmentsResponseDTO();
		try {
			response = shippingService.getAllShipments(status, orderNumber);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error occurred while fetching all shipments: {}", e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while fetching shipments. Please try again later.");
			return ResponseEntity.status(500).body(response);
		}
	}

	@PostMapping("/shipment-status-update")
	public ResponseEntity<ShipStatusUpdateResponseDTO> shipStatusUpdate(
			@RequestBody ShipStatusUpdateRequestDTO shipStatusUpdateRequestDTO) {
		ShipStatusUpdateResponseDTO response = new ShipStatusUpdateResponseDTO();
		try {
			// Null checks for important fields
			if (shipStatusUpdateRequestDTO == null) {
				logger.error("shipStatusUpdateRequestDTO is null");
				response.setStatus(Constants.FAILURE_STATUS);
				response.setStatusMessage("Request body must not be null");
				return ResponseEntity.badRequest().body(response);
			}
			// Example: Add null/empty checks for common fields (customize as needed)
			if (shipStatusUpdateRequestDTO.getTrackingNumber() == null
					|| shipStatusUpdateRequestDTO.getTrackingNumber().trim().isEmpty()) {
				logger.error("TrackId is null or empty in shipStatusUpdateRequestDTO");
				response.setStatus(Constants.FAILURE_STATUS);
				response.setStatusMessage("TrackId must not be null or empty");
				return ResponseEntity.badRequest().body(response);
			}
			if (shipStatusUpdateRequestDTO.getStatus() == null
					|| shipStatusUpdateRequestDTO.getStatus().trim().isEmpty()) {
				logger.error("Status is null or empty in shipStatusUpdateRequestDTO");
				response.setStatus(Constants.FAILURE_STATUS);
				response.setStatusMessage("Status must not be null or empty");
				return ResponseEntity.badRequest().body(response);
			}
			// Add more field checks as required for your DTO

			response = shippingService.shipmentStatusUpdate(shipStatusUpdateRequestDTO);
			logger.info("Shipment status updated successfully for request: {}", shipStatusUpdateRequestDTO);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error occurred while updating shipment status: {}", e.getMessage(), e);
			response.setStatus(Constants.FAILURE_STATUS);
			response.setStatusMessage("An error occurred while updating shipment status. Please try again later");
			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * POST /api/shipment/create
	 *
	 * <p>
	 * Manually triggers shipment creation for the given order, mirroring the internal
	 * flow normally invoked automatically after payment confirmation
	 * ({@code ShippingServiceImpl.processCreateShipmentEvent}). Builds shipment(s) per
	 * warehouse for the order's items and kicks off Shiprocket order creation.
	 *
	 * <p>
	 * Request body: { "orderId": 123 } — "eventType" is optional and defaults to the
	 * SHIPPED event type used internally.
	 */
	@PostMapping("/shipment/create")
	public ResponseEntity<ShippingResponseDTO> createShipment(@RequestBody ShippingRequestDTO request) {
		logger.info("Received createShipment request: orderId={}", request != null ? request.getOrderId() : null);
		if (request == null || request.getOrderId() == null) {
			ShippingResponseDTO response = ShippingResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("orderId must not be null")
				.build();
			return ResponseEntity.badRequest().body(response);
		}
		if ((request.getCartonNo() == null || request.getCartonNo().trim().isEmpty())
				&& request.getRequestCreateCartonDTO() == null) {
			ShippingResponseDTO response = ShippingResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("Either cartonNo or Required value for create carton is required.")
				.orderId(request.getOrderId())
				.build();
			return ResponseEntity.badRequest().body(response);
		}
		try {

			ShippingResponseDTO response = shippingService.processCreateShipmentEvent(request);
			if (response != null && Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				return ResponseEntity.badRequest().body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("createShipment: error triggering shipment creation for orderId={}: {}",
					request.getOrderId(), e.getMessage(), e);
			ShippingResponseDTO response = ShippingResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("An error occurred while triggering shipment creation. Please try again later.")
				.orderId(request.getOrderId())
				.build();
			return ResponseEntity.status(500).body(response);
		}
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Carton CRUD APIs
	// ──────────────────────────────────────────────────────────────────────────

	/**
	 * GET /api/cartons Retrieve all cartons. Optionally filter by status ('A' = Active,
	 * 'I' = Inactive).
	 */
	@GetMapping("/cartons")
	public ResponseEntity<CartonListResponseDTO> getAllCartons(
			@RequestParam(value = "status", required = false) String status) {
		logger.info("Received getAllCartons request with status filter: {}", status);
		CartonListResponseDTO response = shippingService.getAllCartons(status);
		return ResponseEntity.ok(response);
	}

	/**
	 * GET /api/carton/{id} Retrieve a single carton by its id.
	 */
	@GetMapping("/carton/{id}")
	public ResponseEntity<ResponseCreateCartonDTO> getCartonById(@PathVariable("id") Long id) {
		logger.info("Received getCartonById request for id: {}", id);
		if (id == null) {
			return ResponseEntity.badRequest().build();
		}
		ResponseCreateCartonDTO response = shippingService.getCartonById(id);
		return ResponseEntity.ok(response);
	}

	/**
	 * POST /api/carton Create a new CartonEO.
	 */
	@PostMapping("/carton")
	public ResponseEntity<ResponseCreateCartonDTO> addCarton(@RequestBody RequestCreateCartonDTO cartonDTO) {
		if (cartonDTO == null) {
			return ResponseEntity.badRequest().build();
		}
		ResponseCreateCartonDTO carton = shippingService.addCarton(cartonDTO);
		return ResponseEntity.ok(carton);
	}

	/**
	 * PUT /api/carton/{id} Update an existing CartonEO (dimensions, name, weight, who).
	 */
	@PutMapping("/carton/{id}")
	public ResponseEntity<ResponseCreateCartonDTO> updateCarton(@PathVariable("id") Long id,
			@RequestBody CartonUpdateRequestDTO request) {
		logger.info("Received updateCarton request for id: {}", id);
		if (id == null || request == null) {
			return ResponseEntity.badRequest().build();
		}
		ResponseCreateCartonDTO response = shippingService.updateCarton(id, request);
		return ResponseEntity.ok(response);
	}

	/**
	 * DELETE /api/carton/{id} Soft-delete a carton by changing its status (typically 'A'
	 * → 'I'). Physical deletion is not supported; only status changes are allowed.
	 * Request body: { "status": "I", "who": "admin" }
	 */
	@DeleteMapping("/carton/{id}")
	public ResponseEntity<ResponseDTO> deleteCarton(@PathVariable("id") Long id,
			@RequestBody CartonStatusChangeRequestDTO request) {
		logger.info("Received deleteCarton (status change) request for id: {}, newStatus: {}", id,
				request != null ? request.getStatus() : null);
		if (id == null) {
			return ResponseEntity.badRequest().build();
		}
		if (request == null || request.getStatus() == null || request.getStatus().trim().isEmpty()) {
			ResponseDTO errorResp = ResponseDTO.builder()
				.responseStatus("FAILURE")
				.responseMessage("Request body with 'status' field is required")
				.build();
			return ResponseEntity.badRequest().body(errorResp);
		}
		ResponseDTO response = shippingService.deleteCarton(id, request);
		return ResponseEntity.ok(response);
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Manual Shiprocket step APIs
	// ──────────────────────────────────────────────────────────────────────────

	/**
	 * POST /api/shipment/generate-awb Manually generate / assign an AWB for a Shiprocket
	 * shipment. Request body: { "shipment_id": 12345, "courier_id": null }
	 */
	@PostMapping("/shipment/generate-awb")
	public ResponseEntity<AwbResponse> generateAwb(@RequestBody AwbRequest request) {
		logger.info("Received generate-awb request: {}", request);
		if (request == null || request.getShipmentId() == null) {
			return ResponseEntity.badRequest().build();
		}
		AwbResponse response = shippingService.generateAwb(request);
		return ResponseEntity.ok(response);
	}

	/**
	 * POST /api/shipment/request-pickup Manually request a courier pickup for one or more
	 * Shiprocket shipments. Request body: { "shipment_id": [12345] }
	 */
	@PostMapping("/shipment/request-pickup")
	public ResponseEntity<PickupResponse> requestPickup(@RequestBody PickupRequest request) {
		logger.info("Received request-pickup request: {}", request);
		if (request == null || request.getShipmentId() == null || request.getShipmentId().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}
		PickupResponse response = shippingService.requestPickup(request);
		return ResponseEntity.ok(response);
	}

	/**
	 * POST /api/shipment/generate-label Manually generate a shipping label for one or
	 * more Shiprocket shipments. Request body: { "shipment_id": [12345] }
	 */
	@PostMapping("/shipment/generate-label")
	public ResponseEntity<LabelResponse> generateLabel(@RequestBody LabelRequest request) {
		logger.info("Received generate-label request: {}", request);
		if (request == null || request.getShipmentId() == null || request.getShipmentId().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}
		LabelResponse response = shippingService.generateLabel(request);
		return ResponseEntity.ok(response);
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Courier selection log APIs
	// ──────────────────────────────────────────────────────────────────────────

	/**
	 * GET /api/shipment/{shipmentId}/courier-candidates Returns all courier candidates
	 * evaluated for the given internal shipment, sorted by rank (1 = best). The selected
	 * courier has isSelected=true.
	 */
	@GetMapping("/shipment/{shipmentId}/courier-candidates")
	public ResponseEntity<List<CourierSelectionLogDTO>> getCourierCandidates(
			@PathVariable("shipmentId") Long shipmentId) {
		logger.info("getCourierCandidates called for shipmentId={}", shipmentId);
		if (shipmentId == null) {
			return ResponseEntity.badRequest().build();
		}
		List<CourierSelectionLogEO> rows = courierSelectionLogRepository.findByShipmentIdOrderByRankAsc(shipmentId);
		List<CourierSelectionLogDTO> dtos = rows.stream()
			.map(row -> CourierSelectionLogDTO.builder()
				.id(row.getId())
				.courierCompanyId(row.getCourierCompanyId())
				.courierName(row.getCourierName())
				.rate(row.getRate())
				.estimatedDeliveryDays(row.getEstimatedDeliveryDays())
				.rank(row.getRank())
				.isSelected(row.getIsSelected())
				.awbCode(row.getAwbCode())
				.shippingPrice(row.getShippingPrice())
				.createdAt(row.getCreatedAt())
				.build())
			.collect(Collectors.toList());
		return ResponseEntity.ok(dtos);
	}

	/**
	 * GET /api/order/{orderId}/courier-candidates Returns all courier candidates
	 * evaluated for all shipments of a given order, sorted by shipment then rank.
	 */
	@GetMapping("/order/{orderId}/courier-candidates")
	public ResponseEntity<List<CourierSelectionLogDTO>> getCourierCandidatesByOrder(
			@PathVariable("orderId") Long orderId) {
		logger.info("getCourierCandidatesByOrder called for orderId={}", orderId);
		if (orderId == null) {
			return ResponseEntity.badRequest().build();
		}
		List<CourierSelectionLogEO> rows = courierSelectionLogRepository
			.findByOrderIdOrderByShipmentIdAscRankAsc(orderId);
		List<CourierSelectionLogDTO> dtos = rows.stream()
			.map(row -> CourierSelectionLogDTO.builder()
				.id(row.getId())
				.courierCompanyId(row.getCourierCompanyId())
				.courierName(row.getCourierName())
				.rate(row.getRate())
				.estimatedDeliveryDays(row.getEstimatedDeliveryDays())
				.rank(row.getRank())
				.isSelected(row.getIsSelected())
				.awbCode(row.getAwbCode())
				.shippingPrice(row.getShippingPrice())
				.createdAt(row.getCreatedAt())
				.build())
			.collect(Collectors.toList());
		return ResponseEntity.ok(dtos);
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Manual Shiprocket order creation / update (fallback for failed automation)
	// ──────────────────────────────────────────────────────────────────────────

	/**
	 * POST /api/shipment/manual-update
	 *
	 * <p>
	 * Creates or updates a shipment record and optionally adds a tracking history entry.
	 * Used when the automated Shiprocket flow fails at any step (CREATE_ORDER,
	 * GENERATE_AWB, REQUEST_PICKUP, GENERATE_LABEL) so that an admin can supply the
	 * missing data and keep the shipping and history tables consistent.
	 *
	 * <p>
	 * Request body fields (all optional except one identifier):
	 * <ul>
	 * <li>shipmentId / orderId / orderNumber — identifies the shipment</li>
	 * <li>shiprocketOrderId, shiprocketShipmentId — from CREATE_ORDER step</li>
	 * <li>awbCode, courierName, courierCompanyId — from GENERATE_AWB step</li>
	 * <li>pickupId, pickupToken, pickupScheduledDate — from REQUEST_PICKUP step</li>
	 * <li>labelUrl — from GENERATE_LABEL step</li>
	 * <li>shipmentStatus — new status to set on the shipment</li>
	 * <li>historyStatus, historyLocation, historyRemarks — add a tracking history
	 * row</li>
	 * <li>step — Shiprocket step label for the audit log</li>
	 * <li>notes — free-text explanation for the manual override</li>
	 * </ul>
	 */
	@PostMapping("/shipment/manual-update")
	public ResponseEntity<ManualShiprocketUpdateResponseDTO> manualShiprocketUpdate(
			@RequestBody ManualShiprocketUpdateRequestDTO request) {
		logger.info("Received manual Shiprocket update request: shipmentId={}, orderId={}, orderNumber={}, step={}",
				request != null ? request.getShipmentId() : null, request != null ? request.getOrderId() : null,
				request != null ? request.getOrderNumber() : null, request != null ? request.getStep() : null);

		ManualShiprocketUpdateResponseDTO response = new ManualShiprocketUpdateResponseDTO();

		if (request == null) {
			response.setResponseStatus("FAILURE");
			response.setResponseMessage("Request body must not be null.");
			return ResponseEntity.badRequest().body(response);
		}

		// At least one identifier must be provided
		boolean hasIdentifier = request.getShipmentId() != null || request.getOrderId() != null
				|| (request.getOrderNumber() != null && !request.getOrderNumber().trim().isEmpty());
		if (!hasIdentifier) {
			response.setResponseStatus("FAILURE");
			response.setResponseMessage("At least one of shipmentId, orderId, or orderNumber must be provided.");
			return ResponseEntity.badRequest().body(response);
		}

		try {
			response = shippingService.manualShiprocketUpdate(request);
			if ("FAILURE".equals(response.getResponseStatus())) {
				return ResponseEntity.badRequest().body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in manualShiprocketUpdate: {}", e.getMessage(), e);
			response.setResponseStatus("FAILURE");
			response.setResponseMessage("An error occurred during manual Shiprocket update. Please try again later.");
			return ResponseEntity.status(500).body(response);
		}
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Order-number-based shipment management APIs
	// ──────────────────────────────────────────────────────────────────────────

	/**
	 * GET /api/shipment/order/{orderNumber} Fetch full shipping details (including
	 * tracking history) for the given order number.
	 */
	@GetMapping("/shipment/order/{orderNumber}")
	public ResponseEntity<ShippingDetailResponseDTO> getShippingByOrderNumber(
			@PathVariable("orderNumber") String orderNumber) {
		logger.info("getShippingByOrderNumber called for orderNumber={}", orderNumber);
		ShippingDetailResponseDTO response = new ShippingDetailResponseDTO();
		if (orderNumber == null || orderNumber.trim().isEmpty()) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Order number must not be null or empty.");
			return ResponseEntity.badRequest().body(response);
		}
		try {
			response = shippingService.getShippingDetailsByOrderNumber(orderNumber.trim());
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				return ResponseEntity.status(404).body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("getShippingByOrderNumber: error for orderNumber={} — {}", orderNumber, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while fetching shipping details. Please try again later.");
			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * PUT /api/shipment/order/{orderNumber} Update an existing shipping record for the
	 * given order number. Only non-null fields supplied in the request body are applied.
	 * Returns 404 if no shipping record exists (use POST to create one).
	 */
	@PutMapping("/shipment/order/{orderNumber}")
	public ResponseEntity<ManualShiprocketUpdateResponseDTO> updateShippingByOrderNumber(
			@PathVariable("orderNumber") String orderNumber, @RequestBody ShippingOrderRequestDTO request) {
		logger.info("updateShippingByOrderNumber called for orderNumber={}", orderNumber);
		ManualShiprocketUpdateResponseDTO response = new ManualShiprocketUpdateResponseDTO();
		if (orderNumber == null || orderNumber.trim().isEmpty()) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Order number must not be null or empty.");
			return ResponseEntity.badRequest().body(response);
		}
		if (request == null) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Request body must not be null.");
			return ResponseEntity.badRequest().body(response);
		}
		try {
			response = shippingService.updateShippingByOrderNumber(orderNumber.trim(), request);
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				return ResponseEntity.status(404).body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("updateShippingByOrderNumber: error for orderNumber={} — {}", orderNumber, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while updating shipping record. Please try again later.");
			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * POST /api/shipment/order/{orderNumber} Create a new shipping record for the given
	 * order number. Returns 400 if a non-cancelled shipping record already exists (use
	 * PUT to update).
	 */
	@PostMapping("/shipment/order/{orderNumber}")
	public ResponseEntity<ManualShiprocketUpdateResponseDTO> createShippingByOrderNumber(
			@PathVariable("orderNumber") String orderNumber, @RequestBody ShippingOrderRequestDTO request) {
		logger.info("createShippingByOrderNumber called for orderNumber={}", orderNumber);
		ManualShiprocketUpdateResponseDTO response = new ManualShiprocketUpdateResponseDTO();
		if (orderNumber == null || orderNumber.trim().isEmpty()) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Order number must not be null or empty.");
			return ResponseEntity.badRequest().body(response);
		}
		if (request == null) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Request body must not be null.");
			return ResponseEntity.badRequest().body(response);
		}
		try {
			response = shippingService.createShippingByOrderNumber(orderNumber.trim(), request);
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				return ResponseEntity.badRequest().body(response);
			}
			return ResponseEntity.status(201).body(response);
		}
		catch (Exception e) {
			logger.error("createShippingByOrderNumber: error for orderNumber={} — {}", orderNumber, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while creating shipping record. Please try again later.");
			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * GET /api/shipment/order/{orderNumber}/shiprocket-payload
	 *
	 * <p>
	 * Fetches the live shipment data from the actual Shiprocket API for the given
	 * internal order number, returned in exactly the shape expected by the request body
	 * of PUT /api/shipment/order/{orderNumber}. The Shiprocket order/shipment is first
	 * resolved via the local DB (order number → shiprocketOrderId / AWB), then the
	 * courier / AWB / status fields are refreshed live from Shiprocket (falls back to
	 * the last known DB values if the live Shiprocket calls fail).
	 *
	 * <p>
	 * Typical usage: call this GET, copy/adjust the response JSON, then send it as the
	 * body of the PUT call for the same order number.
	 */
	// ──────────────────────────────────────────────────────────────────────────
	// Retrigger shipping process (admin manual retry after a failure)
	// ──────────────────────────────────────────────────────────────────────────

	/**
	 * POST /api/order/{orderNumber}/retrigger-shipping
	 *
	 * <p>
	 * Used by the admin UI to manually retrigger the Shiprocket shipping process
	 * (find best courier → generate AWB → request pickup → generate label → track
	 * shipment) for a failed order, by passing the customer-facing order number.
	 * Typically called some time after the initial failure (e.g.
	 * MANUAL_PROCESSING_REQUIRED, or a shipment stuck without an AWB/label). If a
	 * Shiprocket order already exists for the shipment, the retrigger resumes
	 * processing instead of creating a duplicate order.
	 * <p>
	 * Safe to call multiple times: a short cooldown period is enforced per shipment to
	 * avoid accidental rapid re-triggering, and any shipment that has already been
	 * fully processed (AWB assigned, pickup scheduled, label generated, tracking
	 * captured) for this order number is skipped rather than reprocessed/duplicated.
	 */
	@PostMapping("/order/{orderNumber}/retrigger-shipping")
	public ResponseEntity<RetriggerShippingResponseDTO> retriggerShippingProcess(
			@PathVariable("orderNumber") String orderNumber) {
		logger.info("retriggerShippingProcess called for orderNumber={}", orderNumber);
		RetriggerShippingResponseDTO response = new RetriggerShippingResponseDTO();
		if (orderNumber == null || orderNumber.isBlank()) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("orderNumber must not be null/blank.");
			return ResponseEntity.badRequest().body(response);
		}
		try {
			response = shippingService.retriggerShippingProcess(orderNumber);
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				return ResponseEntity.badRequest().body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			Throwable rootCause = e;
			while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
				rootCause = rootCause.getCause();
			}
			String reason = rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.toString();
			logger.error("retriggerShippingProcess: error for orderNumber={} — {}", orderNumber, reason, e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while retriggering the shipping process: " + reason);
			return ResponseEntity.status(500).body(response);
		}
	}

	@GetMapping("/shipment/order/{orderNumber}/shiprocket-payload")
	public ResponseEntity<ShipmentPutPayloadResponseDTO> getShiprocketPutPayloadByOrderNumber(
			@PathVariable("orderNumber") String orderNumber) {
		logger.info("getShiprocketPutPayloadByOrderNumber called for orderNumber={}", orderNumber);
		ShipmentPutPayloadResponseDTO response = new ShipmentPutPayloadResponseDTO();
		if (orderNumber == null || orderNumber.trim().isEmpty()) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Order number must not be null or empty.");
			return ResponseEntity.badRequest().body(response);
		}
		try {
			response = shippingService.getShiprocketPutPayloadByOrderNumber(orderNumber.trim());
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				return ResponseEntity.status(404).body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("getShiprocketPutPayloadByOrderNumber: error for orderNumber={} — {}", orderNumber,
					e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(
					"An error occurred while fetching the Shiprocket shipment payload. Please try again later.");
			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * GET /api/shipping/{shipmentId}
	 *
	 * Fetch full shipping details for a specific shipment ID, including all fields
	 * and tracking history.
	 */
	@GetMapping("/shipping/{shipmentId}")
	public ResponseEntity<ShippingDetailResponseDTO> getShippingByShipmentId(
			@PathVariable("shipmentId") Long shipmentId) {
		logger.info("getShippingByShipmentId called for shipmentId={}", shipmentId);
		ShippingDetailResponseDTO response = new ShippingDetailResponseDTO();
		if (shipmentId == null) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Shipment ID must not be null.");
			return ResponseEntity.badRequest().body(response);
		}
		try {
			response = shippingService.getShippingByShipmentId(shipmentId);
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				return ResponseEntity.status(404).body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("getShippingByShipmentId: error for shipmentId={} — {}", shipmentId, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while fetching shipping details. Please try again later.");
			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * POST /api/shipping
	 *
	 * Create a new shipping record with all fields. Used to manually save/create
	 * shipping details for an order.
	 *
	 * Request body: ShippingOrderRequestDTO with all optional fields
	 */
	@PostMapping("/shipping")
	public ResponseEntity<ManualShiprocketUpdateResponseDTO> saveShipping(
			@RequestBody ShippingOrderRequestDTO request) {
		logger.info("saveShipping called");
		ManualShiprocketUpdateResponseDTO response = new ManualShiprocketUpdateResponseDTO();
		if (request == null) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Request body must not be null");
			return ResponseEntity.badRequest().body(response);
		}
		try {
			response = shippingService.saveShipping(request);
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				return ResponseEntity.badRequest().body(response);
			}
			return ResponseEntity.status(201).body(response);
		}
		catch (Exception e) {
			logger.error("saveShipping: error — {}", e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while saving shipping record. Please try again later.");
			return ResponseEntity.status(500).body(response);
		}
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Confirmed / Ready to Ship orders with a failed Shiprocket step
	// ──────────────────────────────────────────────────────────────────────────

	/**
	 * GET /api/orders/failed-shiprocket-steps
	 *
	 * <p>
	 * Returns every order whose status is "Confirmed" or "Ready to Ship" (Order With
	 * Shipping) AND which has a shipment problem — either the linked shipment has at
	 * least one of the following Shiprocket step statuses equal to FAILURE:
	 * shiprocketOrderStatus, generateAwbStatus, requestPickupStatus,
	 * generateLabelStatus, trackShipmentStatus, estimateStatus — OR the order does not
	 * have any shipment record yet.
	 *
	 * <p>
	 * Orders with status "Confirmed" are fetched first, followed by orders with status
	 * "Ready to Ship"; for each order its respective shipment is then looked up
	 * separately.
	 *
	 * <p>
	 * Each entry in the response groups the parent Order details and the corresponding
	 * ShippingEO (shipment) details into two separate nested objects (
	 * {@code orderDetails} and {@code shippingDetails}, the latter {@code null} when no
	 * shipment exists yet), plus a {@code failedSteps} list identifying exactly which
	 * step(s) failed (or {@code ["no_shipment"]}) — useful for the admin UI to surface
	 * shipments stuck in the Shiprocket automation pipeline, or orders that never even
	 * got a shipment created, so they can be manually retriggered/fixed.
	 */
	@GetMapping("/orders/failed-shiprocket-steps")
	public ResponseEntity<FailedShiprocketOrdersResponseDTO> getConfirmedOrReadyToShipOrdersWithFailedShiprocketStep() {
		logger.info("Received getConfirmedOrReadyToShipOrdersWithFailedShiprocketStep request");
		FailedShiprocketOrdersResponseDTO response;
		try {
			response = shippingService.getConfirmedOrReadyToShipOrdersWithFailedShiprocketStep();
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				return ResponseEntity.status(500).body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error occurred while fetching failed Shiprocket step orders: {}", e.getMessage(), e);
			FailedShiprocketOrdersResponseDTO errorResponse = FailedShiprocketOrdersResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("An error occurred while fetching failed Shiprocket step orders. Please try again later.")
				.build();
			return ResponseEntity.status(500).body(errorResponse);
		}
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Order ID-based Shipping APIs
	// ──────────────────────────────────────────────────────────────────────────

	/**
	 * GET /api/order/{orderId}/shipping
	 *
	 * <p>
	 * Fetch all shipping records for a given order ID.
	 */
	@GetMapping("/order/{orderId}/shipping")
	public ResponseEntity<ShippingEntityResponseDTO> getShippingsByOrderId(@PathVariable("orderId") Long orderId) {
		logger.info("getShippingsByOrderId called for orderId={}", orderId);
		ShippingEntityResponseDTO response = new ShippingEntityResponseDTO();
		if (orderId == null || orderId <= 0) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Order ID must be a valid positive number");
			return ResponseEntity.badRequest().body(response);
		}
		try {
			response = shippingService.getShippingsByOrderId(orderId);
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				// Check if it's a "not found" or other error
				if (response.getResponseMessage().contains("not found")) {
					return ResponseEntity.status(404).body(response);
				}
				return ResponseEntity.badRequest().body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in getShippingsByOrderId for orderId={}: {}", orderId, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while fetching shipping records. Please try again later.");
			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * POST /api/order/{orderId}/shipping
	 *
	 * <p>
	 * Create or update a shipping record for a given order ID.
	 * If a shipping record already exists for the order, it will be updated.
	 * Otherwise, a new record will be created.
	 * 
	 * <p>
	 * If tracking number is not provided in the request, it will be automatically
	 * generated in the format: TRK_{orderNumber}_{sequenceNumber}, where
	 * sequenceNumber is based on the total count of all shipment records
	 * (including cancelled ones) for this order.
	 * 
	 * <p>
	 * Order Status Handling:
	 * The API compares the existing order status in the Orders table with the
	 * orderStatus value provided in the request payload. If they differ:
	 * <ul>
	 * <li>The order status is updated in the orders table</li>
	 * <li>A new record is created in shipment_tracking_history with:
	 *     <ul>
	 *     <li>status: The new order status from the input</li>
	 *     <li>location: The shipping type (FORWARD or RETURN_PICKUP)</li>
	 *     <li>remarks: Description of the status change</li>
	 *     </ul>
	 * </li>
	 * </ul>
	 *
	 * Request body: CreateShippingRequestDTO with all shipping details including optional orderStatus.
	 */
	@PostMapping("/order/{orderId}/shipping")
	public ResponseEntity<ShippingEntityResponseDTO> createShippingForOrder(
			@PathVariable("orderId") Long orderId,
			@RequestBody CreateShippingRequestDTO request) {
		logger.info("createShippingForOrder called for orderId={}", orderId);
		ShippingEntityResponseDTO response = new ShippingEntityResponseDTO();
		if (orderId == null || orderId <= 0) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Order ID must be a valid positive number");
			return ResponseEntity.badRequest().body(response);
		}
		if (request == null) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Request body must not be null");
			return ResponseEntity.badRequest().body(response);
		}
		try {
			response = shippingService.createShippingForOrder(orderId, request);
			if (Constants.FAILURE_STATUS.equals(response.getResponseStatus())) {
				// Check if it's a "not found" or validation error
				if (response.getResponseMessage().contains("not found")) {
					return ResponseEntity.status(404).body(response);
				}
				return ResponseEntity.badRequest().body(response);
			}
			return ResponseEntity.status(201).body(response);
		}
		catch (Exception e) {
			logger.error("Error in createShippingForOrder for orderId={}: {}", orderId, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while creating/updating shipping record. Please try again later.");
			return ResponseEntity.status(500).body(response);
		}
	}

}
