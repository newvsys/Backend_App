package com.user.service;

import com.user.communication.event.OrderEvent;
import com.user.communication.event.RefundInitiatedEvent;
import com.user.dto.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface OrderService {

	OrderResponseDTO createOrder(OrderCreateDTO orderCreateDTO);

	OrderDTO getOrderByOrderNumber(String orderId);

	OrderDTO updateOrder(String orderId, OrderCreateDTO orderUpdateDTO);

	OrderResponseDTO updateOrderStatus(String orderId, String status);

	OrderStatusDTO getOrderStatus(String orderId);

	OrderHistoryResponseDTO getOrderHistory(OrderHistoryRequestDTO orderHistoryRequestDTO);

	ResponseDTO updateOrderPaymentStatus(PaymentStatusUpdateDTO paymentStatusUpdateDTO);

	ResponseDTO cancelOrder(OrderCancelRequestDTO orderCancelRequestDTO);

	void processCancelOrderEvent(OrderEvent event);

	void processReturnOrderEvent(OrderEvent event);

	ResponseDTO createReason(CreateReasonRequestDTO createReasonRequestDTO);

	List<ReasonDetailsDTO> getReasonsByType(String type);

	ResponseDTO returnOrder(ReturnOrderRequestDTO returnOrderRequestDTO, List<MultipartFile> returnImage);

	ResponseDTO processRefundByReference(String refundReference);

	List<RefundRecordDTO> getRefunds(RefundSearchRequestDTO request);

	ResponseDTO updateRefundApprovedAmount(RefundApproveRequestDTO request);

	void processRefundInitiatedEvent(RefundInitiatedEvent event);

	ResponseDTO createReturnPolicy(ReturnPolicyCreateDTO request);

	ResponseDTO updateReturnPolicy(Long policyId, ReturnPolicyCreateDTO request);

	List<ReturnPolicyResponseDTO> getReturnPolicies();

	ResponseDTO createReturnPolicyCondition(ReturnPolicyConditionCreateDTO request);

	ResponseDTO updateReturnPolicyCondition(Long conditionId, ReturnPolicyConditionCreateDTO request);

	List<ReturnPolicyConditionResponseDTO> getReturnPolicyConditions(Long policyId);

	ResponseDTO createReturnPolicyMapping(ReturnPolicyMappingCreateDTO request);

	/**
	 * Update an existing return-policy mapping by its ID. Only {@code policyId} and
	 * {@code priority} can be changed. {@code entityType} and {@code entityId} are
	 * immutable after creation.
	 */
	ResponseDTO updateReturnPolicyMapping(Long mappingId, ReturnPolicyMappingCreateDTO request);

	/**
	 * Delete a return-policy mapping by its ID.
	 */
	ResponseDTO deleteReturnPolicyMapping(Long mappingId);

	/**
	 * List all return-policy mappings, optionally filtered by entityType and/or entityId.
	 */
	List<ReturnPolicyMappingResponseDTO> getReturnPolicyMappings(String entityType, Long entityId);

	ReturnPolicyDetailDTO getReturnPolicyByProductVariantId(Long productVariantId);

	/**
	 * Returns the effective return policy for each of the supplied product-category IDs.
	 * Lookup order per category: CATEGORY mapping → GLOBAL mapping → null. The result
	 * list preserves input order; categories with no mapping have
	 * {@code returnPolicy = null}.
	 */
	List<CategoryReturnPolicyResponseDTO> getReturnPoliciesByCategories(List<Long> categoryIds);

	List<ReturnRequestResponseDTO> getReturnRequests(String orderNumber, String status);

	ResponseDTO approveReturnRequest(ReturnApproveRequestDTO request);

	// ── Reason Master CRUD ──────────────────────────────────────────────────
	ReasonListResponseDTO getAllReasons(String status, String type);

	ReasonResponseDTO getReasonById(Long id);

	ReasonResponseDTO updateReason(Long id, UpdateReasonRequestDTO request);

	ResponseDTO deleteReason(Long id, ReasonStatusChangeRequestDTO request);

	// ── Order + Shipment combined view ──────────────────────────────────────
	OrderShipmentListResponseDTO getOrdersWithShipments(OrderShipmentSearchRequestDTO request);

	// ── Retry Payment ────────────────────────────────────────────────────────
	OrderResponseDTO retryPayment(RetryPaymentRequestDTO request);

}
