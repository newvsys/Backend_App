package com.user.controller;

import com.user.dto.*;
import com.user.service.OrderService;
import com.user.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {

	@Autowired
	private OrderService orderService;

	private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

	@PostMapping("/orders")
	public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody OrderCreateDTO orderCreateDTO) {
		OrderResponseDTO createdOrder = new OrderResponseDTO();
		try {
			logger.info("Received createOrder request: {}", orderCreateDTO);

			if (orderCreateDTO.getProducts() == null || orderCreateDTO.getProducts().isEmpty()) {
				logger.warn("OrderCreateDTO.products is null or empty: {}", orderCreateDTO);
				createdOrder.setMessage(Constants.ORDER_PRODUCT_MISSING);
				createdOrder.setStatus(Constants.FAILURE_STATUS);
				return ResponseEntity.ok(createdOrder);
			}

			createdOrder = orderService.createOrder(orderCreateDTO);

		}
		catch (Exception e) {
			logger.error("Error in createOrder: {}", orderCreateDTO, e);
			createdOrder.setMessage(Constants.TECHNICAL_ERROR_CREATE_ORDER);
			createdOrder.setStatus("FAILED");
			return ResponseEntity.status(500).body(null);
		}
		return ResponseEntity.ok(createdOrder);
	}

	@GetMapping("/orderStatus/{order_id}")
	public ResponseEntity<?> getOrderStatus(@PathVariable("order_id") String orderId) {
		if (orderId == null || orderId.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "order_id is required"));
		}
		OrderStatusDTO orderStatus = orderService.getOrderStatus(orderId);
		return ResponseEntity.ok(orderStatus);
	}

	@GetMapping("/orders/{order_id}")
	public ResponseEntity<OrderDTO> getOrderById(@PathVariable("order_id") String orderId) {
		OrderDTO order = orderService.getOrderByOrderNumber(orderId);
		return ResponseEntity.ok(order);
	}

	@PutMapping("/orders/{order_id}")
	public ResponseEntity<OrderDTO> updateOrder(@PathVariable("order_id") String orderId,
			@RequestBody OrderCreateDTO orderUpdateDTO) {
		OrderDTO updatedOrder = orderService.updateOrder(orderId, orderUpdateDTO);
		return ResponseEntity.ok(updatedOrder);
	}

	@DeleteMapping("/orders/{order_id}")
	public ResponseEntity<OrderResponseDTO> deleteOrder(@PathVariable("order_id") String orderId) {
		OrderResponseDTO orderResponseDTO = orderService.updateOrderStatus(orderId, "D");
		return ResponseEntity.ok(orderResponseDTO);
	}

	@GetMapping("/order-history")
	public ResponseEntity<OrderHistoryResponseDTO> getOrderHistory(@RequestParam("userId") Long userId,
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "status", required = false) List<String> status) {
		try {
			OrderHistoryRequestDTO orderHistoryRequestDTO = new OrderHistoryRequestDTO();
			if (userId != null)
				orderHistoryRequestDTO.setUserId(userId);
			if (search != null && !search.isEmpty())
				orderHistoryRequestDTO.setSearch(search);
			if (status != null && !status.isEmpty())
				orderHistoryRequestDTO.setStatus(status);
			logger.info("Received getOrderHistory request for userId: {}, search: {}, status: {}", userId, search,
					status);
			OrderHistoryResponseDTO orderHistory = orderService.getOrderHistory(orderHistoryRequestDTO);
			return ResponseEntity.ok(orderHistory);
		}
		catch (Exception e) {
			logger.error("Error in getOrderHistory for userId: {}, search: {}, status: {}", userId, search, status, e);
			return ResponseEntity.status(500).body(null);
		}
	}

	@PostMapping("/create-reason")
	public ResponseEntity<ResponseDTO> createReason(@RequestBody CreateReasonRequestDTO createReasonRequestDTO) {
		ResponseDTO response = orderService.createReason(createReasonRequestDTO);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/reasons/type/{type}")
	public ResponseEntity<List<ReasonDetailsDTO>> getReasonsByType(@PathVariable("type") String type) {
		List<ReasonDetailsDTO> reasons = orderService.getReasonsByType(type);
		return ResponseEntity.ok(reasons);
	}

	// cancel order
	@PostMapping("/order-cancel")
	public ResponseEntity<ResponseDTO> orderCancel(@RequestBody OrderCancelRequestDTO orderCancelRequestDTO) {
		ResponseDTO response = new ResponseDTO();
		try {
			logger.info("Received orderCancel request: {}", orderCancelRequestDTO);

			if (orderCancelRequestDTO.getOrderNumber() == null
					|| orderCancelRequestDTO.getOrderNumber().trim().isEmpty()) {
				logger.warn("OrderCancelRequestDTO.orderNumber is null or empty: {}", orderCancelRequestDTO);
				response.setResponseMessage("Order number is required");
				response.setResponseStatus(Constants.FAILURE_STATUS);
				return ResponseEntity.ok(response);
			}

			response = orderService.cancelOrder(orderCancelRequestDTO);

		}
		catch (Exception e) {
			logger.error("Error in orderCancel: {}", orderCancelRequestDTO, e);
			response.setResponseMessage(Constants.ORDER_CANCEL_FAILURE);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			return ResponseEntity.status(500).body(response);
		}
		return ResponseEntity.ok(response);
	}

	// Return order
	@PostMapping(value = "/order-return", consumes = { "multipart/form-data" })
	public ResponseEntity<ResponseDTO> returnOrder(@RequestPart("returnOrderRequest") String returnOrderRequestJson,
			@RequestPart(value = "images", required = false) List<MultipartFile> images) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			ReturnOrderRequestDTO returnOrderRequestDTO = mapper.readValue(returnOrderRequestJson,
					ReturnOrderRequestDTO.class);
			ResponseDTO response = orderService.returnOrder(returnOrderRequestDTO, images);
			return ResponseEntity.ok(response);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Invalid JSON for product", e);
		}
	}

	@PostMapping("/refund")
	public ResponseEntity<ResponseDTO> processRefund(@RequestParam("refundReferenceNo") String refundReference) {
		ResponseDTO response = new ResponseDTO();
		try {
			logger.info("Received refund request for refundReference: {}", refundReference);
			if (refundReference == null || refundReference.trim().isEmpty()) {
				response.setResponseMessage(Constants.PAYMENT_REFUND_REFERENCE_MISSING);
				response.setResponseStatus(Constants.FAILURE_STATUS);
				return ResponseEntity.ok(response);
			}
			response = orderService.processRefundByReference(refundReference);
		}
		catch (Exception e) {
			logger.error("Error in processRefund for refundReference: {}", refundReference, e);
			response.setResponseMessage(Constants.PAYMENT_REFUND_REFERENCE_MISSING);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			return ResponseEntity.status(500).body(response);
		}
		return ResponseEntity.ok(response);
	}

	@GetMapping("/refunds")
	public ResponseEntity<List<RefundRecordDTO>> getRefunds(
			@RequestParam(value = "status", required = false) List<String> status,
			@RequestParam(value = "createdFrom", required = false) String createdFrom,
			@RequestParam(value = "createdTo", required = false) String createdTo,
			@RequestParam(value = "orderNumber", required = false) String orderNumber) {
		try {
			RefundSearchRequestDTO request = RefundSearchRequestDTO.builder()
				.status(status)
				.createdFrom(createdFrom != null && !createdFrom.isBlank() ? LocalDateTime.parse(createdFrom) : null)
				.createdTo(createdTo != null && !createdTo.isBlank() ? LocalDateTime.parse(createdTo) : null)
				.orderNumber(orderNumber)
				.build();
			List<RefundRecordDTO> refunds = orderService.getRefunds(request);
			return ResponseEntity.ok(refunds);
		}
		catch (Exception e) {
			logger.error("Error in getRefunds with status={}, createdFrom={}, createdTo={}, orderNumber={}", status,
					createdFrom, createdTo, orderNumber, e);
			return ResponseEntity.status(500).body(Collections.emptyList());
		}
	}

	@PostMapping("/refunds/approve")
	public ResponseEntity<ResponseDTO> updateRefundApprovedAmount(@RequestBody RefundApproveRequestDTO request) {
		ResponseDTO response = new ResponseDTO();
		try {
			if (request == null || request.getRefundReference() == null
					|| request.getRefundReference().trim().isEmpty()) {
				response.setResponseMessage(Constants.PAYMENT_REFUND_REFERENCE_MISSING);
				response.setResponseStatus(Constants.FAILURE_STATUS);
				return ResponseEntity.ok(response);
			}
			if (request.getApprovedAmount() == null) {
				response.setResponseMessage("Approved amount is required");
				response.setResponseStatus(Constants.FAILURE_STATUS);
				return ResponseEntity.ok(response);
			}
			response = orderService.updateRefundApprovedAmount(request);
		}
		catch (Exception e) {
			logger.error("Error in updateRefundApprovedAmount for refundReference: {}",
					request != null ? request.getRefundReference() : null, e);
			response.setResponseMessage("Failed to update approved amount");
			response.setResponseStatus(Constants.FAILURE_STATUS);
			return ResponseEntity.status(500).body(response);
		}
		return ResponseEntity.ok(response);
	}

	@PostMapping("/return-policies")
	public ResponseEntity<ResponseDTO> createReturnPolicy(@RequestBody ReturnPolicyCreateDTO request) {
		try {
			logger.info("Received createReturnPolicy request: {}", request);
			ResponseDTO response = orderService.createReturnPolicy(request);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in createReturnPolicy", e);
			return ResponseEntity.status(500).body(null);
		}
	}

	@PutMapping("/return-policies/{policy_id}")
	public ResponseEntity<ResponseDTO> updateReturnPolicy(@PathVariable("policy_id") Long policyId,
			@RequestBody ReturnPolicyCreateDTO request) {
		try {
			logger.info("Received updateReturnPolicy request: policyId={}, payload={}", policyId, request);
			ResponseDTO response = orderService.updateReturnPolicy(policyId, request);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in updateReturnPolicy for policyId={}", policyId, e);
			return ResponseEntity.status(500).body(null);
		}
	}

	@GetMapping("/return-policies")
	public ResponseEntity<List<ReturnPolicyResponseDTO>> getReturnPolicies() {
		try {
			logger.info("Received getReturnPolicies request");
			List<ReturnPolicyResponseDTO> response = orderService.getReturnPolicies();
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in getReturnPolicies", e);
			return ResponseEntity.status(500).body(Collections.emptyList());
		}
	}

	@PostMapping("/return-policy-conditions")
	public ResponseEntity<ResponseDTO> createReturnPolicyCondition(
			@RequestBody ReturnPolicyConditionCreateDTO request) {
		try {
			logger.info("Received createReturnPolicyCondition request: {}", request);
			ResponseDTO response = orderService.createReturnPolicyCondition(request);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in createReturnPolicyCondition", e);
			return ResponseEntity.status(500).body(null);
		}
	}

	@PutMapping("/return-policy-conditions/{condition_id}")
	public ResponseEntity<ResponseDTO> updateReturnPolicyCondition(@PathVariable("condition_id") Long conditionId,
			@RequestBody ReturnPolicyConditionCreateDTO request) {
		try {
			logger.info("Received updateReturnPolicyCondition request: conditionId={}, payload={}", conditionId,
					request);
			ResponseDTO response = orderService.updateReturnPolicyCondition(conditionId, request);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in updateReturnPolicyCondition for conditionId={}", conditionId, e);
			return ResponseEntity.status(500).body(null);
		}
	}

	@GetMapping("/return-policy-conditions")
	public ResponseEntity<List<ReturnPolicyConditionResponseDTO>> getReturnPolicyConditions(
			@RequestParam(value = "policyId", required = false) Long policyId) {
		try {
			logger.info("Received getReturnPolicyConditions request: policyId={}", policyId);
			List<ReturnPolicyConditionResponseDTO> response = orderService.getReturnPolicyConditions(policyId);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in getReturnPolicyConditions", e);
			return ResponseEntity.status(500).body(Collections.emptyList());
		}
	}

	@PostMapping("/return-policy-mappings")
	public ResponseEntity<ResponseDTO> createReturnPolicyMapping(@RequestBody ReturnPolicyMappingCreateDTO request) {
		try {
			logger.info("Received createReturnPolicyMapping request: {}", request);
			ResponseDTO response = orderService.createReturnPolicyMapping(request);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in createReturnPolicyMapping", e);
			return ResponseEntity.status(500).body(null);
		}
	}

	/**
	 * PUT /api/return-policy-mappings/{mappingId} Update an existing return-policy
	 * mapping (policyId and/or priority only). entityType and entityId are immutable.
	 */
	@PutMapping("/return-policy-mappings/{mappingId}")
	public ResponseEntity<ResponseDTO> updateReturnPolicyMapping(@PathVariable("mappingId") Long mappingId,
			@RequestBody ReturnPolicyMappingCreateDTO request) {
		try {
			logger.info("Received updateReturnPolicyMapping: mappingId={}, payload={}", mappingId, request);
			ResponseDTO response = orderService.updateReturnPolicyMapping(mappingId, request);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in updateReturnPolicyMapping for mappingId={}", mappingId, e);
			return ResponseEntity.status(500).body(null);
		}
	}

	/**
	 * DELETE /api/return-policy-mappings/{mappingId} Permanently delete a return-policy
	 * mapping.
	 */
	@DeleteMapping("/return-policy-mappings/{mappingId}")
	public ResponseEntity<ResponseDTO> deleteReturnPolicyMapping(@PathVariable("mappingId") Long mappingId) {
		try {
			logger.info("Received deleteReturnPolicyMapping: mappingId={}", mappingId);
			ResponseDTO response = orderService.deleteReturnPolicyMapping(mappingId);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in deleteReturnPolicyMapping for mappingId={}", mappingId, e);
			return ResponseEntity.status(500).body(null);
		}
	}

	/**
	 * GET /api/return-policy-mappings List return-policy mappings. Optional filters:
	 * entityType (e.g. PRODUCTS / CATEGORY / GLOBAL), entityId.
	 */
	@GetMapping("/return-policy-mappings")
	public ResponseEntity<List<ReturnPolicyMappingResponseDTO>> getReturnPolicyMappings(
			@RequestParam(value = "entityType", required = false) String entityType,
			@RequestParam(value = "entityId", required = false) Long entityId) {
		try {
			logger.info("Received getReturnPolicyMappings: entityType={}, entityId={}", entityType, entityId);
			List<ReturnPolicyMappingResponseDTO> response = orderService.getReturnPolicyMappings(entityType, entityId);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in getReturnPolicyMappings", e);
			return ResponseEntity.status(500).body(Collections.emptyList());
		}
	}

	@GetMapping("/return-policy/product-variant/{productVariantId}")
	public ResponseEntity<ReturnPolicyDetailDTO> getReturnPolicyByProductVariantId(
			@PathVariable("productVariantId") Long productVariantId) {
		try {
			logger.info("Received getReturnPolicyByProductVariantId request: productVariantId={}", productVariantId);
			ReturnPolicyDetailDTO response = orderService.getReturnPolicyByProductVariantId(productVariantId);
			if (response == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in getReturnPolicyByProductVariantId for productVariantId={}", productVariantId, e);
			return ResponseEntity.status(500).body(null);
		}
	}

	/**
	 * GET /api/return-policy/by-category?categoryIds=1,2,3
	 *
	 * Returns the effective return policy for each supplied product-category ID. Lookup
	 * order: CATEGORY mapping (highest priority) → GLOBAL fallback → null.
	 */
	@GetMapping("/return-policy/by-category")
	public ResponseEntity<List<CategoryReturnPolicyResponseDTO>> getReturnPoliciesByCategories(
			@RequestParam("categoryIds") List<Long> categoryIds) {
		try {
			logger.info("Received getReturnPoliciesByCategories request: categoryIds={}", categoryIds);
			if (categoryIds == null || categoryIds.isEmpty()) {
				return ResponseEntity.badRequest().build();
			}
			List<CategoryReturnPolicyResponseDTO> response = orderService.getReturnPoliciesByCategories(categoryIds);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in getReturnPoliciesByCategories for categoryIds={}", categoryIds, e);
			return ResponseEntity.status(500).body(Collections.emptyList());
		}
	}

	@GetMapping("/return-requests")
	public ResponseEntity<List<ReturnRequestResponseDTO>> getReturnRequests(
			@RequestParam(value = "orderNumber", required = false) String orderNumber,
			@RequestParam(value = "status", required = false) String status) {
		try {
			logger.info("Received getReturnRequests request: orderNumber={}, status={}", orderNumber, status);
			List<ReturnRequestResponseDTO> response = orderService.getReturnRequests(orderNumber, status);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in getReturnRequests for orderNumber={}, status={}", orderNumber, status, e);
			return ResponseEntity.status(500).body(Collections.emptyList());
		}
	}

	@PostMapping("/return-requests/approve")
	public ResponseEntity<ResponseDTO> approveReturnRequest(@RequestBody ReturnApproveRequestDTO request) {
		ResponseDTO response = new ResponseDTO();
		try {
			logger.info("Received approveReturnRequest: returnId={}, status={}, userId={}",
					request != null ? request.getReturnId() : null, request != null ? request.getStatus() : null,
					request != null ? request.getUserId() : null);

			if (request == null || request.getReturnId() == null || request.getReturnId().trim().isEmpty()) {
				response.setResponseMessage("Return ID is required");
				response.setResponseStatus(Constants.FAILURE_STATUS);
				return ResponseEntity.ok(response);
			}
			if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
				response.setResponseMessage("Status is required (APPROVED / REJECTED)");
				response.setResponseStatus(Constants.FAILURE_STATUS);
				return ResponseEntity.ok(response);
			}
			if (request.getUserId() == null) {
				response.setResponseMessage("User ID is required");
				response.setResponseStatus(Constants.FAILURE_STATUS);
				return ResponseEntity.ok(response);
			}

			response = orderService.approveReturnRequest(request);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in approveReturnRequest for returnId={}",
					request != null ? request.getReturnId() : null, e);
			response.setResponseMessage("Failed to process return request");
			response.setResponseStatus(Constants.FAILURE_STATUS);
			return ResponseEntity.status(500).body(response);
		}
	}

	// ── Reason Master CRUD ──────────────────────────────────────────────────

	// ── Order + Shipment combined view ──────────────────────────────────────

	/**
	 * GET /api/order-shipment-details Fetch orders with their shipment details and
	 * shipment tracking history. Filters (all optional): - status : order status (e.g.
	 * PENDING, SHIPPED, DELIVERED) - orderCreatedFrom: ISO date-time lower bound for
	 * order creation (inclusive) - orderCreatedTo : ISO date-time upper bound for order
	 * creation (inclusive) - orderNumber : exact order number - shipmentNumber : courier
	 * tracking / AWB number of any linked shipment
	 */
	@GetMapping("/order-shipment-details")
	public ResponseEntity<OrderShipmentListResponseDTO> getOrderShipmentDetails(
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "orderCreatedFrom", required = false) String orderCreatedFrom,
			@RequestParam(value = "orderCreatedTo", required = false) String orderCreatedTo,
			@RequestParam(value = "orderNumber", required = false) String orderNumber,
			@RequestParam(value = "shipmentNumber", required = false) String shipmentNumber) {
		try {
			logger.info(
					"getOrderShipmentDetails called: status={}, orderCreatedFrom={}, orderCreatedTo={}, orderNumber={}, shipmentNumber={}",
					status, orderCreatedFrom, orderCreatedTo, orderNumber, shipmentNumber);

			OrderShipmentSearchRequestDTO request = OrderShipmentSearchRequestDTO.builder()
				.orderStatus(status)
				.orderCreatedFrom(parseDateTime(orderCreatedFrom, false))
				.orderCreatedTo(parseDateTime(orderCreatedTo, true))
				.orderNumber(orderNumber)
				.shipmentNumber(shipmentNumber)
				.build();

			OrderShipmentListResponseDTO response = orderService.getOrdersWithShipments(request);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error in getOrderShipmentDetails: {}", e.getMessage(), e);
			return ResponseEntity.status(500)
				.body(OrderShipmentListResponseDTO.builder().totalCount(0).orders(Collections.emptyList()).build());
		}
	}

	/**
	 * GET /api/reasons List all reasons. Optional filters: status ('A'/'I'), type
	 * ('CANCELLATION'/'RETURN'/'EXCHANGE')
	 */
	@GetMapping("/reasons")
	public ResponseEntity<ReasonListResponseDTO> getAllReasons(
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "type", required = false) String type) {
		logger.info("getAllReasons called with status={}, type={}", status, type);
		ReasonListResponseDTO response = orderService.getAllReasons(status, type);
		return ResponseEntity.ok(response);
	}

	/**
	 * GET /api/reason/{id} Get a single reason by ID (any status).
	 */
	@GetMapping("/reason/{id}")
	public ResponseEntity<ReasonResponseDTO> getReasonById(@PathVariable("id") Long id) {
		logger.info("getReasonById called with id={}", id);
		if (id == null) {
			return ResponseEntity.badRequest().build();
		}
		ReasonResponseDTO response = orderService.getReasonById(id);
		return ResponseEntity.ok(response);
	}

	/**
	 * PUT /api/reason/{id} Update reason description and/or type. reasonCode is
	 * immutable.
	 */
	@PutMapping("/reason/{id}")
	public ResponseEntity<ReasonResponseDTO> updateReason(@PathVariable("id") Long id,
			@RequestBody UpdateReasonRequestDTO request) {
		logger.info("updateReason called with id={}", id);
		if (id == null || request == null) {
			return ResponseEntity.badRequest().build();
		}
		ReasonResponseDTO response = orderService.updateReason(id, request);
		return ResponseEntity.ok(response);
	}

	/**
	 * DELETE /api/reason/{id} Soft-delete: only changes status. No physical row deletion.
	 * Body: {"status": "I"} to deactivate, {"status": "A"} to reactivate.
	 */
	@DeleteMapping("/reason/{id}")
	public ResponseEntity<ResponseDTO> deleteReason(@PathVariable("id") Long id,
			@RequestBody ReasonStatusChangeRequestDTO request) {
		logger.info("deleteReason (status change) called with id={}, newStatus={}", id,
				request != null ? request.getStatus() : null);
		if (id == null) {
			return ResponseEntity.badRequest().build();
		}
		if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
			return ResponseEntity.badRequest()
				.body(ResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage("Request body with 'status' field is required")
					.build());
		}
		ResponseDTO response = orderService.deleteReason(id, request);
		return ResponseEntity.ok(response);
	}

	/**
	 * Parses a date or date-time string into a LocalDateTime. Supports both date-only
	 * format (yyyy-MM-dd) and ISO date-time (yyyy-MM-ddTHH:mm:ss).
	 * @param value the raw string value from query param
	 * @param endOfDay if true and a date-only value is provided, returns end-of-day
	 * (23:59:59); otherwise returns start-of-day (00:00:00)
	 */
	private LocalDateTime parseDateTime(String value, boolean endOfDay) {
		if (value == null || value.isBlank())
			return null;
		try {
			return LocalDateTime.parse(value);
		}
		catch (DateTimeParseException e) {
			LocalDate date = LocalDate.parse(value);
			return endOfDay ? date.atTime(LocalTime.of(23, 59, 59)) : date.atStartOfDay();
		}
	}

}
