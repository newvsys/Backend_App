package com.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.communication.event.EmailDetails;
import com.user.communication.event.Event;
import com.user.communication.event.RefundInitiatedEvent;
import com.user.communication.event.ShiprocketOrderEvent;
import com.user.communication.service.NotificationService;
import com.user.dto.*;
import com.user.model.*;
import com.user.repository.*;

import com.user.utility.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Service
public class ShippingServiceImpl implements ShippingService {

	@Value("${shiprocket.api.base-url}")
	private String baseUrl;

	// Add these missing URL fields

	@Value("${shiprocket.api.base-url}/v1/external/courier/generate/label")
	private String labelUrl;

	@Value("${shiprocket.api.base-url}/v1/external/manifests/generate")
	private String manifestUrl;

	@Value("${shiprocket.api.base-url}/v1/external/courier/track/awb")
	private String trackUrl;

	@Autowired
	private ShippingRepository shippingRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private OrderItemRepository orderItemRepository;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private ShipmentItemRepository shippingItemRepository;

	@Autowired
	private ShipmentTrackingHistoryRepository shipmentTrackingHistoryRepository;

	@Autowired
	private RefundTransactionRepository refundTransactionRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	@Lazy
	private OrderService orderService;

	@Autowired
	private ReturnRequestRepository returnRequestRepository;

	@Autowired
	private ReturnStatusHistoryRepository returnStatusHistoryRepository;

	@Autowired
	private ShiprocketService shiprocketService;

	@Autowired
	private OrderAddressRepository orderAddressRepository;

	@Autowired
	private CartonRepository cartonRepository;

	@Autowired
	private CartonSelectionService cartonSelectionService;

	@Autowired
	private ShiprocketAuthService authService;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ShiprocketOrderLogRepository shiprocketOrderLogRepository;

	@Autowired
	private PushNotificationService pushNotificationService;

	@Autowired
	private CourierSelectionLogRepository courierSelectionLogRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private ShiprocketOrderStatusHistoryRepository shiprocketOrderStatusHistoryRepository;

	@Autowired
	private GenerateAwbStatusHistoryRepository generateAwbStatusHistoryRepository;

	@Autowired
	private RequestPickupStatusHistoryRepository requestPickupStatusHistoryRepository;

	@Autowired
	private GenerateLabelStatusHistoryRepository generateLabelStatusHistoryRepository;

	@Autowired
	private TrackShipmentStatusHistoryRepository trackShipmentStatusHistoryRepository;

	@Autowired
	private EstimateStatusHistoryRepository estimateStatusHistoryRepository;

	private static final Logger logger = LoggerFactory.getLogger(ShippingServiceImpl.class);

	/**
	 * Returns the postal code of the given warehouse by name, or empty string if not
	 * found.
	 */
	private String getWarehousePostalCode(String warehouseName) {
		if (warehouseName == null || warehouseName.isBlank())
			return "";
		return warehouseRepository.findByWarehouseNameIgnoreCaseAndStatus(warehouseName, Constants.STATUS_ACTIVE)
			.map(w -> w.getPostalCode() != null ? w.getPostalCode() : "")
			.orElse("");
	}

	@Override
	public ShippingResponseDTO processCreateShipmentEvent(ShippingRequestDTO shippingDTO) {
		if (shippingDTO == null || shippingDTO.getOrderId() == null) {
			return ShippingResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("orderId must not be null")
				.build();
		}

		// Fetch entities by ID to avoid LazyInitializationException
		OrderEO order = orderRepository.findById(shippingDTO.getOrderId()).orElse(null);

		if (order == null) {
			return ShippingResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("Order not found for orderId=" + shippingDTO.getOrderId())
				.orderId(shippingDTO.getOrderId())
				.build();
		}
		List<Long> createdShipmentIds = new ArrayList<>();
		try {
			// Idempotency guard: skip if a FORWARD shipment already exists for this
			// order
			List<ShippingEO> existingShipments = shippingRepository.findByOrder(order);
			Optional<ShippingEO> existingForwardShipment = existingShipments == null ? Optional.empty()
					: existingShipments.stream()
						.filter(s -> Constants.SHIPMENT_TYPE_FORWARD.equals(s.getType())
								&& !Constants.SHIPMENT_STATUS_CANCELLED.equals(s.getShipmentStatus()))
						.findFirst();
			boolean forwardShipmentExists = existingForwardShipment.isPresent();
			// Only treat the shipment as fully processed (and skip re-creation) when
			// a FORWARD shipment exists AND every Shiprocket processing step
			// (order creation, AWB generation, pickup request, label generation,
			// track shipment) has already completed successfully. If any step is
			// missing/failed, allow the flow to continue so it can be retried/
			// completed instead of silently exiting.
			boolean allStepsSuccessful = existingForwardShipment
				.map(s -> Constants.SUCCESS_STATUS.equals(s.getShiprocketOrderStatus())
						&& Constants.SUCCESS_STATUS.equals(s.getGenerateAwbStatus())
						&& Constants.SUCCESS_STATUS.equals(s.getRequestPickupStatus())
						&& Constants.SUCCESS_STATUS.equals(s.getGenerateLabelStatus())
						&& Constants.SUCCESS_STATUS.equals(s.getTrackShipmentStatus()))
				.orElse(false);
			if (forwardShipmentExists && allStepsSuccessful) {
				logger.warn(
						"processCreateShipmentEvent: active FORWARD shipment already exists and fully processed for orderId={}, skipping duplicate creation",
						order.getOrderId());
				return ShippingResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage(
							"An active FORWARD shipment already exists for orderId=" + order.getOrderId())
					.orderId(shippingDTO.getOrderId())
					.build();
			}

			// A FORWARD shipment already exists but one or more Shiprocket
			// processing steps previously failed/are missing. Do NOT create new
			// ShippingEO / ShipmentTrackingHistoryEO / ShipmentItemEO records —
			// simply rebuild the event for the existing shipment and re-trigger
			// Shiprocket processing so the remaining steps can be retried/completed.
			if (forwardShipmentExists && !allStepsSuccessful) {
				ShippingEO existingShippingEO = existingForwardShipment.get();
				logger.info(
						"processCreateShipmentEvent: active FORWARD shipment exists but not fully processed for orderId={}, shipmentId={}, resuming Shiprocket processing without creating new records",
						order.getOrderId(), existingShippingEO.getShipmentId());
				ShiprocketOrderEvent shiprocketEvent = ShiprocketOrderEvent.builder()
					.shipmentId(existingShippingEO.getShipmentId() != null
							? existingShippingEO.getShipmentId().longValue() : null)
					.orderId(order.getOrderId() != null ? order.getOrderId().longValue() : null)
					.warehouseId(existingShippingEO.getWarehouse() != null
							? existingShippingEO.getWarehouse().getWarehouseId() : null)
					.cartonNo(shippingDTO.getCartonNo())
					.requestCreateCartonDTO(shippingDTO.getRequestCreateCartonDTO())
					.bestCourierId(shippingDTO.getBestCourierId())
					.build();
				ShiprocketOrderEventResponseDTO shiprocketOrderEventResponseDTO = processShiprocketOrderEvent(
						shiprocketEvent);
				logger.info(
						"Resumed Shiprocket order processing for existing shipmentId={}, orderId={}, warehouseId={}",
						shiprocketEvent.getShipmentId(), shiprocketEvent.getOrderId(),
						shiprocketEvent.getWarehouseId());
				createdShipmentIds.add(existingShippingEO.getShipmentId());
				return ShippingResponseDTO.builder()
					.responseStatus(Constants.SUCCESS_STATUS)
					.responseMessage(
							"Resumed Shiprocket processing for existing FORWARD shipment for orderId="
									+ order.getOrderId())
					.orderId(shippingDTO.getOrderId())
					.shipmentIds(createdShipmentIds)
					.build();
			}

			List<OrderItemEO> orderItems = orderItemRepository.findByOrder(order);

			// ── N+1 fix: batch-load all inventory records in a single query
			// ──────────
			List<ProductVariantEO> variants = orderItems.stream()
				.map(OrderItemEO::getProductVar)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
			Map<Integer, InventoryEO> inventoryByVariantId = inventoryRepository.findByProductVariantIn(variants)
				.stream()
				.filter(inv -> inv.getProductVariant() != null)
				.collect(
						Collectors.toMap(inv -> inv.getProductVariant().getId(), Function.identity(), (a, b) -> a));
			// ─────────────────────────────────────────────────────────────────────────

			// Group order items by warehouse ID
			Map<Long, List<OrderItemEO>> warehouseItemMap = new LinkedHashMap<>();
			Map<Long, WarehouseEO> warehouseById = new LinkedHashMap<>();
			for (OrderItemEO item : orderItems) {
				ProductVariantEO productVariantEO = item.getProductVar();
				InventoryEO inventoryEO = null;
				if (productVariantEO != null) {
					inventoryEO = inventoryByVariantId.get(productVariantEO.getId());
				}
				if (inventoryEO != null && inventoryEO.getWarehouse() != null) {
					WarehouseEO warehouseEO = inventoryEO.getWarehouse();
					Long warehouseId = warehouseEO.getWarehouseId();
					warehouseItemMap.computeIfAbsent(warehouseId, k -> new ArrayList<>()).add(item);
					warehouseById.putIfAbsent(warehouseId, warehouseEO);
				}
			}

			if (warehouseItemMap.isEmpty()) {
				logger.warn(
						"processCreateShipmentEvent: no warehouse-mapped items found for orderId={}, cannot create shipment",
						order.getOrderId());
				return ShippingResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage(
							"No warehouse-mapped items found for orderId=" + order.getOrderId()
									+ ", cannot create shipment")
					.orderId(shippingDTO.getOrderId())
					.build();
			}
			// For each warehouse, create a shipment and shipment items
			for (Map.Entry<Long, List<OrderItemEO>> entry : warehouseItemMap.entrySet()) {
				WarehouseEO warehouseEO = warehouseById.get(entry.getKey());
				List<OrderItemEO> itemsForWarehouse = entry.getValue();
				ShippingEO shippingEO = new ShippingEO();
				shippingEO.setOrder(order);
				String orderNumber = (order.getOrderNumber() != null) ? order.getOrderNumber() : "UNKNOWN";
				shippingEO.setTrackingNumber("TRK" + orderNumber + "_" + warehouseEO.getWarehouseId());
				// shippingEO.setCourierName(Constants.COURIER_NAME);
				shippingEO.setShipmentStatus(Constants.SHIPMENT_STATUS_CREATED);
				shippingEO.setWarehouse(warehouseEO);
				shippingEO.setType(Constants.SHIPMENT_TYPE_FORWARD);
				ShippingEO savedShippingEO = shippingRepository.save(shippingEO);
				createdShipmentIds.add(savedShippingEO.getShipmentId());
				ShipmentTrackingHistoryEO shipmentTrackingHistoryEO = new ShipmentTrackingHistoryEO();
				shipmentTrackingHistoryEO.setShipment(savedShippingEO);
				shipmentTrackingHistoryEO.setStatus(Constants.SHIPMENT_ORDER_STATUS_CREATED);
				shipmentTrackingHistoryEO.setLocation(warehouseEO.getAddressLine1() + ", "
						+ warehouseEO.getAddressLine2() + "," + warehouseEO.getCity() + ", "
						+ warehouseEO.getState() + " - " + warehouseEO.getPostalCode());
				shipmentTrackingHistoryEO.setRemarks(Constants.SHIPMENT_ORDER_STATUS_CREATED_REMARK);
				shipmentTrackingHistoryRepository.save(shipmentTrackingHistoryEO);

				// Save ShipmentItemEO records so downstream processing can fetch them
				for (OrderItemEO item : itemsForWarehouse) {
					ShipmentItemEO shipmentItemEO = new ShipmentItemEO();
					shipmentItemEO.setShipment(savedShippingEO);
					shipmentItemEO.setOrderItem(item);
					shipmentItemEO.setQuantity(item.getQuantity());
					shippingItemRepository.save(shipmentItemEO);
				}

				// Build event and trigger Shiprocket order creation directly (in-process)
				ShiprocketOrderEvent shiprocketEvent = ShiprocketOrderEvent.builder()
					.shipmentId(savedShippingEO.getShipmentId() != null
							? savedShippingEO.getShipmentId().longValue() : null)
					.orderId(order.getOrderId() != null ? order.getOrderId().longValue() : null)
					.warehouseId(warehouseEO.getWarehouseId())
					.cartonNo(shippingDTO.getCartonNo())
					.requestCreateCartonDTO(shippingDTO.getRequestCreateCartonDTO())
					.bestCourierId(shippingDTO.getBestCourierId())
					.build();
				// Directly trigger Shiprocket order creation
				ShiprocketOrderEventResponseDTO shiprocketOrderEventResponseDTO=processShiprocketOrderEvent(shiprocketEvent);
				logger.info("Triggered Shiprocket order creation for shipmentId={}, orderId={}, warehouseId={}",
						shiprocketEvent.getShipmentId(), shiprocketEvent.getOrderId(),
						shiprocketEvent.getWarehouseId());
			}

			return ShippingResponseDTO.builder()
				.responseStatus(Constants.SUCCESS_STATUS)
				.responseMessage("Shipment(s) created successfully for orderId=" + order.getOrderId())
				.orderId(shippingDTO.getOrderId())
				.shipmentIds(createdShipmentIds)
				.build();
		}
		catch (Exception e) {
			logger.error("processCreateShipmentEvent: error creating shipment for orderId={}: {}",
					order.getOrderId(), e.getMessage(), e);
			return ShippingResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("An error occurred while creating shipment: " + e.getMessage())
				.orderId(shippingDTO.getOrderId())
				.shipmentIds(createdShipmentIds)
				.build();
		}
	}

	// ─── Helper: create and persist a new CartonEO from a RequestCreateCartonDTO ──
	private CartonEO createCartonFromRequest(com.user.dto.RequestCreateCartonDTO requestCreateCartonDTO) {
		CartonEO cartonEO = CartonEO.builder()
			.name(requestCreateCartonDTO.getName())
			.length(requestCreateCartonDTO.getLength())
			.breadth(requestCreateCartonDTO.getBreadth())
			.height(requestCreateCartonDTO.getHeight())
			.maxWeight(requestCreateCartonDTO.getMaxWeight())
			.emptyWeight(requestCreateCartonDTO.getEmptyWeight())
			.status("A")
			.who(requestCreateCartonDTO.getWho())
			.build();
		return cartonRepository.save(cartonEO);
	}

	// ─── Helper: create and persist a brand-new step log row ────────────────
	private ShiprocketOrderLogEO saveStepLog(ShiprocketOrderEvent event, String step, String status,
			String errorMessage) {
		ShiprocketOrderLogEO stepLog = ShiprocketOrderLogEO.builder()
			.shipmentId(event.getShipmentId())
			.orderId(event.getOrderId())
			.warehouseId(event.getWarehouseId())
			.step(step)
			.status(status)
			.errorMessage(errorMessage)
			.build();
		return shiprocketOrderLogRepository.save(stepLog);
	}

	// ─── Helpers: persist per-step status onto ShippingEO and record a
	// corresponding history row for auditability. ────────────────────────────
	private void recordShiprocketOrderStatus(ShippingEO shippingEO, String status, String remarks) {
		try {
			shippingEO.setShiprocketOrderStatus(status);
			shippingRepository.save(shippingEO);
			shiprocketOrderStatusHistoryRepository.save(ShiprocketOrderStatusHistoryEO.builder()
				.shipment(shippingEO)
				.status(status)
				.remarks(remarks)
				.build());
		}
		catch (Exception ex) {
			logger.warn("recordShiprocketOrderStatus: failed to persist status={} for shipmentId={}: {}", status,
					shippingEO != null ? shippingEO.getShipmentId() : null, ex.getMessage());
		}
	}

	private void recordGenerateAwbStatus(ShippingEO shippingEO, String status, String remarks) {
		try {
			shippingEO.setGenerateAwbStatus(status);
			shippingRepository.save(shippingEO);
			generateAwbStatusHistoryRepository.save(GenerateAwbStatusHistoryEO.builder()
				.shipment(shippingEO)
				.status(status)
				.remarks(remarks)
				.build());
		}
		catch (Exception ex) {
			logger.warn("recordGenerateAwbStatus: failed to persist status={} for shipmentId={}: {}", status,
					shippingEO != null ? shippingEO.getShipmentId() : null, ex.getMessage());
		}
	}

	private void recordRequestPickupStatus(ShippingEO shippingEO, String status, String remarks) {
		try {
			shippingEO.setRequestPickupStatus(status);
			shippingRepository.save(shippingEO);
			requestPickupStatusHistoryRepository.save(RequestPickupStatusHistoryEO.builder()
				.shipment(shippingEO)
				.status(status)
				.remarks(remarks)
				.build());
		}
		catch (Exception ex) {
			logger.warn("recordRequestPickupStatus: failed to persist status={} for shipmentId={}: {}", status,
					shippingEO != null ? shippingEO.getShipmentId() : null, ex.getMessage());
		}
	}

	private void recordGenerateLabelStatus(ShippingEO shippingEO, String status, String remarks) {
		try {
			shippingEO.setGenerateLabelStatus(status);
			shippingRepository.save(shippingEO);
			generateLabelStatusHistoryRepository.save(GenerateLabelStatusHistoryEO.builder()
				.shipment(shippingEO)
				.status(status)
				.remarks(remarks)
				.build());
		}
		catch (Exception ex) {
			logger.warn("recordGenerateLabelStatus: failed to persist status={} for shipmentId={}: {}", status,
					shippingEO != null ? shippingEO.getShipmentId() : null, ex.getMessage());
		}
	}

	private void recordTrackShipmentStatus(ShippingEO shippingEO, String status, String remarks) {
		try {
			shippingEO.setTrackShipmentStatus(status);
			shippingRepository.save(shippingEO);
			trackShipmentStatusHistoryRepository.save(TrackShipmentStatusHistoryEO.builder()
				.shipment(shippingEO)
				.status(status)
				.remarks(remarks)
				.build());
		}
		catch (Exception ex) {
			logger.warn("recordTrackShipmentStatus: failed to persist status={} for shipmentId={}: {}", status,
					shippingEO != null ? shippingEO.getShipmentId() : null, ex.getMessage());
		}
	}

	private void recordEstimateStatus(ShippingEO shippingEO, String status, String remarks) {
		try {
			shippingEO.setEstimateStatus(status);
			shippingRepository.save(shippingEO);
			estimateStatusHistoryRepository.save(EstimateStatusHistoryEO.builder()
				.shipment(shippingEO)
				.status(status)
				.remarks(remarks)
				.build());
		}
		catch (Exception ex) {
			logger.warn("recordEstimateStatus: failed to persist status={} for shipmentId={}: {}", status,
					shippingEO != null ? shippingEO.getShipmentId() : null, ex.getMessage());
		}
	}

	// ─── Helper: mark the order Ready to Ship whenever any required shipment
	// detail (carton, Shiprocket order/AWB, courier, label) could not be
	// generated. The already-created shipping / shipment_item /
	// shipment_tracking_history / shiprocket_order_log records are left as-is
	// (with whatever details are available) — only the Order status is
	// updated so downstream processes can pick this up for manual completion.
	private void markOrderReadyToShip(OrderEO order, ShippingEO shippingEO, String reason) {
		if (order == null) {
			return;
		}
		try {
			order.setOrderStatus(Constants.ORDER_STATUS_READY_TO_SHIP);
			orderRepository.save(order);
			logger.info("Order status updated to Ready to Ship for orderId={}: {}", order.getOrderId(), reason);

			if (shippingEO != null && !shipmentTrackingHistoryRepository
					.existsByShipmentAndStatusIgnoreCase(shippingEO, Constants.ORDER_STATUS_READY_TO_SHIP)) {
				ShipmentTrackingHistoryEO readyHistory = new ShipmentTrackingHistoryEO();
				readyHistory.setShipment(shippingEO);
				readyHistory.setStatus(Constants.ORDER_STATUS_READY_TO_SHIP);
				readyHistory.setRemarks(reason);
				readyHistory.setUpdatedAt(LocalDateTime.now());
				shipmentTrackingHistoryRepository.save(readyHistory);
			}
		}
		catch (Exception ex) {
			logger.error("Failed to mark order Ready to Ship for orderId={}: {}",
					order.getOrderId(), ex.getMessage(), ex);
		}
	}

	@Override
	public ShiprocketOrderEventResponseDTO processShiprocketOrderEvent(ShiprocketOrderEvent event) {
		if (event == null || event.getShipmentId() == null) {
			logger.warn("processShiprocketOrderEvent: null or incomplete event received");
			return ShiprocketOrderEventResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("null or incomplete event received")
				.failedStep("VALIDATION")
				.build();
		}
		logger.info("Processing ShiprocketOrderEvent for shipmentId={}, orderId={}", event.getShipmentId(),
				event.getOrderId());

		try {
			ShiprocketEventContext ctx = new ShiprocketEventContext();

			ShiprocketOrderEventResponseDTO loadFailure = loadShipmentAndOrder(event, ctx);
			if (loadFailure != null) {
				return loadFailure;
			}

			Map<String, Object> shiprocketOrderRequest = buildBaseShiprocketOrderRequest(event, ctx);

			ShiprocketOrderEventResponseDTO cartonFailure = selectCartonForShipment(event, ctx);
			if (cartonFailure != null) {
				return cartonFailure;
			}
			finalizeShiprocketOrderRequest(ctx, shiprocketOrderRequest);

			// Step 1: Create Order on Shiprocket
			ShiprocketOrderEventResponseDTO createOrderFailure = executeCreateOrderStep(event, ctx,
					shiprocketOrderRequest);
			if (createOrderFailure != null) {
				return createOrderFailure;
			}

			// Step 1.5: Find Top-N Best Courier Services via Serviceability API
			executeFindBestCourierStep(event, ctx);

			// Step 1.6: bail out to manual processing if no eligible courier was found
			ShiprocketOrderEventResponseDTO noCourierFailure = handleNoCourierFound(event, ctx);
			if (noCourierFailure != null) {
				return noCourierFailure;
			}

			// Step 2: Generate AWB — try best couriers in order
			ShiprocketOrderEventResponseDTO awbFailure = executeGenerateAwbStep(event, ctx);
			if (awbFailure != null) {
				return awbFailure;
			}
			// Step 3: Request Pickup
			executeRequestPickupStep(event, ctx);

			// Step 4: Generate Label
			executeGenerateLabelStep(event, ctx);

			// Step 5: Track Shipment
			executeTrackShipmentStep(event, ctx);

			return finalizeOrderStatusAndBuildResponse(event, ctx);
		}
		catch (Exception e) {
			// Outer catch-all: save a generic FAILED record so nothing is silently lost
			saveStepLog(event, "PROCESS_EVENT", "FAILED", "Unexpected error: " + e.getMessage());
			logger.error("Error processing ShiprocketOrderEvent for shipmentId={}: {}", event.getShipmentId(),
					e.getMessage(), e);
			return ShiprocketOrderEventResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("Unexpected error: " + e.getMessage())
				.shipmentId(event.getShipmentId())
				.orderId(event.getOrderId())
				.warehouseId(event.getWarehouseId())
				.failedStep("PROCESS_EVENT")
				.build();
		}
	}

	/**
	 * Mutable holder used internally by {@link #processShiprocketOrderEvent} to
	 * thread shared state through the per-step helper methods below without
	 * having to pass a long list of individual parameters around. All fields are
	 * package-private for brevity since this is a private implementation detail.
	 */
	private static class ShiprocketEventContext {
		ShippingEO shippingEO;
		OrderEO order;
		OrderAddressEO orderAddress;
		String previousOrderStatusForEmail;

		boolean orderStatusAlreadySuccess;
		boolean awbStatusAlreadySuccess;
		boolean pickupStatusAlreadySuccess;
		boolean labelStatusAlreadySuccess;
		boolean trackStatusAlreadySuccess;

		List<OrderItemEO> itemsForWarehouse;
		String shipmentWarehouseName;
		String shipmentChannelId;

		List<Map<String, Object>> orderItemsList;
		double itemsWeight;

		CartonEO selectedCarton;

		Integer shipOrderId;
		Integer shipmentId;

		List<Integer> bestCourierIds = new ArrayList<>();
		Map<Integer, Double> courierRateMap = new HashMap<>();

		String awbCode;
		String generatedLabelUrl;
	}

	/**
	 * Loads the ShippingEO and OrderEO referenced by the event, along with the
	 * per-step idempotency flags, order items, order address and warehouse
	 * name/channel id. Returns a terminal failure response if either entity is
	 * missing; otherwise returns {@code null} and leaves {@code ctx} populated so
	 * processing can continue.
	 */
	private ShiprocketOrderEventResponseDTO loadShipmentAndOrder(ShiprocketOrderEvent event,
			ShiprocketEventContext ctx) {
		ShippingEO shippingEO = shippingRepository.findById(event.getShipmentId()).orElse(null);
		if (shippingEO == null) {
			saveStepLog(event, "CREATE_ORDER", "FAILED",
					"ShippingEO not found for shipmentId=" + event.getShipmentId());
			logger.error("processShiprocketOrderEvent: ShippingEO not found for shipmentId={}",
					event.getShipmentId());
			return ShiprocketOrderEventResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("ShippingEO not found for shipmentId=" + event.getShipmentId())
				.shipmentId(event.getShipmentId())
				.orderId(event.getOrderId())
				.warehouseId(event.getWarehouseId())
				.failedStep("CREATE_ORDER")
				.build();
		}
		ctx.shippingEO = shippingEO;

		OrderEO order = orderRepository.findById(event.getOrderId()).orElse(null);
		// Capture the order status as it was before this event's processing so we
		// can detect (after all 5 steps have run) whether it actually changed and
		// only then send a single "Order Status Update" email.
		ctx.previousOrderStatusForEmail = order != null ? order.getOrderStatus() : null;
		if (order == null) {
			saveStepLog(event, "CREATE_ORDER", "FAILED", "OrderEO not found for orderId=" + event.getOrderId());
			logger.error("processShiprocketOrderEvent: OrderEO not found for orderId={}", event.getOrderId());
			return ShiprocketOrderEventResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("OrderEO not found for orderId=" + event.getOrderId())
				.shipmentId(event.getShipmentId())
				.orderId(event.getOrderId())
				.warehouseId(event.getWarehouseId())
				.shipmentStatus(shippingEO.getShipmentStatus())
				.failedStep("CREATE_ORDER")
				.build();
		}
		ctx.order = order;

		// ── Per-step idempotency guards ────────────────────────────────────
		// Validate each of the five Shiprocket processing step statuses already
		// persisted on ShippingEO. If a step previously completed successfully,
		// skip re-executing it; otherwise (missing/failed) execute it.
		ctx.orderStatusAlreadySuccess = Constants.SUCCESS_STATUS.equals(shippingEO.getShiprocketOrderStatus());
		ctx.awbStatusAlreadySuccess = Constants.SUCCESS_STATUS.equals(shippingEO.getGenerateAwbStatus());
		ctx.pickupStatusAlreadySuccess = Constants.SUCCESS_STATUS.equals(shippingEO.getRequestPickupStatus());
		ctx.labelStatusAlreadySuccess = Constants.SUCCESS_STATUS.equals(shippingEO.getGenerateLabelStatus());
		ctx.trackStatusAlreadySuccess = Constants.SUCCESS_STATUS.equals(shippingEO.getTrackShipmentStatus());

		// Fetch shipment items for this shipment
		List<ShipmentItemEO> shipmentItems = shippingItemRepository.findByShipment(shippingEO);
		List<OrderItemEO> itemsForWarehouse = new ArrayList<>();
		for (ShipmentItemEO si : shipmentItems) {
			if (si.getOrderItem() != null) {
				itemsForWarehouse.add(si.getOrderItem());
			}
		}
		ctx.itemsForWarehouse = itemsForWarehouse;

		ctx.orderAddress = orderAddressRepository.findByOrder(order).orElse(null);

		// Resolve warehouse name and channel ID from the inventory-associated
		// warehouse for this shipment
		if (event.getWarehouseId() != null) {
			WarehouseEO shipmentWarehouse = warehouseRepository.findById(event.getWarehouseId()).orElse(null);
			if (shipmentWarehouse != null) {
				ctx.shipmentWarehouseName = shipmentWarehouse.getWarehouseName();
				ctx.shipmentChannelId = shipmentWarehouse.getChannelId();
			}
		}
		return null;
	}

	/**
	 * Builds the Shiprocket create-order request payload from the order, order
	 * address and order items — everything except the carton-dependent fields,
	 * which are added later by {@link #finalizeShiprocketOrderRequest} once a
	 * carton has been selected. Also stores the built order-items list and total
	 * item weight on {@code ctx} for later use.
	 */
	private Map<String, Object> buildBaseShiprocketOrderRequest(ShiprocketOrderEvent event,
			ShiprocketEventContext ctx) {
		OrderEO order = ctx.order;
		OrderAddressEO orderAddress = ctx.orderAddress;

		Map<String, Object> shiprocketOrderRequest = new HashMap<>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy");
		String orderDate = LocalDate.now().format(formatter);
		shiprocketOrderRequest.put("order_id", order.getOrderNumber());
		shiprocketOrderRequest.put("order_date", orderDate);
		shiprocketOrderRequest.put("pickup_location",
				ctx.shipmentWarehouseName != null ? ctx.shipmentWarehouseName : "warehouse");
		shiprocketOrderRequest.put("channel_id",
				ctx.shipmentChannelId != null ? ctx.shipmentChannelId : Constants.DEFAULT_SHIPMENT_CHANNEL_ID);
		String customername = order.getCustomer() != null ? order.getCustomer().getFirstName()
				: orderAddress != null && orderAddress.getRecipientName() != null ? orderAddress.getRecipientName()
						: "Customer";
		String customermobileno = order.getCustomer() != null ? order.getCustomer().getMobileNumber() : "Customer";
		if (customername == null || customername.isEmpty()) {
			customername = customermobileno;
		}
		shiprocketOrderRequest.put("billing_customer_name", customername);
		shiprocketOrderRequest.put("billing_address", orderAddress != null && orderAddress.getAddressLine1() != null
				? orderAddress.getAddressLine1() : "");
		shiprocketOrderRequest.put("billing_city",
				orderAddress != null && orderAddress.getCity() != null ? orderAddress.getCity() : "");
		shiprocketOrderRequest.put("billing_pincode",
				orderAddress != null && orderAddress.getPostalCode() != null ? orderAddress.getPostalCode() : "");
		shiprocketOrderRequest.put("billing_state",
				orderAddress != null && orderAddress.getState() != null ? orderAddress.getState() : "");
		shiprocketOrderRequest.put("billing_country",
				orderAddress != null && orderAddress.getCountry() != null && !orderAddress.getCountry().isEmpty()
						? orderAddress.getCountry() : "India");
		shiprocketOrderRequest.put("billing_email",
				order.getCustomer() != null ? order.getCustomer().getEmail() : "");
		shiprocketOrderRequest.put("billing_phone",
				order.getCustomer() != null ? order.getCustomer().getMobileNumber() : "");
		shiprocketOrderRequest.put("shipping_is_billing", true);
		shiprocketOrderRequest.put("billing_last_name", "");

		List<Map<String, Object>> orderItemsList = new ArrayList<>();
		double weightTemp = 0.0;
		for (OrderItemEO item : ctx.itemsForWarehouse) {
			Map<String, Object> itemMap = new HashMap<>();
			ProductVariantEO variant = item.getProductVar();
			if (variant != null) {
				int qty = item.getQuantity() != null ? item.getQuantity() : 1;
				// variant.getWeight() is the per-unit weight in GRAMS (see
				// product-api-docs.md / ProductVariantEO); multiply by quantity so
				// items ordered more than once are weighed correctly. The running
				// total is converted from grams to kilograms below (ctx.itemsWeight).
				weightTemp += variant.getWeight() * qty;
			}

			itemMap.put("name", item.getProductVar() != null && item.getProductVar().getProduct() != null
					? item.getProductVar().getProduct().getName() : "");
			itemMap.put("sku", item.getProductVar() != null ? item.getProductVar().getSkuCode() : "");
			itemMap.put("units", item.getQuantity());
			itemMap.put("selling_price", item.getUnitPrice());
			// Calculate discount as (mrp - sellingPrice) if both are present
			double discount = 0.0;
			if (variant != null && variant.getMrp() != null && variant.getSellingPrice() != null) {
				discount = variant.getMrp().doubleValue() - variant.getSellingPrice().doubleValue();
			}
			itemMap.put("discount", discount);
			itemMap.put("tax", 0);
			itemMap.put("hsn", "");
			orderItemsList.add(itemMap);
		}
		ctx.orderItemsList = orderItemsList;
		ctx.itemsWeight = weightTemp/1000.0;
		return shiprocketOrderRequest;
	}

	/**
	 * Resolves the carton to use for this shipment (caller-supplied cartonNo,
	 * caller-supplied requestCreateCartonDTO, or automatic selection) and stores
	 * it on {@code ctx.selectedCarton}. On failure the order is marked Ready to
	 * Ship and a terminal failure response is returned.
	 */
	private ShiprocketOrderEventResponseDTO selectCartonForShipment(ShiprocketOrderEvent event,
			ShiprocketEventContext ctx) {
		CartonEO selectedCarton;
		String requestedCartonNo = event.getCartonNo();
		com.user.dto.RequestCreateCartonDTO requestedCreateCartonDTO = event.getRequestCreateCartonDTO();
		boolean cartonNoProvided = requestedCartonNo != null && !requestedCartonNo.trim().isEmpty();
		boolean createCartonDTOProvided = requestedCreateCartonDTO != null;
		try {
			if (!cartonNoProvided && !createCartonDTOProvided) {
				// Neither cartonNo nor requestCreateCartonDTO supplied — fall back to
				// the existing automatic carton selection logic.
				selectedCarton = cartonSelectionService.selectCarton(ctx.itemsForWarehouse);
			}
			else if (cartonNoProvided) {
				// Caller supplied a specific cartonNo — try to fetch that carton by id.
				Long cartonId = null;
				try {
					cartonId = Long.parseLong(requestedCartonNo.trim());
				}
				catch (NumberFormatException nfe) {
					logger.warn(
							"Step SELECT_CARTON: cartonNo '{}' is not a valid numeric carton id for shipmentId={}",
							requestedCartonNo, event.getShipmentId());
				}
				selectedCarton = cartonId != null ? cartonRepository.findById(cartonId).orElse(null) : null;
				if (selectedCarton == null) {
					if (createCartonDTOProvided) {
						// No carton found for the supplied cartonNo — create a new one
						// using the supplied requestCreateCartonDTO and use it.
						selectedCarton = createCartonFromRequest(requestedCreateCartonDTO);
						logger.info(
								"Step SELECT_CARTON: cartonNo='{}' not found; created new cartonId={} for shipmentId={}",
								requestedCartonNo, selectedCarton.getId(), event.getShipmentId());
					}
					else {
						throw new IllegalStateException("No carton found for cartonNo=" + requestedCartonNo
								+ " and no requestCreateCartonDTO provided to create a new one");
					}
				}
				else {
					logger.info("Step SELECT_CARTON: using caller-supplied cartonNo={} (cartonId={}) for shipmentId={}",
							requestedCartonNo, selectedCarton.getId(), event.getShipmentId());
				}
			}
			else {
				// cartonNo not provided but requestCreateCartonDTO is — create a new
				// carton from the supplied details and use it.
				selectedCarton = createCartonFromRequest(requestedCreateCartonDTO);
				logger.info("Step SELECT_CARTON: created new cartonId={} from requestCreateCartonDTO for shipmentId={}",
						selectedCarton.getId(), event.getShipmentId());
			}
		}
		catch (Exception cartonEx) {
			// Automatic carton creation is disabled — CartonSelectionService already
			// notified admins. Log a specific, actionable step so this doesn't get
			// buried under a generic "PROCESS_EVENT FAILED" audit entry.
			saveStepLog(event, "SELECT_CARTON", "FAILED", cartonEx.getMessage());
			logger.error("Step SELECT_CARTON FAILED for shipmentId={}: {}", event.getShipmentId(),
					cartonEx.getMessage(), cartonEx);
			// Carton unavailable — cannot proceed to Shiprocket. Keep the order
			// (and whatever shipping/shipment_item/tracking-history/order-log
			// records already exist) but mark it Ready to Ship so it can be
			// picked up for manual processing instead of leaving it stuck.
			markOrderReadyToShip(ctx.order, ctx.shippingEO, "Carton not available: " + cartonEx.getMessage());
			return ShiprocketOrderEventResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("Carton not available: " + cartonEx.getMessage())
				.shipmentId(event.getShipmentId())
				.orderId(event.getOrderId())
				.warehouseId(event.getWarehouseId())
				.shipmentStatus(ctx.shippingEO.getShipmentStatus())
				.failedStep("SELECT_CARTON")
				.build();
		}
		ctx.selectedCarton = selectedCarton;
		return null;
	}

	/**
	 * Adds the carton-dependent fields (order_items, payment_method, sub_total,
	 * dimensions, weight) to the Shiprocket create-order request now that a
	 * carton has been selected, and persists the resolved dimensions/weight onto
	 * the ShippingEO.
	 */
	private void finalizeShiprocketOrderRequest(ShiprocketEventContext ctx,
			Map<String, Object> shiprocketOrderRequest) {
		OrderEO order = ctx.order;
		CartonEO selectedCarton = ctx.selectedCarton;
		ShippingEO shippingEO = ctx.shippingEO;

		shiprocketOrderRequest.put("order_items", ctx.orderItemsList);
		shiprocketOrderRequest.put("payment_method",
				order.getPaymentStatus() != null && order.getPaymentStatus().equalsIgnoreCase("PAID") ? "Prepaid"
						: "COD");
		shiprocketOrderRequest.put("sub_total", order.getTotalAmount());
		shiprocketOrderRequest.put("length", selectedCarton.getLength());
		shiprocketOrderRequest.put("breadth", selectedCarton.getBreadth());
		shiprocketOrderRequest.put("height", selectedCarton.getHeight());
		// NOTE: variant weight (see ctx.itemsWeight in buildBaseShiprocketOrderRequest)
		// and carton empty/max weight (see CartonEO / shipping-api-docs.md — carton
		// weights are captured/stored in GRAMS) are both entered in grams, but
		// Shiprocket's "weight" field expects kilograms. ctx.itemsWeight is already
		// converted to kg, so the carton's empty weight must also be converted here
		// (grams / 1000) before being added — otherwise the total weight sent to
		// Shiprocket ends up ~1000x too large (e.g. a 400g box becomes "400 kg").
		double emptyWeightKg = selectedCarton.getEmptyWeight() / 1000.0;
		double totalWeightKg = emptyWeightKg + ctx.itemsWeight;
		// Shiprocket requires a minimum shipment weight of 1.1 kg; enforce that
		// floor here so lightweight orders aren't sent with an under-billed weight.
		final double MIN_SHIPMENT_WEIGHT_KG = 1.1;
		if (totalWeightKg < MIN_SHIPMENT_WEIGHT_KG) {
			totalWeightKg = MIN_SHIPMENT_WEIGHT_KG;
		}
		shiprocketOrderRequest.put("weight", totalWeightKg);
		shippingEO.setLength(selectedCarton.getLength());
		shippingEO.setBreadth(selectedCarton.getBreadth());
		shippingEO.setHeight(selectedCarton.getHeight());
		shippingEO.setWeight(totalWeightKg);
		// Persist a direct entity reference to the carton used for this shipment.
		shippingEO.setCarton(selectedCarton);
	}

	/**
	 * Step 1: Create Order on Shiprocket (or reuse an already-created order if
	 * this call is a retrigger). Populates {@code ctx.shipOrderId} /
	 * {@code ctx.shipmentId} on success. Returns a terminal failure response if
	 * the order could not be created.
	 */
	private ShiprocketOrderEventResponseDTO executeCreateOrderStep(ShiprocketOrderEvent event,
			ShiprocketEventContext ctx, Map<String, Object> shiprocketOrderRequest) {
		ShippingEO shippingEO = ctx.shippingEO;
		OrderEO order = ctx.order;
		Integer shipOrderId = null;
		Integer shipmentId = null;
		// ── Idempotency / retrigger guard ──────────────────────────────────
		// If this shipment already has a Shiprocket order (e.g. this call is a
		// manual admin retrigger after a downstream step failed), reuse the
		// existing order/shipment ids instead of calling createOrder again —
		// that would create a duplicate order on Shiprocket.
		if (ctx.orderStatusAlreadySuccess && shippingEO.getShipOrderId() != null
				&& shippingEO.getShipShipmentId() != null) {
			shipOrderId = shippingEO.getShipOrderId();
			shipmentId = shippingEO.getShipShipmentId();
			shippingRepository.save(shippingEO); // persist carton dims computed above
			saveStepLog(event, "CREATE_ORDER", "SKIPPED",
					"Shiprocket order already exists (order_id=" + shipOrderId + ", shipment_id=" + shipmentId
							+ "); reusing existing order instead of creating a duplicate");
			logger.info("Step CREATE_ORDER SKIPPED (already exists): reusing order_id={}, shipment_id={}",
					shipOrderId, shipmentId);
		}
		else
		try {
			Map response = shiprocketService.createOrder(shiprocketOrderRequest);
			shipOrderId = response != null ? (Integer) response.get("order_id") : null;
			if (response != null && shipOrderId != null) {
				shipmentId = (Integer) response.get("shipment_id");
				shippingEO.setShipOrderId(shipOrderId);
				shippingEO.setShipShipmentId(shipmentId);
				// Populate estimated_delivery_date from createOrder response
				Object estDelivery = response.get("estimated_delivery_date");
				if (estDelivery instanceof String && !((String) estDelivery).isEmpty()) {
					try {
						shippingEO.setEstimatedDeliveryDate(LocalDateTime.parse((String) estDelivery,
								DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
					}
					catch (Exception ignored) {
						try {
							shippingEO.setEstimatedDeliveryDate(
									java.time.LocalDate.parse((String) estDelivery).atStartOfDay());
						}
						catch (Exception ex2) {
							logger.warn("Could not parse estimated_delivery_date '{}': {}", estDelivery,
									ex2.getMessage());
						}
					}
				}
				shippingRepository.save(shippingEO);
				// ── Separate log record for CREATE_ORDER success ──
				shiprocketOrderLogRepository.save(ShiprocketOrderLogEO.builder()
					.shipmentId(event.getShipmentId())
					.orderId(event.getOrderId())
					.warehouseId(event.getWarehouseId())
					.step("CREATE_ORDER")
					.status(Constants.SUCCESS_STATUS)
					.shiprocketOrderId(shipOrderId)
					.shiprocketShipmentId(shipmentId)
					.build());
				logger.info("Step CREATE_ORDER SUCCESS: order_id={}, shipment_id={}", shipOrderId, shipmentId);
				// Order successfully created and a non-null shipOrderId was returned —
				// mark shiprocket_order_status SUCCESS on ShippingEO and record the
				// corresponding status-history row.
				recordShiprocketOrderStatus(shippingEO, Constants.SUCCESS_STATUS, null);
			}
			else {
				// Either Shiprocket returned no response at all, or it returned a
				// response without an order_id (shipOrderId == null) — both are
				// treated as a failed order creation.
				String failureReason = response == null ? "Shiprocket createOrder returned null response"
						: "Shiprocket createOrder response did not contain an order_id";
				saveStepLog(event, "CREATE_ORDER", "FAILED", failureReason);
				logger.error("Step CREATE_ORDER FAILED: {}", failureReason);
				// Mark shiprocket_order_status FAILED on ShippingEO and record the
				// corresponding status-history row.
				recordShiprocketOrderStatus(shippingEO, "FAILED", failureReason);
				markOrderReadyToShip(order, shippingEO, "Shiprocket order was not generated (" + failureReason + ")");
				return ShiprocketOrderEventResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage("Shiprocket order was not generated (" + failureReason + ")")
					.shipmentId(event.getShipmentId())
					.orderId(event.getOrderId())
					.warehouseId(event.getWarehouseId())
					.shipmentStatus(shippingEO.getShipmentStatus())
					.failedStep("CREATE_ORDER")
					.build();
			}
		}
		catch (Exception ex) {
			// Exception while calling createOrder — mark shiprocket_order_status
			// FAILED on ShippingEO and record the corresponding status-history row.
			saveStepLog(event, "CREATE_ORDER", "FAILED", ex.getMessage());
			logger.error("Step CREATE_ORDER FAILED for shipmentId={}: {}", event.getShipmentId(), ex.getMessage(),
					ex);
			recordShiprocketOrderStatus(shippingEO, "FAILED", ex.getMessage());
			markOrderReadyToShip(order, shippingEO, "Shiprocket order was not generated: " + ex.getMessage());
			return ShiprocketOrderEventResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("Shiprocket order was not generated: " + ex.getMessage())
				.shipmentId(event.getShipmentId())
				.orderId(event.getOrderId())
				.warehouseId(event.getWarehouseId())
				.shipmentStatus(shippingEO.getShipmentStatus())
				.failedStep("CREATE_ORDER")
				.build();
		}
		ctx.shipOrderId = shipOrderId;
		ctx.shipmentId = shipmentId;
		return null;
	}

	/**
	 * Step 1.5: Find Top-N Best Courier Services via Serviceability API.
	 * Populates {@code ctx.bestCourierIds} and {@code ctx.courierRateMap}. Never
	 * fails the overall event — any error here just falls back to Shiprocket
	 * auto-assign.
	 */
	private void executeFindBestCourierStep(ShiprocketOrderEvent event, ShiprocketEventContext ctx) {
		ShippingEO shippingEO = ctx.shippingEO;
		OrderEO order = ctx.order;
		OrderAddressEO orderAddress = ctx.orderAddress;
		Integer shipOrderId = ctx.shipOrderId;
		Integer shipmentId = ctx.shipmentId;
		List<Integer> bestCourierIds = ctx.bestCourierIds;
		Map<Integer, Double> courierRateMap = ctx.courierRateMap;
		List<Map<String, Object>> courierDetailsList = new ArrayList<>();
		try {
			if (event.getBestCourierId() != null
					&& !Constants.BLOCKLISTED_COURIER_COMPANY_IDS.contains(event.getBestCourierId())) {
				// Caller already specified a preferred courier — skip the serviceability
				// lookup entirely and use it directly.
				bestCourierIds.add(event.getBestCourierId());
				saveStepLog(event, "FIND_BEST_COURIER", "SKIPPED",
						"Using caller-supplied bestCourierId=" + event.getBestCourierId());
				logger.info("Step FIND_BEST_COURIER: using caller-supplied bestCourierId={} for shipmentId={}",
						event.getBestCourierId(), shipmentId);
			}
			else if (event.getBestCourierId() != null) {
				logger.warn(
						"Step FIND_BEST_COURIER: caller-supplied bestCourierId={} is blocklisted, falling back to serviceability lookup",
						event.getBestCourierId());
			}

			String deliveryPostcode = orderAddress != null ? orderAddress.getPostalCode() : null;
			if (!bestCourierIds.isEmpty()) {
				// Already resolved via caller-supplied bestCourierId above.
			}
			else if (deliveryPostcode != null && !deliveryPostcode.isEmpty() && shipmentId != null) {
				Double weighttemp1 = shippingEO.getWeight() != null ? shippingEO.getWeight() : 1.1;
				if (weighttemp1 <= 1.1) {
					weighttemp1 = 1.1;
				}

				ServiceabilityRequestDTO serviceabilityReq = ServiceabilityRequestDTO.builder()
					.orderId(shipOrderId)
					.pickupPostcode(Integer.parseInt(getWarehousePostalCode(ctx.shipmentWarehouseName)))
					.deliveryPostcode(Integer.parseInt(deliveryPostcode.trim()))
					.cod(order.getPaymentStatus() != null && order.getPaymentStatus().equalsIgnoreCase("PAID") ? 0
							: 1)
					.weight(String.valueOf(weighttemp1))
					.length(shippingEO.getLength() != null && shippingEO.getLength() > 0
							? shippingEO.getLength().intValue() : null)
					.breadth(shippingEO.getBreadth() != null && shippingEO.getBreadth() > 0
							? shippingEO.getBreadth().intValue() : null)
					.height(shippingEO.getHeight() != null && shippingEO.getHeight() > 0
							? shippingEO.getHeight().intValue() : null)
					.build();

				List<Integer> allBestCouriers = shiprocketService.getBestCourierServices(serviceabilityReq,
						Constants.MAX_BEST_COURIER_COUNT, courierRateMap, courierDetailsList);

				// Filter out blocklisted courier IDs
				for (Integer cId : allBestCouriers) {
					if (cId != null && !Constants.BLOCKLISTED_COURIER_COMPANY_IDS.contains(cId)) {
						bestCourierIds.add(cId);
					}
					else {
						logger.info("Step FIND_BEST_COURIER: skipping blocklisted courierCompanyId={}", cId);
					}
				}

				// ── Persist all candidate couriers to courier_selection_log ──
				String orderNumber = order.getOrderNumber();
				for (Map<String, Object> detail : courierDetailsList) {
					try {
						Integer cId = (Integer) detail.get("courierCompanyId");
						String cName = (String) detail.get("courierName");
						Double cRate = (Double) detail.get("rate");
						Double cDays = (Double) detail.get("estimatedDeliveryDays");
						Integer cRank = (Integer) detail.get("rank");
						CourierSelectionLogEO logEntry = CourierSelectionLogEO.builder()
							.orderId(event.getOrderId())
							.orderNumber(orderNumber)
							.shipmentId(event.getShipmentId())
							.shipShipmentId(shipmentId)
							.courierCompanyId(cId)
							.courierName(cName)
							.rate(cRate != null ? new java.math.BigDecimal(cRate) : null)
							.estimatedDeliveryDays(cDays)
							.rank(cRank)
							.isSelected(false)
							.build();
						courierSelectionLogRepository.save(logEntry);
					}
					catch (Exception saveEx) {
						logger.warn("Could not save courier_selection_log entry: {}", saveEx.getMessage());
					}
				}

				// ── Separate log record for FIND_BEST_COURIER ──
				String fcStatus = bestCourierIds.isEmpty() ? "NOT_FOUND" : Constants.SUCCESS_STATUS;
				String fcError = bestCourierIds.isEmpty()
						? "No eligible (non-blocked) couriers found; AWB will use Shiprocket auto-assign" : null;
				saveStepLog(event, "FIND_BEST_COURIER", fcStatus, fcError);
				logger.info("Step FIND_BEST_COURIER: {} eligible couriers for shipmentId={}: {}",
						bestCourierIds.size(), shipmentId, bestCourierIds);
			}
			else {
				saveStepLog(event, "FIND_BEST_COURIER", "SKIPPED",
						"Delivery postcode unavailable; serviceability check skipped");
				logger.warn("Step FIND_BEST_COURIER skipped: delivery postcode unavailable for shipmentId={}",
						event.getShipmentId());
			}
		}
		catch (Exception ex) {
			saveStepLog(event, "FIND_BEST_COURIER", "FAILED",
					"Will proceed with auto-assign. Error: " + ex.getMessage());
			logger.warn("Step FIND_BEST_COURIER FAILED for shipmentId={}, will proceed with auto-assign: {}",
					event.getShipmentId(), ex.getMessage());
		}
	}

	/**
	 * Step 1.6: If no eligible courier was found via getBestCourierServices, the
	 * Shiprocket order is already created (Step 1) — stop here and flag the
	 * shipment for manual processing instead of attempting AWB/pickup
	 * auto-assign, which is unreliable when Shiprocket itself found no
	 * serviceable courier for this route/weight/COD combination. Returns
	 * {@code null} if a courier was found (i.e. processing should continue).
	 */
	private ShiprocketOrderEventResponseDTO handleNoCourierFound(ShiprocketOrderEvent event,
			ShiprocketEventContext ctx) {
		if (!ctx.bestCourierIds.isEmpty()) {
			return null;
		}
		ShippingEO shippingEO = ctx.shippingEO;
		OrderEO order = ctx.order;
		Integer shipOrderId = ctx.shipOrderId;

		shippingEO.setShipmentStatus(Constants.SHIPMENT_STATUS_MANUAL_PROCESSING_REQUIRED);
		shippingRepository.save(shippingEO);

		recordShiprocketOrderStatus(shippingEO, Constants.FAILURE_STATUS,
				"No Best Courier for this Order");

		ShipmentTrackingHistoryEO manualTrackingEntry = new ShipmentTrackingHistoryEO();
		manualTrackingEntry.setShipment(shippingEO);
		manualTrackingEntry.setStatus(Constants.SHIPMENT_STATUS_MANUAL_PROCESSING_REQUIRED);
		manualTrackingEntry.setRemarks(
				"No eligible courier found for this route/weight/COD combination. Shiprocket order created (order_id="
						+ shipOrderId + "); shipment requires manual courier assignment.");
		if (!shipmentTrackingHistoryRepository.existsByShipmentAndStatusIgnoreCase(shippingEO,
				Constants.SHIPMENT_STATUS_MANUAL_PROCESSING_REQUIRED)) {
			shipmentTrackingHistoryRepository.save(manualTrackingEntry);
		}

		saveStepLog(event, "GENERATE_AWB", "SKIPPED",
				"No eligible courier found via getBestCourierServices; Shiprocket order was created successfully, shipment will be processed manually");
		logger.warn(
				"Step GENERATE_AWB SKIPPED for shipmentId={}: no eligible courier found. Shiprocket order_id={} was created; shipment flagged for manual processing.",
				event.getShipmentId(), shipOrderId);
		recordGenerateAwbStatus(shippingEO, "SKIPPED", "No eligible courier found via getBestCourierServices");

		// Best-effort admin push notification — never breaks order processing.
		try {
			pushNotificationService.notifyAdminsNoCourierFound(shippingEO, shipOrderId);
		}
		catch (Exception pushEx) {
			logger.error("Failed to send no-courier-found push notification for shipmentId={}: {}", event.getShipmentId(),
					pushEx.getMessage(), pushEx);
		}

		// Courier service not available — Shiprocket order exists but AWB /
		// label cannot be generated without a courier. Mark Ready to Ship.
		markOrderReadyToShip(order, shippingEO, "No courier service available for this shipment");
		return ShiprocketOrderEventResponseDTO.builder()
			.responseStatus(Constants.FAILURE_STATUS)
			.responseMessage("No courier service available for this shipment")
			.shipmentId(event.getShipmentId())
			.orderId(event.getOrderId())
			.warehouseId(event.getWarehouseId())
			.shipOrderId(shipOrderId)
			.shipShipmentId(ctx.shipmentId)
			.shipmentStatus(shippingEO.getShipmentStatus())
			.failedStep("GENERATE_AWB")
			.build();
	}

	/**
	 * Step 2: Generate AWB — try best couriers in order until one succeeds.
	 * Populates {@code ctx.awbCode} on success. Returns a terminal failure
	 * response only when {@code shipmentId} is missing (AWB cannot even be
	 * attempted); any per-courier failures are recorded but do not stop
	 * processing.
	 */
	private ShiprocketOrderEventResponseDTO executeGenerateAwbStep(ShiprocketOrderEvent event,
			ShiprocketEventContext ctx) {
		ShippingEO shippingEO = ctx.shippingEO;
		OrderEO order = ctx.order;
		Integer shipOrderId = ctx.shipOrderId;
		Integer shipmentId = ctx.shipmentId;
		List<Integer> bestCourierIds = ctx.bestCourierIds;
		Map<Integer, Double> courierRateMap = ctx.courierRateMap;

		String awbCode = null;
		if (ctx.awbStatusAlreadySuccess && shippingEO.getAwb() != null) {
			awbCode = shippingEO.getAwb();
			saveStepLog(event, "GENERATE_AWB", "SKIPPED", "AWB already generated previously: " + awbCode);
			logger.info("Step GENERATE_AWB SKIPPED (already success) for shipmentId={}: awb={}",
					event.getShipmentId(), awbCode);
		}
		else
		try {
			if (shipmentId == null) {
				saveStepLog(event, "GENERATE_AWB", "FAILED",
						"Cannot generate AWB without shipment_id from Shiprocket");
				logger.error("Step GENERATE_AWB FAILED: shipment_id is null, cannot proceed");
				recordGenerateAwbStatus(shippingEO, "FAILED", "Cannot generate AWB without shipment_id from Shiprocket");
				markOrderReadyToShip(order, shippingEO, "AWB not generated: shipment_id missing from Shiprocket");
				return ShiprocketOrderEventResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage("AWB not generated: shipment_id missing from Shiprocket")
					.shipmentId(event.getShipmentId())
					.orderId(event.getOrderId())
					.warehouseId(event.getWarehouseId())
					.shipOrderId(shipOrderId)
					.shipShipmentId(shipmentId)
					.shipmentStatus(shippingEO.getShipmentStatus())
					.failedStep("GENERATE_AWB")
					.build();
			}

			// Only ranked/eligible couriers are attempted now — bestCourierIds is
			// guaranteed non-empty at this point.
			List<Integer> couriersToTry = bestCourierIds;

			Integer courierCompanyId = null;
			String courierName = null;
			java.math.BigDecimal shippingPrice = null;

			for (int ci = 0; ci < couriersToTry.size() && awbCode == null; ci++) {
				Integer tryCourierId = couriersToTry.get(ci);
				try {
					Map awbResp = shiprocketService.generateAWB(shipmentId, tryCourierId);
					String extracted = extractAwbCode(awbResp);
					if (extracted != null) {
						awbCode = extracted;

						// Extract courier details
						Object responseObj = awbResp != null ? awbResp.get("response") : null;
						Map dataDetail = null;
						if (responseObj instanceof Map) {
							Object dataObj = ((Map) responseObj).get("data");
							if (dataObj instanceof Map)
								dataDetail = (Map) dataObj;
							else
								dataDetail = (Map) responseObj;
						}
						if (dataDetail != null) {
							Object ccId = dataDetail.get("courier_company_id");
							if (ccId instanceof Number)
								courierCompanyId = ((Number) ccId).intValue();
							Object cn = dataDetail.get("courier_name");
							if (cn instanceof String)
								courierName = (String) cn;
							// freight_charge / rate → shipping price
							Object freightObj = dataDetail.get("freight_charge");
							if (freightObj == null)
								freightObj = dataDetail.get("rate");
							if (freightObj instanceof Number) {
								shippingPrice = new java.math.BigDecimal(((Number) freightObj).doubleValue());
							}
							// etd
							Object etdObj = dataDetail.get("etd");
							if (etdObj instanceof String && !((String) etdObj).isEmpty()) {
								final String etdStr = (String) etdObj;
								try {
									shippingEO.setExpectedDeliveryDate(LocalDateTime.parse(etdStr,
											DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
								}
								catch (Exception ignored) {
									try {
										shippingEO.setExpectedDeliveryDate(
												java.time.LocalDate.parse(etdStr).atStartOfDay());
									}
									catch (Exception ex2) {
										logger.warn("Could not parse etd '{}': {}", etdStr, ex2.getMessage());
									}
								}
							}
						}
						logger.info("Step GENERATE_AWB SUCCESS on attempt #{} with courierCompanyId={}: awb={}",
								ci + 1, tryCourierId, awbCode);
					}
					else {
						// Log per-attempt failure as a separate record
						String awbErr = extractAwbGenerateError(awbResp);
						saveStepLog(event, "GENERATE_AWB", "ATTEMPT_FAILED", "courierCompanyId=" + tryCourierId
								+ " attempt #" + (ci + 1) + (awbErr != null ? ". Error: " + awbErr : ""));
						logger.warn("Step GENERATE_AWB: courier {} (attempt #{}) returned no AWB code. error={}",
								tryCourierId, ci + 1, awbErr);
					}
				}
				catch (Exception ex) {
					saveStepLog(event, "GENERATE_AWB", "ATTEMPT_FAILED", "courierCompanyId=" + tryCourierId
							+ " attempt #" + (ci + 1) + ". Exception: " + ex.getMessage());
					logger.warn("Step GENERATE_AWB: exception with courierCompanyId={} (attempt #{}): {}",
							tryCourierId, ci + 1, ex.getMessage());
				}
			}

			// ── Separate final log record for GENERATE_AWB outcome ──
			if (awbCode == null) {
				String awbFailureReason = "AWB code not received from Shiprocket after trying "
						+ couriersToTry.size() + " courier(s)";
				saveStepLog(event, "GENERATE_AWB", "FAILED", awbFailureReason);
				logger.error("Step GENERATE_AWB FAILED for shipmentId={} after trying {} couriers",
						event.getShipmentId(), couriersToTry.size());
				// Persist generateAwbStatus=FAILED on ShippingEO and record the
				// corresponding status-history row.
				recordGenerateAwbStatus(shippingEO, "FAILED", awbFailureReason);
			}
			else {
				shippingEO.setAwb(awbCode);
				if (courierCompanyId != null)
					shippingEO.setCourierCompanyId(courierCompanyId);
				if (courierName != null)
					shippingEO.setCourierName(courierName);
				// Set shipping price: prefer AWB response freight_charge; fall back
				// to serviceability rate map
				if (shippingPrice != null) {
					shippingEO.setShippingPrice(shippingPrice);
				}
				else if (courierCompanyId != null && courierRateMap.containsKey(courierCompanyId)) {
					shippingEO.setShippingPrice(new java.math.BigDecimal(courierRateMap.get(courierCompanyId)));
				}
				shippingRepository.save(shippingEO);

				// ── Mark the selected courier in courier_selection_log ──
				final Integer finalCourierCompanyId = courierCompanyId;
				final String finalAwbCode = awbCode;
				final java.math.BigDecimal finalShippingPriceForLog = shippingEO.getShippingPrice();
				if (finalCourierCompanyId != null) {
					try {
						courierSelectionLogRepository.findByShipmentIdOrderByRankAsc(event.getShipmentId())
							.stream()
							.filter(e -> finalCourierCompanyId.equals(e.getCourierCompanyId()))
							.findFirst()
							.ifPresent(entry -> {
								entry.setIsSelected(true);
								entry.setAwbCode(finalAwbCode);
								entry.setShippingPrice(finalShippingPriceForLog);
								courierSelectionLogRepository.save(entry);
							});
					}
					catch (Exception markEx) {
						logger.warn("Could not mark selected courier in courier_selection_log: {}",
								markEx.getMessage());
					}
				}

				ShiprocketOrderLogEO awbLog = ShiprocketOrderLogEO.builder()
					.shipmentId(event.getShipmentId())
					.orderId(event.getOrderId())
					.warehouseId(event.getWarehouseId())
					.step("GENERATE_AWB")
					.status(Constants.SUCCESS_STATUS)
					.awbCode(awbCode)
					.build();
				shiprocketOrderLogRepository.save(awbLog);
				recordGenerateAwbStatus(shippingEO, Constants.SUCCESS_STATUS, null);
			}
		}
		catch (Exception ex) {
			saveStepLog(event, "GENERATE_AWB", "FAILED", ex.getMessage());
			logger.error("Step GENERATE_AWB FAILED for shipmentId={}: {}", event.getShipmentId(), ex.getMessage(),
					ex);
			recordGenerateAwbStatus(shippingEO, "FAILED", ex.getMessage());
		}
		ctx.awbCode = awbCode;
		return null;
	}

	/**
	 * Step 3: Request Pickup — only attempted once CREATE_ORDER and GENERATE_AWB
	 * have both succeeded. Never returns a terminal failure; any error here is
	 * logged/recorded and processing continues to the remaining steps.
	 */
	private void executeRequestPickupStep(ShiprocketOrderEvent event, ShiprocketEventContext ctx) {
		ShippingEO shippingEO = ctx.shippingEO;
		Integer shipmentId = ctx.shipmentId;

		boolean pickupPrerequisitesMet = Constants.SUCCESS_STATUS.equals(shippingEO.getShiprocketOrderStatus())
				&& Constants.SUCCESS_STATUS.equals(shippingEO.getGenerateAwbStatus());
		if (ctx.pickupStatusAlreadySuccess) {
			saveStepLog(event, "REQUEST_PICKUP", "SKIPPED", "Pickup already requested successfully previously");
			logger.info("Step REQUEST_PICKUP SKIPPED (already success) for shipmentId={}", event.getShipmentId());
		}
		else if (!pickupPrerequisitesMet) {
			String pickupSkipReason = "Skipping REQUEST_PICKUP: prerequisite steps not successful (shiprocketOrderStatus="
					+ shippingEO.getShiprocketOrderStatus() + ", generateAwbStatus="
					+ shippingEO.getGenerateAwbStatus() + ")";
			saveStepLog(event, "REQUEST_PICKUP", "SKIPPED", pickupSkipReason);
			logger.warn("Step REQUEST_PICKUP SKIPPED for shipmentId={}: {}", event.getShipmentId(),
					pickupSkipReason);
			recordRequestPickupStatus(shippingEO, "SKIPPED", pickupSkipReason);
		}
		else
		try {
			Map pickupResponseMap = shiprocketService.requestPickup(shipmentId.toString());
			shippingEO.setShipmentStatus("PICKUP_SCHEDULED");

			// Check if Shiprocket indicated the pickup was already in queue
			boolean alreadyInQueue = pickupResponseMap != null
					&& Boolean.TRUE.equals(pickupResponseMap.get("already_in_pickup_queue"));

			// Persist pickup_id, pickup_scheduled_date, pickup_token from response
			// Shiprocket nests these inside pickupResponseMap → "response"
			if (pickupResponseMap != null && !alreadyInQueue) {
				Map pickupData = null;
				Object responseObj = pickupResponseMap.get("response");
				if (responseObj instanceof Map) {
					pickupData = (Map) responseObj;
				}
				else {
					// Fallback: fields at top level
					pickupData = pickupResponseMap;
				}

				Object pickupIdObj = pickupData.get("pickup_id");
				if (pickupIdObj instanceof Number) {
					shippingEO.setPickupId(((Number) pickupIdObj).longValue());
				}
				Object scheduledDateObj = pickupData.get("pickup_scheduled_date");
				if (scheduledDateObj instanceof String) {
					try {
						shippingEO.setPickupScheduledDate(LocalDateTime.parse((String) scheduledDateObj,
								DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
					}
					catch (Exception ex) {
						logger.warn("Could not parse pickup_scheduled_date '{}': {}", scheduledDateObj,
								ex.getMessage());
					}
				}
				Object tokenObj = pickupData.get("pickup_token_number");
				if (tokenObj instanceof String) {
					shippingEO.setPickupToken((String) tokenObj);
				}
			}
			ctx.shippingEO = shippingRepository.save(shippingEO);
			shippingEO = ctx.shippingEO;

			// ── Separate log record for REQUEST_PICKUP success ──
			String pickupNote = alreadyInQueue ? "Already in Pickup Queue – treated as PICKUP_SCHEDULED" : null;
			saveStepLog(event, "REQUEST_PICKUP", Constants.SUCCESS_STATUS, pickupNote);

			// ── Save PICKUP_SCHEDULED entry in shipment_tracking_history ──
			if (!shipmentTrackingHistoryRepository.existsByShipmentAndStatusIgnoreCase(shippingEO,
					"PICKUP_SCHEDULED")) {
				ShipmentTrackingHistoryEO pickupHistory = new ShipmentTrackingHistoryEO();
				pickupHistory.setShipment(shippingEO);
				pickupHistory.setStatus("PICKUP_SCHEDULED");
				pickupHistory.setRemarks(alreadyInQueue ? "Pickup already in queue - Pickup Scheduled"
						: "Pickup requested successfully");
				pickupHistory.setUpdatedAt(LocalDateTime.now());
				shipmentTrackingHistoryRepository.save(pickupHistory);
			}

			logger.info("Step REQUEST_PICKUP SUCCESS for shipmentId={}, alreadyInQueue={}", event.getShipmentId(),
					alreadyInQueue);
			recordRequestPickupStatus(shippingEO, Constants.SUCCESS_STATUS, pickupNote);
		}
		catch (Exception ex) {
			saveStepLog(event, "REQUEST_PICKUP", "FAILED", ex.getMessage());
			logger.error("Step REQUEST_PICKUP FAILED for shipmentId={}: {}", event.getShipmentId(), ex.getMessage(),
					ex);
			recordRequestPickupStatus(shippingEO, "FAILED", ex.getMessage());
		}
	}

	/**
	 * Step 4: Generate Label. Populates {@code ctx.generatedLabelUrl}. Never
	 * returns a terminal failure; failures are logged/recorded and processing
	 * continues.
	 */
	private void executeGenerateLabelStep(ShiprocketOrderEvent event, ShiprocketEventContext ctx) {
		ShippingEO shippingEO = ctx.shippingEO;
		Integer shipmentId = ctx.shipmentId;
		String generatedLabelUrl = null;
		if (ctx.labelStatusAlreadySuccess && shippingEO.getLabelUrl() != null) {
			generatedLabelUrl = shippingEO.getLabelUrl();
			saveStepLog(event, "GENERATE_LABEL", "SKIPPED",
					"Label already generated previously: " + generatedLabelUrl);
			logger.info("Step GENERATE_LABEL SKIPPED (already success) for shipmentId={}: labelUrl={}",
					event.getShipmentId(), generatedLabelUrl);
		}
		else
		try {
			List<String> shipmentIdStrings = new ArrayList<>();
			shipmentIdStrings.add(shipmentId.toString());
			Map response4 = shiprocketService.generateLabel(shipmentIdStrings);
			generatedLabelUrl = extractLabelUrl(response4);
			logger.info("Step GENERATE_LABEL raw response keys={}", response4 != null ? response4.keySet() : null);

			// ── Separate final log record for GENERATE_LABEL outcome ──
			if (generatedLabelUrl == null) {
				saveStepLog(event, "GENERATE_LABEL", "FAILED",
						"Label URL not received from Shiprocket after all attempts");
			}
			else {
				ShiprocketOrderLogEO labelLog = ShiprocketOrderLogEO.builder()
					.shipmentId(event.getShipmentId())
					.orderId(event.getOrderId())
					.warehouseId(event.getWarehouseId())
					.step("GENERATE_LABEL")
					.status(Constants.SUCCESS_STATUS)
					.labelUrl(generatedLabelUrl)
					.build();
				shiprocketOrderLogRepository.save(labelLog);
			}
			shippingEO.setLabelUrl(generatedLabelUrl);
			shippingRepository.save(shippingEO);
			logger.info("Step GENERATE_LABEL {}: labelUrl={}",
					generatedLabelUrl != null ? Constants.SUCCESS_STATUS : "FAILED", generatedLabelUrl);
			recordGenerateLabelStatus(shippingEO, generatedLabelUrl != null ? Constants.SUCCESS_STATUS : "FAILED",
					generatedLabelUrl != null ? null : "Label URL not received from Shiprocket after all attempts");
		}
		catch (Exception ex) {
			saveStepLog(event, "GENERATE_LABEL", "FAILED", ex.getMessage());
			logger.error("Step GENERATE_LABEL FAILED for shipmentId={}: {}", event.getShipmentId(), ex.getMessage(),
					ex);
			recordGenerateLabelStatus(shippingEO, "FAILED", ex.getMessage());
		}
		ctx.generatedLabelUrl = generatedLabelUrl;
	}

	/**
	 * Step 5: Track Shipment — populates track_url, etd, edd. Only attempted if
	 * GENERATE_LABEL succeeded. Never returns a terminal failure.
	 */
	private void executeTrackShipmentStep(ShiprocketOrderEvent event, ShiprocketEventContext ctx) {
		ShippingEO shippingEO = ctx.shippingEO;
		boolean generateLabelSucceeded = Constants.SUCCESS_STATUS.equals(shippingEO.getGenerateLabelStatus());
		if (ctx.trackStatusAlreadySuccess) {
			saveStepLog(event, "TRACK_SHIPMENT", "SKIPPED", "Tracking already completed successfully previously");
			logger.info("Step TRACK_SHIPMENT SKIPPED (already success) for shipmentId={}", event.getShipmentId());
		}
		else if (!generateLabelSucceeded) {
			String skipReason = "GENERATE_LABEL step did not succeed (status="
					+ shippingEO.getGenerateLabelStatus() + "); skipping TRACK_SHIPMENT";
			saveStepLog(event, "TRACK_SHIPMENT", "SKIPPED", skipReason);
			logger.warn("Step TRACK_SHIPMENT SKIPPED for shipmentId={}: {}", event.getShipmentId(), skipReason);
			recordTrackShipmentStatus(shippingEO, "SKIPPED", skipReason);
		}
		else
		try {
			String awbForTracking = shippingEO.getAwb();
			if (awbForTracking != null && !awbForTracking.isEmpty()) {
				Map trackResponse = shiprocketService.trackShipment(awbForTracking);
				if (trackResponse != null) {
					Object trackingDataObj = trackResponse.get("tracking_data");
					if (trackingDataObj instanceof Map) {
						Map trackingData = (Map) trackingDataObj;

						// track_url → shippingEO.trackUrl
						Object trackUrlObj = trackingData.get("track_url");
						if (trackUrlObj instanceof String) {
							shippingEO.setTrackUrl((String) trackUrlObj);
						}

						// etd → expectedDeliveryDate
						Object etdObj = trackingData.get("etd");
						if (etdObj instanceof String && !((String) etdObj).isEmpty()) {
							try {
								shippingEO.setExpectedDeliveryDate(LocalDateTime.parse((String) etdObj,
										DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
							}
							catch (Exception ignored) {
								logger.warn("Could not parse etd '{}' in trackShipment response", etdObj);
							}
						}

						// edd from shipment_track[0].edd → estimatedDeliveryDate
						Object shipmentTrackObj = trackingData.get("shipment_track");
						if (shipmentTrackObj instanceof List) {
							List shipmentTrackList = (List) shipmentTrackObj;
							if (!shipmentTrackList.isEmpty() && shipmentTrackList.get(0) instanceof Map) {
								Object eddObj = ((Map) shipmentTrackList.get(0)).get("edd");
								if (eddObj instanceof String && !((String) eddObj).isEmpty()) {
									try {
										shippingEO.setEstimatedDeliveryDate(LocalDateTime.parse((String) eddObj,
												DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
									}
									catch (Exception ignored) {
										logger.warn("Could not parse edd '{}' in trackShipment response", eddObj);
									}
								}
							}
						}

						shippingRepository.save(shippingEO);
						// ── Separate log record for TRACK_SHIPMENT success ──
						saveStepLog(event, "TRACK_SHIPMENT", Constants.SUCCESS_STATUS, null);
						logger.info("Step TRACK_SHIPMENT SUCCESS for awb={}: trackUrl={}, etd={}, edd={}",
								awbForTracking, shippingEO.getTrackUrl(), shippingEO.getExpectedDeliveryDate(),
								shippingEO.getEstimatedDeliveryDate());
						recordTrackShipmentStatus(shippingEO, Constants.SUCCESS_STATUS, null);
						boolean estimateAvailable = shippingEO.getEstimatedDeliveryDate() != null
								|| shippingEO.getExpectedDeliveryDate() != null;
						recordEstimateStatus(shippingEO, estimateAvailable ? Constants.SUCCESS_STATUS : "NOT_AVAILABLE",
								estimateAvailable ? null
										: "No estimated/expected delivery date returned by Shiprocket tracking response");
					}
				}
			}
			else {
				saveStepLog(event, "TRACK_SHIPMENT", "SKIPPED",
						"AWB not available for shipmentId=" + event.getShipmentId());
				logger.warn("Step TRACK_SHIPMENT skipped: AWB not available for shipmentId={}",
						event.getShipmentId());
				recordTrackShipmentStatus(shippingEO, "SKIPPED", "AWB not available for shipmentId=" + event.getShipmentId());
			}
		}
		catch (Exception ex) {
			saveStepLog(event, "TRACK_SHIPMENT", "FAILED", ex.getMessage());
			logger.warn("Step TRACK_SHIPMENT FAILED for shipmentId={}: {}", event.getShipmentId(), ex.getMessage());
			recordTrackShipmentStatus(shippingEO, "FAILED", ex.getMessage());
		}
	}

	/**
	 * Determines the final order status now that all 5 steps have run, sends the
	 * "Order Status Update" notification if the status actually changed, and
	 * builds the final response DTO.
	 */
	private ShiprocketOrderEventResponseDTO finalizeOrderStatusAndBuildResponse(ShiprocketOrderEvent event,
			ShiprocketEventContext ctx) {
		ShippingEO shippingEO = ctx.shippingEO;
		OrderEO order = ctx.order;
		Integer shipOrderId = ctx.shipOrderId;
		Integer shipmentId = ctx.shipmentId;
		String awbCode = ctx.awbCode;
		String generatedLabelUrl = ctx.generatedLabelUrl;

		// ── Final Order status determination ──
		// Only when ALL five Shiprocket processing steps (order creation, AWB
		// generation, pickup request, label generation, and shipment tracking)
		// have completed successfully do we consider the shipment fully ready;
		// in that case mark the order PICKUP_SCHEDULED. If any step is
		// missing/failed, keep the order (and its already-created shipping/
		// shipment_item/tracking-history/shiprocket_order_log records) as
		// Ready to Ship so it can be completed/monitored manually.
		boolean allShipmentDetailsPresent = Constants.SUCCESS_STATUS.equals(shippingEO.getShiprocketOrderStatus())
				&& Constants.SUCCESS_STATUS.equals(shippingEO.getGenerateAwbStatus())
				&& Constants.SUCCESS_STATUS.equals(shippingEO.getRequestPickupStatus())
				&& Constants.SUCCESS_STATUS.equals(shippingEO.getGenerateLabelStatus());
		if (allShipmentDetailsPresent) {
			order.setOrderStatus(Constants.ORDER_STATUS_PICKUP_SCHEDULED);
			orderRepository.save(order);
			logger.info(
					"Order status updated to PICKUP_SCHEDULED for orderId={}: shiprocket order, AWB, pickup, label and tracking all successful",
					event.getOrderId());
		}
		else {
			markOrderReadyToShip(order, shippingEO,
					"Incomplete shipment processing (shiprocketOrderStatus=" + shippingEO.getShiprocketOrderStatus()
							+ ", generateAwbStatus=" + shippingEO.getGenerateAwbStatus()
							+ ", requestPickupStatus=" + shippingEO.getRequestPickupStatus()
							+ ", generateLabelStatus=" + shippingEO.getGenerateLabelStatus()
							+ ", trackShipmentStatus=" + shippingEO.getTrackShipmentStatus() + ")");
		}

		sendOrderStatusUpdateEmail(event, ctx);

		// ── Build the final response now that every step has run ──
		String finalFailedStep = allShipmentDetailsPresent ? null
				: (shipOrderId == null ? "CREATE_ORDER"
						: (awbCode == null ? "GENERATE_AWB"
								: (generatedLabelUrl == null ? "GENERATE_LABEL" : "REQUEST_PICKUP")));
		return ShiprocketOrderEventResponseDTO.builder()
			.responseStatus(allShipmentDetailsPresent ? Constants.SUCCESS_STATUS : Constants.FAILURE_STATUS)
			.responseMessage(allShipmentDetailsPresent
					? "Shiprocket order, courier, AWB and label all generated successfully"
					: "Incomplete shipment details (shipOrderId=" + shipOrderId + ", awb=" + awbCode
							+ ", courierCompanyId=" + shippingEO.getCourierCompanyId() + ", labelUrl="
							+ generatedLabelUrl + ")")
			.shipmentId(event.getShipmentId())
			.orderId(event.getOrderId())
			.warehouseId(event.getWarehouseId())
			.shipOrderId(shipOrderId)
			.shipShipmentId(shipmentId)
			.awbCode(awbCode)
			.courierCompanyId(shippingEO.getCourierCompanyId())
			.courierName(shippingEO.getCourierName())
			.labelUrl(generatedLabelUrl)
			.trackUrl(shippingEO.getTrackUrl())
			.shipmentStatus(shippingEO.getShipmentStatus())
			.failedStep(finalFailedStep)
			.build();
	}

	/**
	 * Sends the "Order Status Update" notification now that all 5 shipment steps
	 * have completed, but only if the order's status actually changed as a
	 * result of this processing run. Never throws — failures are logged only.
	 */
	private void sendOrderStatusUpdateEmail(ShiprocketOrderEvent event, ShiprocketEventContext ctx) {
		ShippingEO shippingEO = ctx.shippingEO;
		OrderEO order = ctx.order;
		try {
			String currentOrderStatusForEmail = order.getOrderStatus();
			boolean orderStatusChanged = currentOrderStatusForEmail != null
					&& !currentOrderStatusForEmail.equals(ctx.previousOrderStatusForEmail);
			if (orderStatusChanged) {
				OrderEO emailOrder = order;
				if (emailOrder.getCustomer() != null) {
					CustomerEO customer = emailOrder.getCustomer();
					String customerName = customer.getFirstName();
					String customerEmail = customer.getEmail();
					String customerMobile = customer.getMobileNumber();

					// Format expected delivery date
					String deliveryDateStr = null;
					DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
					if (shippingEO.getEstimatedDeliveryDate() != null) {
						deliveryDateStr = shippingEO.getEstimatedDeliveryDate().format(displayFmt);
					}
					else if (shippingEO.getExpectedDeliveryDate() != null) {
						deliveryDateStr = shippingEO.getExpectedDeliveryDate().format(displayFmt);
					}

					EmailDetails emailDetails = EmailDetails.builder()
						.orderId(emailOrder.getOrderNumber())
						.customerName(customerName)
						.orderStatus(emailOrder.getOrderStatus())
						.trackingNumber(shippingEO.getAwb())
						.expectedDelivery(deliveryDateStr)
						.trackingUrl(shippingEO.getTrackUrl())
						.build();

					Event notificationEvent = Event.builder()
						.email(customerEmail)
						.mobile(customerMobile)
						.purpose(Constants.COMMUNICATION_PURPOSE_ORDER_CONFIRMATION)
						.emailSubject("Order Status Update - " + emailOrder.getOrderNumber())
						.emailMessage(String.format("Hi %s, your order %s is now %s. Track: %s", customerName,
								emailOrder.getOrderNumber(), emailOrder.getOrderStatus(),
								shippingEO.getTrackUrl()))
						.smsSubject("Order Status Update")
						.smsMessage(String.format("Order #%s is now %s. Track: %s", emailOrder.getOrderNumber(),
								emailOrder.getOrderStatus(), shippingEO.getAwb()))
						.channel(Constants.COMMUNICATION_CHANNEL_BOTH)
						.emailDetails(emailDetails)
						.build();

					notificationService.processEvent(notificationEvent);
					logger.info(
							"Order status update email event sent for orderNumber={} after all 5 shipment steps completed (status changed {} -> {})",
							emailOrder.getOrderNumber(), ctx.previousOrderStatusForEmail, currentOrderStatusForEmail);
				}
			}
			else {
				logger.info(
						"Skipping order status update email for orderId={}: order status unchanged ({}) after all 5 shipment steps completed",
						event.getOrderId(), currentOrderStatusForEmail);
			}
		}
		catch (Exception emailEx) {
			logger.error("Failed to send order status update email for shipmentId={}: {}", event.getShipmentId(),
					emailEx.getMessage());
		}
	}

	@Override
	public ShipTrackHistoryResponseDTO getShippingHistory(ShipTrackHistoryRequestDTO requestDTO) {
		ShipTrackHistoryResponseDTO responseDTO = new ShipTrackHistoryResponseDTO();
		logger.info("Fetching shipping history for tracking number: {}",
				requestDTO != null ? requestDTO.getTrackId() : null);
		try {
			String trackingNumber = requestDTO.getTrackId();
			// Fetch only non-cancelled shipment by tracking number
			ShippingEO shipment = shippingRepository.findByTrackingNumberNonCancelled(trackingNumber);
			if (shipment == null) {
				logger.warn("No non-cancelled shipment found for tracking number: {}", trackingNumber);
				return responseDTO;
			}
			List<ShipmentTrackingHistoryEO> historyList = shipmentTrackingHistoryRepository
				.findByShipmentOrderByUpdatedAtAsc(shipment);
			List<ShipTrackHistoryDTO> history = new ArrayList<>();
			for (ShipmentTrackingHistoryEO entry : historyList) {
				ShipTrackHistoryDTO h = new ShipTrackHistoryDTO();
				h.setStatus(entry.getStatus());
				h.setDate(entry.getUpdatedAt());
				h.setLocation(entry.getLocation());
				h.setRemarks(entry.getRemarks());
				history.add(h);
			}
			responseDTO.setHistory(history);
			logger.info("Shipping history fetched successfully for tracking number: {}", trackingNumber);
		}
		catch (Exception e) {
			logger.error("Error occurred while fetching shipping history: {}", e.getMessage(), e);
			responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
			responseDTO.setResponseMessage("An error occurred while fetching shipping history. Please try again later");
		}
		return responseDTO;
	}

	@Override
	public ShipStatusUpdateResponseDTO shipmentStatusUpdate(ShipStatusUpdateRequestDTO requestDTO) {
		logger.info("Received shipment status update request: {}", requestDTO);
		ShipStatusUpdateResponseDTO response = new ShipStatusUpdateResponseDTO();
		try {
			String trackingNumber = requestDTO.getTrackingNumber();
			logger.debug("Looking up shipment by tracking number: {}", trackingNumber);
			ShippingEO shipment = shippingRepository.findByTrackingNumber(trackingNumber);
			if (shipment == null) {
				logger.warn("Shipment not found for tracking number: {}", trackingNumber);
				response.setStatus(Constants.FAILURE_STATUS);
				response.setStatusMessage("Shipment not found");
				return response;
			}

			// ── Idempotency guard ────────────────────────────────────────────
			// The same status can arrive twice for a shipment: once when it is
			// updated in-app (e.g. as part of the order-cancel flow) and again via
			// the Shiprocket webhook callback. Check ShipmentTrackingHistoryEO for
			// an existing record with the same status for this shipment before
			// creating a new one, so the Track Order page doesn't show the same
			// status twice.
			boolean alreadyRecorded = shipmentTrackingHistoryRepository.existsByShipmentAndStatusIgnoreCase(shipment,
					requestDTO.getStatus());
			if (alreadyRecorded) {
				logger.info(
						"ShipmentTrackingHistoryEO already has a record with status='{}' for trackingNumber={}. Skipping duplicate status update (likely already updated in-app; webhook update ignored).",
						requestDTO.getStatus(), trackingNumber);
				response.setStatus(Constants.SUCCESS_STATUS);
				response.setStatusMessage("Shipment already has a tracking history record with status: "
						+ requestDTO.getStatus() + ". No update performed.");
				return response;
			}

			ShipmentTrackingHistoryEO history = new ShipmentTrackingHistoryEO();
			history.setShipment(shipment);
			history.setStatus(requestDTO.getStatus());
			history.setLocation(requestDTO.getLocation());
			history.setRemarks(requestDTO.getRemarks());
			history.setUpdatedAt(requestDTO.getEventTime());
			shipmentTrackingHistoryRepository.save(history);
			logger.info("Saved shipment tracking history for tracking number: {}", trackingNumber);

			shipment.setShipmentStatus(requestDTO.getStatus());
			shippingRepository.save(shipment);
			logger.info("Updated shipment status for tracking number: {} to status: {}", trackingNumber,
					requestDTO.getStatus());

			// update order status based on shipment status
			OrderEO order = shipment.getOrder();
			if (requestDTO.getStatus().equalsIgnoreCase(Constants.SHIPMENT_STATUS_DELIVERED)) {
				order.setOrderStatus(Constants.ORDER_STATUS_DELIVERED);
				logger.info("Order status set to DELIVERED for order: {}", order != null ? order.getOrderId() : null);
			}
			else {
				order.setOrderStatus(requestDTO.getStatus());
				logger.info("Order status set to SHIPPED for order: {}", order != null ? order.getOrderId() : null);
			}
			orderRepository.save(order);

			// Initiate refund if this is a RETURN_PICKUP shipment received at warehouse
			if (Constants.SHIPMENT_TYPE_RETURN_PICKUP.equalsIgnoreCase(shipment.getType())
					&& Constants.SHIPMENT_STATUS_RECEIVED.equalsIgnoreCase(requestDTO.getStatus())) {
				logger.info("Return pickup shipment RECEIVED for trackingNumber={}, initiating refund for orderId={}",
						trackingNumber, order != null ? order.getOrderId() : null);
				initiateReturnRefund(shipment, order);
			}

			// Update return_request and return_status_history if shipment is
			// RETURN_PICKUP
			if (Constants.SHIPMENT_TYPE_RETURN_PICKUP.equalsIgnoreCase(shipment.getType())) {
				logger.info("Updating return request status for RETURN_PICKUP trackingNumber={}, newStatus={}",
						trackingNumber, requestDTO.getStatus());
				updateReturnRequestStatus(order, requestDTO.getStatus(), requestDTO.getRemarks());
			}

			response.setStatus(Constants.SUCCESS_STATUS);
			response.setStatusMessage("Shipment status updated successfully");
			logger.info("Shipment status update successful for tracking number: {}", trackingNumber);
		}
		catch (Exception e) {
			logger.error("Error occurred while updating shipment status: {}", e.getMessage(), e);
			response.setStatus(Constants.FAILURE_STATUS);
			response.setStatusMessage("An error occurred while updating shipment status. Please try again later");
		}
		return response;
	}

	/**
	 * Creates a RefundTransactionEO and publishes a RefundInitiatedEvent when a
	 * RETURN_PICKUP shipment is marked as RECEIVED at the warehouse.
	 */
	private void initiateReturnRefund(ShippingEO shipment, OrderEO order) {
		try {
			if (order == null) {
				logger.warn("initiateReturnRefund: order is null for shipmentId={}", shipment.getShipmentId());
				return;
			}

			// Fetch payment for the order
			PaymentEO payment = paymentRepository.findByOrder(order).orElse(null);
			if (payment == null) {
				logger.warn("initiateReturnRefund: no payment found for orderId={}", order.getOrderId());
				return;
			}

			// Build and save RefundTransactionEO
			String refundReference = "REFUND-RTN-" + order.getOrderNumber() + "-" + System.currentTimeMillis();
			RefundTransactionEO refundTransaction = RefundTransactionEO.builder()
				.orderId(order.getOrderId() != null ? order.getOrderId().longValue() : null)
				.paymentTransactionId(payment.getPaymentId() != null ? payment.getPaymentId().longValue() : null)
				.refundReference(refundReference)
				.refundType("RETURN")
				.refundReason("Return pickup received at warehouse. Tracking: " + shipment.getTrackingNumber())
				.requestedAmount(order.getTotalAmount())
				.currency(order.getCurrency() != null ? order.getCurrency() : Constants.PAYMENT_CURRENCY)
				.status(Constants.PAYMENT_REFUND_STATUS_INPROGRESS)
				.initiatedAt(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
			RefundTransactionEO savedRefund = refundTransactionRepository.save(refundTransaction);
			logger.info("Created RefundTransactionEO id={} refundReference={} for orderId={}", savedRefund.getId(),
					refundReference, order.getOrderId());

			// Build RefundInitiatedEvent and process it directly (in-process)
			RefundInitiatedEvent event = RefundInitiatedEvent.builder()
				.orderId(order.getOrderId() != null ? order.getOrderId().longValue() : null)
				.refundReference(refundReference)
				.amount(order.getTotalAmount())
				.currency(savedRefund.getCurrency())
				.build();
			// Directly process refund initiated event
			orderService.processRefundInitiatedEvent(event);
			logger.info("Triggered refund processing for refundReference={}, orderId={}", refundReference,
					order.getOrderId());

		}
		catch (Exception e) {
			logger.error("Error in initiateReturnRefund for shipmentId={}, orderId={}: {}",
					shipment != null ? shipment.getShipmentId() : null, order != null ? order.getOrderId() : null,
					e.getMessage(), e);
		}
	}

	/**
	 * Maps the incoming shipment status to a return request status and updates both
	 * return_request and return_status_history tables. Only called when shipment type is
	 * RETURN_PICKUP.
	 */
	private void updateReturnRequestStatus(OrderEO order, String shipmentStatus, String remarks) {
		try {
			if (order == null) {
				logger.warn("updateReturnRequestStatus: order is null, skipping");
				return;
			}

			// Map shipment status → return request status
			String returnStatus = mapShipmentStatusToReturnStatus(shipmentStatus);
			if (returnStatus == null) {
				logger.info("updateReturnRequestStatus: no return status mapping for shipmentStatus={}, skipping",
						shipmentStatus);
				return;
			}

			// Fetch all return requests for this order
			List<ReturnRequestEO> returnRequests = returnRequestRepository.findByOrder(order);
			if (returnRequests == null || returnRequests.isEmpty()) {
				logger.warn("updateReturnRequestStatus: no return request found for orderId={}", order.getOrderId());
				return;
			}

			for (ReturnRequestEO returnRequest : returnRequests) {
				String previousStatus = returnRequest.getStatus();
				returnRequest.setStatus(returnStatus);
				returnRequestRepository.save(returnRequest);
				logger.info("Updated ReturnRequestEO returnId={} status from '{}' to '{}'", returnRequest.getReturnId(),
						previousStatus, returnStatus);

				// Save return status history entry
				ReturnStatusHistoryEO statusHistory = new ReturnStatusHistoryEO();
				statusHistory.setReturnRequest(returnRequest);
				statusHistory.setNewStatus(returnStatus);
				statusHistory.setActivityType("SHIPMENT_STATUS_UPDATE");
				statusHistory.setRemarks(remarks != null && !remarks.trim().isEmpty() ? remarks
						: "Shipment status updated to: " + shipmentStatus);
				statusHistory.setChangedAt(LocalDateTime.now());
				returnStatusHistoryRepository.save(statusHistory);
				logger.info("Saved ReturnStatusHistory for returnId={} with status='{}'", returnRequest.getReturnId(),
						returnStatus);
			}

		}
		catch (Exception e) {
			logger.error("Error in updateReturnRequestStatus for orderId={}, shipmentStatus={}: {}",
					order != null ? order.getOrderId() : null, shipmentStatus, e.getMessage(), e);
		}
	}

	/**
	 * Maps shipment status values to the corresponding return request status. Returns
	 * null if no mapping applies (status should not update return request).
	 */
	private String mapShipmentStatusToReturnStatus(String shipmentStatus) {
		if (shipmentStatus == null)
			return null;
		switch (shipmentStatus.toUpperCase()) {
			case "RETURN_PICKUP_INITIATED":
				return Constants.RETURN_STATUS_APPROVED;
			case "IN_TRANSIT":
				return Constants.RETURN_STATUS_PICKUP_IN_TRANSIT;
			case "RECEIVED":
				return Constants.RETURN_STATUS_RECEIVED;
			default:
				return null;
		}
	}

	@Override
	public AllShipmentsResponseDTO getAllShipments(String status, String orderNumber) {
		AllShipmentsResponseDTO response = new AllShipmentsResponseDTO();
		logger.info("getAllShipments called with status filter: {}, orderNumber filter: {}", status, orderNumber);
		try {
			String statusFilter = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
			String orderNumberFilter = (orderNumber != null && !orderNumber.trim().isEmpty()) ? orderNumber.trim()
					: null;
			List<ShippingEO> shippingList = shippingRepository.findAllByOptionalStatusAndOrderNumber(statusFilter,
					orderNumberFilter);

			List<ShipmentDetailDTO> shipmentDetails = new ArrayList<>();
			for (ShippingEO shipping : shippingList) {
				ShipmentDetailDTO detail = new ShipmentDetailDTO();
				detail.setShipmentId(shipping.getShipmentId());
				detail.setTrackingNumber(shipping.getTrackingNumber());
				detail.setCourierName(shipping.getCourierName());
				detail.setCourierCompanyId(shipping.getCourierCompanyId());
				detail.setAwb(shipping.getAwb());
				detail.setShipmentType(shipping.getType());
				detail.setShipmentStatus(shipping.getShipmentStatus());
				detail.setShippedDate(shipping.getShippedDate());
				detail.setDeliveredDate(shipping.getDeliveredDate());
				detail.setCreatedAt(shipping.getCreatedAt());
				detail.setUpdatedAt(shipping.getUpdatedAt());
				detail.setShippingPrice(shipping.getShippingPrice());
				if (shipping.getOrder() != null) {
					detail.setOrderId(shipping.getOrder().getOrderId() != null
							? shipping.getOrder().getOrderId().longValue() : null);
					detail.setOrderNumber(shipping.getOrder().getOrderNumber());
				}

				// Fetch courier candidates for this shipment
				try {
					List<CourierSelectionLogDTO> candidates = buildCourierCandidateDTOs(shipping.getShipmentId());
					detail.setCourierCandidates(candidates);
				}
				catch (Exception ex) {
					logger.warn("Could not load courierCandidates for shipmentId={}: {}", shipping.getShipmentId(),
							ex.getMessage());
				}

				// Fetch tracking history for this shipment
				List<ShipmentTrackingHistoryEO> historyList = shipmentTrackingHistoryRepository
					.findByShipmentOrderByUpdatedAtAsc(shipping);
				List<ShipTrackHistoryDTO> historyDTOs = new ArrayList<>();
				for (ShipmentTrackingHistoryEO h : historyList) {
					ShipTrackHistoryDTO dto = new ShipTrackHistoryDTO();
					dto.setStatus(h.getStatus());
					dto.setLocation(h.getLocation());
					dto.setRemarks(h.getRemarks());
					dto.setDate(h.getUpdatedAt());
					historyDTOs.add(dto);
				}
				detail.setTrackingHistory(historyDTOs);
				shipmentDetails.add(detail);
			}

			response.setShipments(shipmentDetails);
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Shipments fetched successfully. Total: " + shipmentDetails.size());
			logger.info("getAllShipments fetched {} records with statusFilter={}, orderNumberFilter={}",
					shipmentDetails.size(), statusFilter, orderNumberFilter);
		}
		catch (Exception e) {
			logger.error("Error in getAllShipments with status={}, orderNumber={}: {}", status, orderNumber,
					e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while fetching shipments: " + e.getMessage());
		}
		return response;
	}

	@Override
	public ResponseCreateCartonDTO addCarton(RequestCreateCartonDTO requestCreateCartonDTO) {
		ResponseCreateCartonDTO response = new ResponseCreateCartonDTO();
		try {
			if (requestCreateCartonDTO == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Request body must not be null");
				return response;
			}
			CartonEO cartonEO = new CartonEO();
			cartonEO.setName(requestCreateCartonDTO.getName());
			cartonEO.setLength(requestCreateCartonDTO.getLength());
			cartonEO.setBreadth(requestCreateCartonDTO.getBreadth());
			cartonEO.setHeight(requestCreateCartonDTO.getHeight());
			cartonEO.setMaxWeight(requestCreateCartonDTO.getMaxWeight());
			cartonEO.setEmptyWeight(requestCreateCartonDTO.getEmptyWeight());
			cartonEO.setStatus("A"); // default Active on creation
			cartonEO.setWho(requestCreateCartonDTO.getWho());
			CartonEO savedCarton = cartonRepository.save(cartonEO);
			response.setId(savedCarton.getId());
			response.setName(savedCarton.getName());
			response.setLength(savedCarton.getLength());
			response.setBreadth(savedCarton.getBreadth());
			response.setHeight(savedCarton.getHeight());
			response.setMaxWeight(savedCarton.getMaxWeight());
			response.setEmptyWeight(savedCarton.getEmptyWeight());
			response.setStatus(savedCarton.getStatus());
			response.setWho(savedCarton.getWho());
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Carton created successfully with id: " + savedCarton.getId());
		}
		catch (Exception e) {
			logger.error("Error in addCarton: {}", e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while creating carton: " + e.getMessage());
		}
		return response;
	}

	@Override
	public ResponseCreateCartonDTO getCartonById(Long id) {
		ResponseCreateCartonDTO response = new ResponseCreateCartonDTO();
		try {
			if (id == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Carton id must not be null");
				return response;
			}
			CartonEO carton = cartonRepository.findById(id).orElse(null);
			if (carton == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Carton not found with id: " + id);
				return response;
			}
			mapCartonToResponse(carton, response);
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Carton fetched successfully");
		}
		catch (Exception e) {
			logger.error("Error in getCartonById for id={}: {}", id, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while fetching carton: " + e.getMessage());
		}
		return response;
	}

	@Override
	public CartonListResponseDTO getAllCartons(String status) {
		CartonListResponseDTO response = new CartonListResponseDTO();
		try {
			List<CartonEO> cartons;
			if (status != null && !status.trim().isEmpty()) {
				cartons = cartonRepository.findAllByStatusOrderByLengthAscBreadthAscHeightAsc(status.trim());
			}
			else {
				cartons = cartonRepository.findAll();
			}
			List<ResponseCreateCartonDTO> dtos = new ArrayList<>();
			for (CartonEO c : cartons) {
				ResponseCreateCartonDTO dto = new ResponseCreateCartonDTO();
				mapCartonToResponse(c, dto);
				dtos.add(dto);
			}
			response.setCartons(dtos);
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Cartons fetched successfully. Total: " + dtos.size());
		}
		catch (Exception e) {
			logger.error("Error in getAllCartons with status={}: {}", status, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while fetching cartons: " + e.getMessage());
		}
		return response;
	}

	@Override
	public ResponseCreateCartonDTO updateCarton(Long id, CartonUpdateRequestDTO request) {
		ResponseCreateCartonDTO response = new ResponseCreateCartonDTO();
		try {
			if (id == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Carton id must not be null");
				return response;
			}
			if (request == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Request body must not be null");
				return response;
			}
			CartonEO carton = cartonRepository.findById(id).orElse(null);
			if (carton == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Carton not found with id: " + id);
				return response;
			}
			carton.setName(request.getName());
			carton.setLength(request.getLength());
			carton.setBreadth(request.getBreadth());
			carton.setHeight(request.getHeight());
			carton.setMaxWeight(request.getMaxWeight());
			carton.setEmptyWeight(request.getEmptyWeight());
			carton.setWho(request.getWho());
			CartonEO updated = cartonRepository.save(carton);
			mapCartonToResponse(updated, response);
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Carton updated successfully with id: " + updated.getId());
		}
		catch (Exception e) {
			logger.error("Error in updateCarton for id={}: {}", id, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while updating carton: " + e.getMessage());
		}
		return response;
	}

	@Override
	public ResponseDTO deleteCarton(Long id, CartonStatusChangeRequestDTO request) {
		ResponseDTO response = new ResponseDTO();
		try {
			if (id == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Carton id must not be null");
				return response;
			}
			if (request == null || request.getStatus() == null || request.getStatus().trim().isEmpty()) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Status must not be null or empty");
				return response;
			}
			String newStatus = request.getStatus().trim().toUpperCase();
			if (!newStatus.equals("A") && !newStatus.equals("I")) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Status must be 'A' (Active) or 'I' (Inactive)");
				return response;
			}
			CartonEO carton = cartonRepository.findById(id).orElse(null);
			if (carton == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Carton not found with id: " + id);
				return response;
			}
			carton.setStatus(newStatus);
			if (request.getWho() != null && !request.getWho().trim().isEmpty()) {
				carton.setWho(request.getWho());
			}
			cartonRepository.save(carton);
			String action = "I".equals(newStatus) ? "deactivated (soft-deleted)" : "reactivated";
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Carton " + id + " " + action + " successfully");
		}
		catch (Exception e) {
			logger.error("Error in deleteCarton for id={}: {}", id, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while updating carton status: " + e.getMessage());
		}
		return response;
	}

	/** Helper to map a CartonEO to a ResponseCreateCartonDTO. */
	private void mapCartonToResponse(CartonEO carton, ResponseCreateCartonDTO dto) {
		dto.setId(carton.getId());
		dto.setName(carton.getName());
		dto.setLength(carton.getLength());
		dto.setBreadth(carton.getBreadth());
		dto.setHeight(carton.getHeight());
		dto.setMaxWeight(carton.getMaxWeight());
		dto.setEmptyWeight(carton.getEmptyWeight());
		dto.setStatus(carton.getStatus());
		dto.setWho(carton.getWho());
	}

	// ─── AWB / Label extraction helpers ──────────────────────────────────────

	/**
	 * Extracts the AWB code from a Shiprocket generateAWB response map. Checks:
	 * response→data→awb_code, response→awb_code, top-level awb_code.
	 */
	private String extractAwbCode(Map response2) {
		if (response2 == null)
			return null;
		// Primary: response → data → awb_code
		Object responseObj = response2.get("response");
		if (responseObj instanceof Map) {
			Object dataObj = ((Map) responseObj).get("data");
			if (dataObj instanceof Map) {
				Object awb = ((Map) dataObj).get("awb_code");
				if (awb instanceof String && !((String) awb).isEmpty())
					return (String) awb;
			}
			// Fallback: directly inside "response"
			Object directAwb = ((Map) responseObj).get("awb_code");
			if (directAwb instanceof String && !((String) directAwb).isEmpty())
				return (String) directAwb;
		}
		// Top-level fallback
		Object topLevel = response2.get("awb_code");
		if (topLevel instanceof String && !((String) topLevel).isEmpty())
			return (String) topLevel;
		return null;
	}

	/**
	 * Extracts awb_generate_error message from a Shiprocket generateAWB response map.
	 */
	private String extractAwbGenerateError(Map response2) {
		if (response2 == null)
			return null;
		Object errTop = response2.get("awb_generate_error");
		if (errTop instanceof String && !((String) errTop).isEmpty())
			return (String) errTop;
		Object responseObj = response2.get("response");
		if (responseObj instanceof Map) {
			Object errNested = ((Map) responseObj).get("awb_generate_error");
			if (errNested instanceof String && !((String) errNested).isEmpty())
				return (String) errNested;
		}
		return null;
	}

	/**
	 * Extracts label_url from a Shiprocket generateLabel response map. Checks:
	 * response[0]→label_url, top-level label_url.
	 */
	private String extractLabelUrl(Map response4) {
		if (response4 == null)
			return null;
		Object responseListObj = response4.get("response");
		if (responseListObj instanceof List) {
			List responseList = (List) responseListObj;
			if (!responseList.isEmpty() && responseList.get(0) instanceof Map) {
				Object nestedUrl = ((Map) responseList.get(0)).get("label_url");
				if (nestedUrl instanceof String && !((String) nestedUrl).isEmpty())
					return (String) nestedUrl;
			}
		}
		Object topLevel = response4.get("label_url");
		if (topLevel instanceof String && !((String) topLevel).isEmpty())
			return (String) topLevel;
		return null;
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Manual Shiprocket step APIs
	// ───────��──────────────────────────────────────────────────────────────────

	@Override
	public AwbResponse generateAwb(AwbRequest request) {
		logger.info("generateAwb called with shipmentId={}", request != null ? request.getShipmentId() : null);
		if (request == null || request.getShipmentId() == null) {
			AwbResponse error = new AwbResponse();
			error.setAwbAssignStatus(0);
			return error;
		}
		Map rawResponse = shiprocketService.generateAWB(request.getShipmentId(), request.getCourierId());
		AwbResponse awbResponse = new ObjectMapper().convertValue(rawResponse, AwbResponse.class);

		// Persist AWB code, courier company id, courier name and expected delivery date
		// into ShippingEO
		if (awbResponse != null && awbResponse.getResponse() != null
				&& awbResponse.getResponse().getResolvedAwbCode() != null) {
			String awbCode = awbResponse.getResponse().getResolvedAwbCode();
			Integer ccId = awbResponse.getResponse().getResolvedCourierCompanyId();
			String cn = awbResponse.getResponse().getResolvedCourierName();
			String etd = awbResponse.getResponse().getResolvedEtd();
			// Extract freight_charge from raw response for shipping_price
			java.math.BigDecimal resolvedShippingPrice = null;
			if (rawResponse != null) {
				Object respObj = rawResponse.get("response");
				Map dataMap = null;
				if (respObj instanceof Map) {
					Object dataObj = ((Map) respObj).get("data");
					dataMap = (dataObj instanceof Map) ? (Map) dataObj : (Map) respObj;
				}
				if (dataMap != null) {
					Object freightObj = dataMap.get("freight_charge");
					if (freightObj == null)
						freightObj = dataMap.get("rate");
					if (freightObj instanceof Number) {
						resolvedShippingPrice = new java.math.BigDecimal(((Number) freightObj).doubleValue());
					}
				}
			}
			final java.math.BigDecimal finalShippingPrice = resolvedShippingPrice;
			shippingRepository.findByShipShipmentId(request.getShipmentId()).ifPresent(shippingEO -> {
				shippingEO.setAwb(awbCode);
				if (ccId != null)
					shippingEO.setCourierCompanyId(ccId);
				if (cn != null)
					shippingEO.setCourierName(cn);
				if (finalShippingPrice != null)
					shippingEO.setShippingPrice(finalShippingPrice);
				if (etd != null && !etd.isEmpty()) {
					try {
						shippingEO.setExpectedDeliveryDate(
								LocalDateTime.parse(etd, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
					}
					catch (Exception ignored) {
						try {
							shippingEO.setExpectedDeliveryDate(
									java.time.LocalDate.parse(etd).atStartOfDay());
						}
						catch (Exception ex2) {
							logger.warn("Could not parse etd '{}': {}", etd, ex2.getMessage());
						}
					}
				}
				shippingRepository.save(shippingEO);
				logger.info(
						"AWB code {} saved for shipShipmentId={}, courierCompanyId={}, courierName={}, etd={}, shippingPrice={}",
						awbCode, request.getShipmentId(), ccId, cn, etd, finalShippingPrice);
			});
		}
		return awbResponse;
	}

	@Override
	public PickupResponse requestPickup(PickupRequest request) {
		logger.info("requestPickup called with shipmentIds={}", request != null ? request.getShipmentId() : null);
		if (request == null || request.getShipmentId() == null || request.getShipmentId().isEmpty()) {
			return new PickupResponse();
		}
		// Shiprocket expects the first (or only) shipment id as a string
		Integer firstShipmentId = request.getShipmentId().get(0);
		String shipmentIdStr = firstShipmentId.toString();
		Map rawResponse = shiprocketService.requestPickup(shipmentIdStr);
		PickupResponse pickupResponse = new ObjectMapper().convertValue(rawResponse, PickupResponse.class);

		// Persist pickup details into ShippingEO
		if (pickupResponse != null) {
			shippingRepository.findByShipShipmentId(firstShipmentId).ifPresent(shippingEO -> {
				Long resolvedPickupId = pickupResponse.getResolvedPickupId();
				String resolvedScheduledDate = pickupResponse.getResolvedPickupScheduledDate();
				String resolvedToken = pickupResponse.getResolvedPickupTokenNumber();

				if (resolvedPickupId != null)
					shippingEO.setPickupId(resolvedPickupId);
				if (resolvedScheduledDate != null) {
					try {
						shippingEO.setPickupScheduledDate(LocalDateTime.parse(resolvedScheduledDate,
								DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
					}
					catch (Exception ex) {
						logger.warn("Could not parse pickupScheduledDate '{}': {}", resolvedScheduledDate,
								ex.getMessage());
					}
				}
				if (resolvedToken != null)
					shippingEO.setPickupToken(resolvedToken);
				shippingRepository.save(shippingEO);
				logger.info("Saved pickup details for shipShipmentId={}: pickupId={}, scheduledDate={}, token={}",
						firstShipmentId, resolvedPickupId, resolvedScheduledDate, resolvedToken);
			});
		}
		return pickupResponse;
	}

	@Override
	public LabelResponse generateLabel(LabelRequest request) {
		logger.info("generateLabel called with shipmentIds={}", request != null ? request.getShipmentId() : null);
		if (request == null || request.getShipmentId() == null || request.getShipmentId().isEmpty()) {
			LabelResponse error = new LabelResponse();
			error.setLabelCreated(0);
			return error;
		}
		List<String> shipmentIdStrings = new ArrayList<>();
		for (Integer id : request.getShipmentId()) {
			shipmentIdStrings.add(id.toString());
		}
		Map rawResponse = shiprocketService.generateLabel(shipmentIdStrings);
		LabelResponse labelResponse = new ObjectMapper().convertValue(rawResponse, LabelResponse.class);

		// Persist label URL into ShippingEO for each shipment id
		if (labelResponse != null && labelResponse.getLabelUrl() != null) {
			String labelUrl = labelResponse.getLabelUrl();
			for (Integer shipmentId : request.getShipmentId()) {
				shippingRepository.findByShipShipmentId(shipmentId).ifPresent(shippingEO -> {
					shippingEO.setLabelUrl(labelUrl);
					shippingRepository.save(shippingEO);
					logger.info("Label URL saved for shipShipmentId={}", shipmentId);
				});
			}
		}
		return labelResponse;
	}

	@Override
	public TrackShipmentResponseDTO trackShipment(String awbCode) {
		TrackShipmentResponseDTO response = new TrackShipmentResponseDTO();
		logger.info("trackShipment called with awbCode={}", awbCode);
		try {
			if (awbCode == null || awbCode.trim().isEmpty()) {
				response.setResponseStatus("FAILURE");
				response.setResponseMessage("AWB code must not be null or empty");
				return response;
			}

			// 1. Lookup ShippingEO from DB by AWB
			ShippingEO shippingEO = shippingRepository.findByAwb(awbCode.trim()).orElse(null);
			if (shippingEO == null) {
				response.setResponseStatus("FAILURE");
				response.setResponseMessage("No shipment found for AWB code: " + awbCode);
				return response;
			}

			// 2. Populate internal shipment fields
			response.setShipmentId(shippingEO.getShipmentId());
			response.setAwbCode(shippingEO.getAwb());
			response.setTrackingNumber(shippingEO.getTrackingNumber());
			response.setCourierName(shippingEO.getCourierName());
			response.setCourierCompanyId(shippingEO.getCourierCompanyId());
			response.setShipmentStatus(shippingEO.getShipmentStatus());
			response.setShipmentType(shippingEO.getType());
			response.setShippedDate(shippingEO.getShippedDate());
			response.setDeliveredDate(shippingEO.getDeliveredDate());
			response.setEstimatedDeliveryDate(shippingEO.getEstimatedDeliveryDate());
			response.setExpectedDeliveryDate(shippingEO.getExpectedDeliveryDate());
			response.setPickupScheduledDate(shippingEO.getPickupScheduledDate());
			response.setLabelUrl(shippingEO.getLabelUrl());
			response.setTrackUrl(shippingEO.getTrackUrl());
			response.setShippingPrice(shippingEO.getShippingPrice());
			if (shippingEO.getOrder() != null) {
				response.setOrderId(shippingEO.getOrder().getOrderId() != null
						? shippingEO.getOrder().getOrderId().longValue() : null);
				response.setOrderNumber(shippingEO.getOrder().getOrderNumber());
			}

			// Populate courier candidates
			try {
				response.setCourierCandidates(buildCourierCandidateDTOs(shippingEO.getShipmentId()));
			}
			catch (Exception ex) {
				logger.warn("Could not load courierCandidates for awb={}: {}", awbCode, ex.getMessage());
			}

			// 3. Fetch local tracking history from DB
			List<ShipmentTrackingHistoryEO> historyList = shipmentTrackingHistoryRepository
				.findByShipmentOrderByUpdatedAtAsc(shippingEO);
			List<ShipTrackHistoryDTO> historyDTOs = new ArrayList<>();
			for (ShipmentTrackingHistoryEO h : historyList) {
				ShipTrackHistoryDTO dto = new ShipTrackHistoryDTO();
				dto.setStatus(h.getStatus());
				dto.setLocation(h.getLocation());
				dto.setRemarks(h.getRemarks());
				dto.setDate(h.getUpdatedAt());
				historyDTOs.add(dto);
			}
			response.setTrackingHistory(historyDTOs);

			// 4. Fetch live tracking data from Shiprocket
			try {
				Map shiprocketData = shiprocketService.trackShipment(awbCode.trim());
				response.setShiprocketTracking(shiprocketData);
			}
			catch (Exception ex) {
				logger.warn("Could not fetch live tracking from Shiprocket for awb={}: {}", awbCode, ex.getMessage());
				response.setShiprocketTracking(null);
			}

			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Shipment tracked successfully");
			logger.info("trackShipment SUCCESS for awbCode={}", awbCode);

		}
		catch (Exception e) {
			logger.error("Error in trackShipment for awbCode={}: {}", awbCode, e.getMessage(), e);
			response.setResponseStatus("FAILURE");
			response.setResponseMessage("An error occurred while tracking shipment: " + e.getMessage());
		}
		return response;
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Manual Shiprocket create / update API
	// ────────────────────────────────────────────���─────────────────────────────

	@Override
	@Transactional
	public ManualShiprocketUpdateResponseDTO manualShiprocketUpdate(ManualShiprocketUpdateRequestDTO request) {

		ManualShiprocketUpdateResponseDTO response = new ManualShiprocketUpdateResponseDTO();

		logger.info("manualShiprocketUpdate called: shipmentId={}, orderId={}, orderNumber={}, step={}",
				request.getShipmentId(), request.getOrderId(), request.getOrderNumber(), request.getStep());

		try {
			// ── 1. Resolve the ShippingEO ────────────────────────────────────
			ShippingEO shippingEO = resolveShipment(request);
			if (shippingEO == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Shipment not found. Provide a valid shipmentId, orderId, or orderNumber.");
				return response;
			}

			// ── 2. Apply every non-null field from the request ───────────────
			boolean anyFieldUpdated = false;

			if (request.getShiprocketOrderId() != null) {
				shippingEO.setShipOrderId(request.getShiprocketOrderId());
				anyFieldUpdated = true;
			}
			if (request.getShiprocketShipmentId() != null) {
				shippingEO.setShipShipmentId(request.getShiprocketShipmentId());
				anyFieldUpdated = true;
			}
			if (request.getAwbCode() != null && !request.getAwbCode().trim().isEmpty()) {
				shippingEO.setAwb(request.getAwbCode().trim());
				anyFieldUpdated = true;
			}
			if (request.getCourierName() != null && !request.getCourierName().trim().isEmpty()) {
				shippingEO.setCourierName(request.getCourierName().trim());
				anyFieldUpdated = true;
			}
			if (request.getCourierCompanyId() != null) {
				shippingEO.setCourierCompanyId(request.getCourierCompanyId());
				anyFieldUpdated = true;
			}
			if (request.getShippingPrice() != null) {
				shippingEO.setShippingPrice(request.getShippingPrice());
				anyFieldUpdated = true;
			}
			if (request.getShipmentStatus() != null && !request.getShipmentStatus().trim().isEmpty()) {
				shippingEO.setShipmentStatus(request.getShipmentStatus().trim());
				anyFieldUpdated = true;
			}
			if (request.getTrackUrl() != null && !request.getTrackUrl().trim().isEmpty()) {
				shippingEO.setTrackUrl(request.getTrackUrl().trim());
				anyFieldUpdated = true;
			}
			if (request.getPickupId() != null) {
				shippingEO.setPickupId(request.getPickupId());
				anyFieldUpdated = true;
			}
			if (request.getPickupToken() != null && !request.getPickupToken().trim().isEmpty()) {
				shippingEO.setPickupToken(request.getPickupToken().trim());
				anyFieldUpdated = true;
			}

			// Parse date strings ──────────────────────────────────────────────
			if (request.getEstimatedDeliveryDate() != null && !request.getEstimatedDeliveryDate().trim().isEmpty()) {
				LocalDateTime parsed = parseDateFlexible(request.getEstimatedDeliveryDate().trim());
				if (parsed != null) {
					shippingEO.setEstimatedDeliveryDate(parsed);
					anyFieldUpdated = true;
				}
			}
			if (request.getExpectedDeliveryDate() != null && !request.getExpectedDeliveryDate().trim().isEmpty()) {
				LocalDateTime parsed = parseDateFlexible(request.getExpectedDeliveryDate().trim());
				if (parsed != null) {
					shippingEO.setExpectedDeliveryDate(parsed);
					anyFieldUpdated = true;
				}
			}
			if (request.getPickupScheduledDate() != null && !request.getPickupScheduledDate().trim().isEmpty()) {
				LocalDateTime parsed = parseDateFlexible(request.getPickupScheduledDate().trim());
				if (parsed != null) {
					shippingEO.setPickupScheduledDate(parsed);
					anyFieldUpdated = true;
				}
			}

			ShippingEO savedShipping = shippingRepository.save(shippingEO);
			logger.info("manualShiprocketUpdate: ShippingEO id={} saved, anyFieldUpdated={}",
					savedShipping.getShipmentId(), anyFieldUpdated);

			// ── 3. Create tracking history entry (if historyStatus is given) ─
			boolean historyCreated = false;
			if (request.getHistoryStatus() != null && !request.getHistoryStatus().trim().isEmpty()) {
				ShipmentTrackingHistoryEO history = ShipmentTrackingHistoryEO.builder()
					.shipment(savedShipping)
					.status(request.getHistoryStatus().trim())
					.location(request.getHistoryLocation())
					.remarks(request.getHistoryRemarks() != null ? request.getHistoryRemarks()
							: "Manual update by admin — step: " + resolveStep(request.getStep()))
					.updatedAt(LocalDateTime.now())
					.build();
				shipmentTrackingHistoryRepository.save(history);
				historyCreated = true;
				logger.info("manualShiprocketUpdate: tracking history entry created for shipmentId={}",
						savedShipping.getShipmentId());
			}

			// ── 4. Log the manual override in shiprocket_order_log ───────────
			String step = resolveStep(request.getStep());
			Long logOrderId = savedShipping.getOrder() != null && savedShipping.getOrder().getOrderId() != null
					? savedShipping.getOrder().getOrderId().longValue() : null;
			Long logWarehouseId = savedShipping.getWarehouse() != null ? savedShipping.getWarehouse().getWarehouseId()
					: null;

			ShiprocketOrderLogEO logEntry = ShiprocketOrderLogEO.builder()
				.shipmentId(savedShipping.getShipmentId())
				.orderId(logOrderId)
				.warehouseId(logWarehouseId)
				.step(step)
				.status("MANUAL_SUCCESS")
				.shiprocketOrderId(savedShipping.getShipOrderId())
				.shiprocketShipmentId(savedShipping.getShipShipmentId())
				.awbCode(savedShipping.getAwb())
				.labelUrl(savedShipping.getLabelUrl())
				.errorMessage(request.getNotes())
				.build();
			shiprocketOrderLogRepository.save(logEntry);
			logger.info("manualShiprocketUpdate: shiprocket_order_log entry saved for step={}", step);

			// ── 5. Build and return the response ─────────────────────────────
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Shipment record updated successfully via manual override.");
			response.setShipmentId(savedShipping.getShipmentId());
			response
				.setOrderNumber(savedShipping.getOrder() != null ? savedShipping.getOrder().getOrderNumber() : null);
			response.setShipmentStatus(savedShipping.getShipmentStatus());
			response.setShiprocketOrderId(savedShipping.getShipOrderId());
			response.setShiprocketShipmentId(savedShipping.getShipShipmentId());
			response.setAwbCode(savedShipping.getAwb());
			response.setCourierName(savedShipping.getCourierName());
			response.setCourierCompanyId(savedShipping.getCourierCompanyId());
			response.setLabelUrl(savedShipping.getLabelUrl());
			response.setTrackUrl(savedShipping.getTrackUrl());
			response.setShippingPrice(savedShipping.getShippingPrice());
			response.setUpdatedAt(savedShipping.getUpdatedAt());
			response.setHistoryEntryCreated(historyCreated);
			response.setStepLogged(step);

		}
		catch (Exception e) {
			logger.error("manualShiprocketUpdate: unexpected error — {}", e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred during manual Shiprocket update: " + e.getMessage());
		}
		return response;
	}

	/**
	 * Resolves a {@link ShippingEO} from the three possible identifiers in the request.
	 * Priority: shipmentId > orderId > orderNumber.
	 */
	private ShippingEO resolveShipment(ManualShiprocketUpdateRequestDTO request) {
		// 1. By internal shipmentId
		if (request.getShipmentId() != null) {
			ShippingEO eo = shippingRepository.findById(request.getShipmentId()).orElse(null);
			if (eo != null)
				return eo;
			logger.warn("resolveShipment: no ShippingEO found for shipmentId={}", request.getShipmentId());
		}
		// 2. By orderId
		if (request.getOrderId() != null) {
			OrderEO order = orderRepository.findById(request.getOrderId()).orElse(null);
			if (order != null) {
			List<ShippingEO> list = shippingRepository.findByOrder(order);
			if (list != null && !list.isEmpty()) {
				// Filter out cancelled shipments first
				List<ShippingEO> nonCancelledList = list.stream()
					.filter(s -> !Constants.SHIPMENT_STATUS_CANCELLED.equals(s.getShipmentStatus()))
					.collect(Collectors.toList());
				// Prefer the active FORWARD shipment
				return nonCancelledList.stream()
					.filter(s -> Constants.SHIPMENT_TYPE_FORWARD.equals(s.getType()))
					.findFirst()
					.orElse(nonCancelledList.isEmpty() ? null : nonCancelledList.get(0));
			}
			}
			logger.warn("resolveShipment: no ShippingEO found for orderId={}", request.getOrderId());
		}
		// 3. By orderNumber
		if (request.getOrderNumber() != null && !request.getOrderNumber().trim().isEmpty()) {
			ShippingEO eo = shippingRepository.findByTrackingNumber(request.getOrderNumber().trim());
			// trackingNumber is "TRK{orderNumber}_{warehouseId}", so also search order
			if (eo == null) {
			// Attempt to locate via order
			List<ShippingEO> all = shippingRepository.findAllByOptionalStatusAndOrderNumber(null,
					request.getOrderNumber().trim());
			if (all != null && !all.isEmpty()) {
				// Filter out cancelled shipments first
				List<ShippingEO> nonCancelledAll = all.stream()
					.filter(s -> !Constants.SHIPMENT_STATUS_CANCELLED.equals(s.getShipmentStatus()))
					.collect(Collectors.toList());
				return nonCancelledAll.stream()
					.filter(s -> Constants.SHIPMENT_TYPE_FORWARD.equals(s.getType()))
					.findFirst()
					.orElse(nonCancelledAll.isEmpty() ? null : nonCancelledAll.get(0));
			}
			}
			else {
				return eo;
			}
			logger.warn("resolveShipment: no ShippingEO found for orderNumber={}", request.getOrderNumber());
		}
		return null;
	}

	/**
	 * Returns the step label; defaults to {@code MANUAL_OVERRIDE} when the supplied value
	 * is null or blank.
	 */
	private String resolveStep(String step) {
		return (step != null && !step.trim().isEmpty()) ? step.trim() : "MANUAL_OVERRIDE";
	}

	/**
	 * Tries to parse a date string in "yyyy-MM-dd HH:mm:ss" format first, then falls back
	 * to "yyyy-MM-dd" (at start of day). Returns null if both attempts fail.
	 */
	private LocalDateTime parseDateFlexible(String dateStr) {
		try {
			return LocalDateTime.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		}
		catch (Exception ignored) {
		}
		try {
			return java.time.LocalDate.parse(dateStr).atStartOfDay();
		}
		catch (Exception e) {
			logger.warn("parseDateFlexible: could not parse date '{}'", dateStr);
			return null;
		}
	}

	// ─── Order-number-based shipment management ─────────────────────────────────

	/**
	 * GET — returns full shipping details + tracking history for the given order number.
	 */
	@Override
	@Transactional(readOnly = true)
	public ShippingDetailResponseDTO getShippingDetailsByOrderNumber(String orderNumber) {
		ShippingDetailResponseDTO response = new ShippingDetailResponseDTO();
		try {
			if (orderNumber == null || orderNumber.trim().isEmpty()) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Order number must not be null or empty.");
				return response;
			}
			List<ShippingEO> list = shippingRepository.findAllByOptionalStatusAndOrderNumber(null, orderNumber.trim());
			if (list == null || list.isEmpty()) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("No shipping record found for order number: " + orderNumber);
				return response;
			}
			// Prefer active FORWARD shipment
			ShippingEO eo = list.stream()
				.filter(s -> Constants.SHIPMENT_TYPE_FORWARD.equals(s.getType())
						&& !Constants.SHIPMENT_STATUS_CANCELLED.equals(s.getShipmentStatus()))
				.findFirst()
				.orElse(list.get(0));

			// Tracking history (chronological)
			List<ShipmentTrackingHistoryEO> historyEOs = shipmentTrackingHistoryRepository
				.findByShipmentOrderByUpdatedAtAsc(eo);
			List<ShipTrackHistoryDTO> history = historyEOs.stream()
				.map(h -> ShipTrackHistoryDTO.builder()
					.status(h.getStatus())
					.location(h.getLocation())
					.remarks(h.getRemarks())
					.date(h.getUpdatedAt())
					.build())
				.collect(Collectors.toList());

			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Shipping details fetched successfully.");
			response.setShipmentId(eo.getShipmentId());
			response.setOrderNumber(eo.getOrder() != null ? eo.getOrder().getOrderNumber() : null);
			response.setOrderId(eo.getOrder() != null && eo.getOrder().getOrderId() != null
					? eo.getOrder().getOrderId().longValue() : null);
			response.setShiprocketOrderId(eo.getShipOrderId());
			response.setShiprocketShipmentId(eo.getShipShipmentId());
			response.setAwbCode(eo.getAwb());
			response.setCourierName(eo.getCourierName());
			response.setCourierCompanyId(eo.getCourierCompanyId());
			response.setShipmentStatus(eo.getShipmentStatus());
			response.setShipmentType(eo.getType());
			response.setTrackingNumber(eo.getTrackingNumber());
			response.setLength(eo.getLength());
			response.setBreadth(eo.getBreadth());
			response.setHeight(eo.getHeight());
			response.setWeight(eo.getWeight());
			response.setShippingPrice(eo.getShippingPrice());
			response.setLabelUrl(eo.getLabelUrl());
			response.setTrackUrl(eo.getTrackUrl());
			response.setPickupId(eo.getPickupId());
			response.setPickupToken(eo.getPickupToken());
			response.setPickupScheduledDate(eo.getPickupScheduledDate());
			response.setEstimatedDeliveryDate(eo.getEstimatedDeliveryDate());
			response.setExpectedDeliveryDate(eo.getExpectedDeliveryDate());
			response.setShippedDate(eo.getShippedDate());
			response.setDeliveredDate(eo.getDeliveredDate());
			response.setCreatedAt(eo.getCreatedAt());
			response.setUpdatedAt(eo.getUpdatedAt());
			if (eo.getWarehouse() != null) {
				response.setWarehouseId(eo.getWarehouse().getWarehouseId());
				response.setWarehouseName(eo.getWarehouse().getWarehouseName());
			}
			response.setTrackingHistory(history);
		}
		catch (Exception e) {
			logger.error("getShippingDetailsByOrderNumber: error for orderNumber={} — {}", orderNumber, e.getMessage(),
					e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while fetching shipping details: " + e.getMessage());
		}
		return response;
	}

	/**
	 * PUT — update an existing shipping record identified by order number.
	 */
	@Override
	@Transactional
	public ManualShiprocketUpdateResponseDTO updateShippingByOrderNumber(String orderNumber,
			ShippingOrderRequestDTO request) {

		ManualShiprocketUpdateResponseDTO response = new ManualShiprocketUpdateResponseDTO();
		try {
			List<ShippingEO> list = shippingRepository.findAllByOptionalStatusAndOrderNumber(null, orderNumber.trim());
			if (list == null || list.isEmpty()) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("No shipping record found for order number: " + orderNumber
						+ ". Use POST to create a new record.");
				return response;
			}
			ShippingEO eo = list.stream()
				.filter(s -> Constants.SHIPMENT_TYPE_FORWARD.equals(s.getType())
						&& !Constants.SHIPMENT_STATUS_CANCELLED.equals(s.getShipmentStatus()))
				.findFirst()
				.orElse(list.get(0));

			applyShippingOrderRequest(eo, request);
			ShippingEO saved = shippingRepository.save(eo);

			boolean historyCreated = saveTrackingHistoryIfRequested(saved, request);
			syncOrderStatusWithShipment(saved);
			syncCourierSelectionLogSelection(saved);
			logShiprocketOrderLog(saved, "MANUAL_UPDATE", request.getNotes());

			buildManualUpdateResponse(response, saved, historyCreated, "MANUAL_UPDATE");
			response.setResponseMessage("Shipping record updated successfully.");
		}
		catch (Exception e) {
			logger.error("updateShippingByOrderNumber: error for orderNumber={} — {}", orderNumber, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while updating shipping record: " + e.getMessage());
		}
		return response;
	}

	/**
	 * POST — create a brand-new shipping record for the given order number.
	 */
	@Override
	@Transactional
	public ManualShiprocketUpdateResponseDTO createShippingByOrderNumber(String orderNumber,
			ShippingOrderRequestDTO request) {

		ManualShiprocketUpdateResponseDTO response = new ManualShiprocketUpdateResponseDTO();
		try {
			// Check order exists
			OrderEO order = orderRepository.findByOrderNumber(orderNumber.trim()).orElse(null);
			if (order == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("No order found for order number: " + orderNumber);
				return response;
			}
			// Guard: reject if a non-cancelled shipping record already exists
			List<ShippingEO> existing = shippingRepository.findAllByOptionalStatusAndOrderNumber(null,
					orderNumber.trim());
			boolean hasActive = existing != null && existing.stream()
				.anyMatch(s -> !Constants.SHIPMENT_STATUS_CANCELLED.equals(s.getShipmentStatus()));
			if (hasActive) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("A shipping record already exists for order number: " + orderNumber
						+ ". Use PUT to update it.");
				return response;
			}

			// Resolve warehouse if warehouseId supplied
			WarehouseEO warehouse = null;
			if (request.getWarehouseId() != null) {
				warehouse = warehouseRepository.findById(request.getWarehouseId()).orElse(null);
				if (warehouse == null) {
					logger.warn(
							"createShippingByOrderNumber: warehouseId={} not found, creating record without warehouse.",
							request.getWarehouseId());
				}
			}

			ShippingEO eo = ShippingEO.builder()
				.order(order)
				.warehouse(warehouse)
				.type(request.getShipmentType() != null ? request.getShipmentType().trim()
						: Constants.SHIPMENT_TYPE_FORWARD)
				.shipmentStatus(request.getShipmentStatus() != null ? request.getShipmentStatus().trim()
						: Constants.SHIPMENT_STATUS_CREATED)
				.build();

			applyShippingOrderRequest(eo, request);
			ShippingEO saved = shippingRepository.save(eo);

			boolean historyCreated = saveTrackingHistoryIfRequested(saved, request);
			syncOrderStatusWithShipment(saved);
			syncCourierSelectionLogSelection(saved);
			logShiprocketOrderLog(saved, "MANUAL_CREATE", request.getNotes());

			buildManualUpdateResponse(response, saved, historyCreated, "MANUAL_CREATE");
			response.setResponseMessage("Shipping record created successfully.");
		}
		catch (Exception e) {
			logger.error("createShippingByOrderNumber: error for orderNumber={} — {}", orderNumber, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while creating shipping record: " + e.getMessage());
		}
		return response;
	}

	/**
	 * GET — Fetch the live Shiprocket shipment data for the given order number, shaped
	 * exactly like the PUT /api/shipment/order/{orderNumber} request body.
	 * <p>
	 * This method does NOT read from the local shipping DB at all — the Shiprocket
	 * order is located purely via the live Shiprocket "search orders" API (matching on
	 * {@code channel_order_id}, which is the internal order number sent to Shiprocket
	 * at order-creation time), and all fields are then populated/refreshed from the
	 * live Shiprocket "order details" and "track AWB" APIs.
	 */
	@Override
	@Transactional(readOnly = true)
	public ShipmentPutPayloadResponseDTO getShiprocketPutPayloadByOrderNumber(String orderNumber) {
		ShipmentPutPayloadResponseDTO response = new ShipmentPutPayloadResponseDTO();
		try {
			if (orderNumber == null || orderNumber.trim().isEmpty()) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Order number must not be null or empty.");
				return response;
			}
			String trimmedOrderNumber = orderNumber.trim();

			// ── Resolve the Shiprocket order id live via Shiprocket's order search API ──
			// (no local DB lookup — the internal order number is the channel_order_id)
			Integer shiprocketOrderId = null;
			Map<String, Object> matchedOrder = null;
			try {
				Map<String, Object> searchResult = shiprocketService.searchOrdersByChannelOrderId(trimmedOrderNumber);
				List<Map<String, Object>> orders = extractOrderList(searchResult);

				List<Map<String, Object>> exactMatches = new ArrayList<>();
				for (Map<String, Object> order : orders) {
					Object channelOrderIdObj = order.get("channel_order_id");
					if (channelOrderIdObj != null
							&& trimmedOrderNumber.equalsIgnoreCase(channelOrderIdObj.toString().trim())) {
						exactMatches.add(order);
					}
				}
				List<Map<String, Object>> candidates = exactMatches.isEmpty() ? orders : exactMatches;

				// Prefer an active (non-cancelled) order, same precedence as before
				matchedOrder = candidates.stream()
					.filter(o -> {
						Object status = o.get("status");
						return status == null || !status.toString().toUpperCase().contains("CANCEL");
					})
					.findFirst()
					.orElse(candidates.isEmpty() ? null : candidates.get(0));

				if (matchedOrder != null) {
					Object idObj = matchedOrder.get("id");
					if (idObj == null)
						idObj = matchedOrder.get("order_id");
					shiprocketOrderId = toInteger(idObj);
				}
			}
			catch (Exception ex) {
				logger.warn(
						"getShiprocketPutPayloadByOrderNumber: Shiprocket order search failed for orderNumber={} — {}",
						trimmedOrderNumber, ex.getMessage());
			}

			// ── Fallback: resolve via the local DB if the live Shiprocket search ──
			// didn't find a match (e.g. Shiprocket's `search` query doesn't index the
			// channel_order_id reliably/immediately). Our own ShippingEO row already
			// stores the Shiprocket order/shipment IDs assigned when the order was
			// first created, so use those to continue fetching live data instead of
			// failing outright when the order clearly exists in our system.
			ShippingEO localShipment = null;
			if (shiprocketOrderId == null) {
				OrderEO localOrder = orderRepository.findByOrderNumber(trimmedOrderNumber).orElse(null);
				if (localOrder != null) {
					List<ShippingEO> localShipments = shippingRepository.findByOrder(localOrder);
					if (localShipments != null && !localShipments.isEmpty()) {
						// Prefer a shipment that actually has a Shiprocket order id, and
						// among those, the most recently created/updated one.
						localShipment = localShipments.stream()
							.filter(s -> s.getShipOrderId() != null)
							.max(Comparator.comparing(s -> s.getUpdatedAt() != null ? s.getUpdatedAt()
									: LocalDateTime.MIN))
							.orElse(null);
						if (localShipment != null) {
							shiprocketOrderId = localShipment.getShipOrderId();
							logger.info(
									"getShiprocketPutPayloadByOrderNumber: live Shiprocket search found no match for orderNumber={}, falling back to locally-stored shiprocketOrderId={}",
									trimmedOrderNumber, shiprocketOrderId);
						}
					}
				}
			}

			if (shiprocketOrderId == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("No shipment found on Shiprocket for order number: " + trimmedOrderNumber);
				return response;
			}

			response.setShiprocketOrderId(shiprocketOrderId);
			if (matchedOrder != null) {
				populateFromShiprocketOrderSummary(response, matchedOrder);
			}
			else if (localShipment != null) {
				// Seed baseline fields from the local DB record so the response is still
				// fully populated even if some of the subsequent live-refresh calls fail.
				response.setWarehouseId(
						localShipment.getWarehouse() != null ? localShipment.getWarehouse().getWarehouseId() : null);
				response.setShiprocketShipmentId(localShipment.getShipShipmentId());
				response.setAwbCode(localShipment.getAwb());
				response.setCourierName(localShipment.getCourierName());
				response.setCourierCompanyId(localShipment.getCourierCompanyId());
				response.setShipmentStatus(localShipment.getShipmentStatus());
				response.setShipmentType(localShipment.getType());
				response.setTrackingNumber(localShipment.getTrackingNumber());
				response.setLength(localShipment.getLength());
				response.setBreadth(localShipment.getBreadth());
				response.setHeight(localShipment.getHeight());
				response.setWeight(localShipment.getWeight());
				response.setShippingPrice(localShipment.getShippingPrice());
				response.setLabelUrl(localShipment.getLabelUrl());
				response.setTrackUrl(localShipment.getTrackUrl());
				if (localShipment.getEstimatedDeliveryDate() != null) {
					response.setEstimatedDeliveryDate(localShipment.getEstimatedDeliveryDate().toString());
				}
				if (localShipment.getExpectedDeliveryDate() != null) {
					response.setExpectedDeliveryDate(localShipment.getExpectedDeliveryDate().toString());
				}
			}

			// ── Refresh order/shipment level info live from Shiprocket (order/show) ──
			if (response.getShiprocketOrderId() != null) {
				try {
					Map orderDetails = shiprocketService.getOrderDetails(response.getShiprocketOrderId());
					Object dataObj = orderDetails != null ? orderDetails.get("data") : null;
					if (dataObj instanceof Map) {
						Map data = (Map) dataObj;
						Object shipmentsObj = data.get("shipments");
						Map shipment = null;
						if (shipmentsObj instanceof List && !((List) shipmentsObj).isEmpty()) {
							Object first = ((List) shipmentsObj).get(0);
							if (first instanceof Map) {
								shipment = (Map) first;
							}
						}
						if (shipment != null) {
							if (shipment.get("id") != null) {
								response.setShiprocketShipmentId(toInteger(shipment.get("id")));
							}
							if (shipment.get("awb") != null && !shipment.get("awb").toString().trim().isEmpty()) {
								response.setAwbCode(shipment.get("awb").toString().trim());
							}
							if (shipment.get("courier") != null) {
								response.setCourierName(shipment.get("courier").toString());
							}
							if (shipment.get("status") != null) {
								response.setShipmentStatus(shipment.get("status").toString());
							}
							Object ccIdObj = shipment.get("courier_company_id");
							if (ccIdObj == null)
								ccIdObj = shipment.get("courier_id");
							Integer ccId = toInteger(ccIdObj);
							if (ccId != null) {
								response.setCourierCompanyId(ccId);
							}
							// Shiprocket's order/show shipment object sometimes carries the
							// estimated delivery date directly — grab it here as an initial
							// value; it will be refreshed/overridden below from the live
							// track/awb API if that call succeeds and returns a value.
							Object estDeliveryObj = shipment.get("estimated_delivery_date");
							if (estDeliveryObj != null && !estDeliveryObj.toString().trim().isEmpty()) {
								response.setEstimatedDeliveryDate(estDeliveryObj.toString().trim());
							}
						}
					}
				}
				catch (Exception ex) {
					logger.warn(
							"getShiprocketPutPayloadByOrderNumber: could not fetch live order details from Shiprocket for orderNumber={}, shiprocketOrderId={} — {}",
							orderNumber, shiprocketOrderId, ex.getMessage());
				}
			}

			// ── Refresh label URL live from Shiprocket (courier/generate/label) ──
			// Shiprocket's generate-label call is idempotent: if a label already exists
			// for the shipment it simply returns the existing label_url instead of
			// creating a duplicate, so it's safe to call here to get the latest URL.
			if (response.getShiprocketShipmentId() != null) {
				try {
					Map labelResp = shiprocketService

						.generateLabel(List.of(response.getShiprocketShipmentId().toString()));
					String liveLabelUrl = extractLabelUrl(labelResp);
					if (liveLabelUrl != null && !liveLabelUrl.trim().isEmpty()) {
						response.setLabelUrl(liveLabelUrl.trim());
					}
				}
				catch (Exception ex) {
					logger.warn(
							"getShiprocketPutPayloadByOrderNumber: could not fetch live label URL from Shiprocket for orderNumber={}, shiprocketShipmentId={} — {}",
							orderNumber, response.getShiprocketShipmentId(), ex.getMessage());
				}
			}

			// ── Refresh AWB / courier tracking data live from Shiprocket (track/awb) ──
			if (response.getAwbCode() != null && !response.getAwbCode().trim().isEmpty()) {
				try {
					Map trackingData = shiprocketService.trackShipment(response.getAwbCode().trim());
					Object dataObj = trackingData != null ? trackingData.get("tracking_data") : null;
					if (dataObj instanceof Map) {
						Map trackingInfo = (Map) dataObj;
						if (trackingInfo.get("courier_name") != null) {
							response.setCourierName(trackingInfo.get("courier_name").toString());
						}
						// etd (top-level) → expectedDeliveryDate, matching the mapping used
						// elsewhere in this file (see processShiprocketOrderEvent step
						// TRACK_SHIPMENT). Falls back to a top-level "edd" key in case
						// Shiprocket returns a differently-shaped response.
						if (trackingInfo.get("etd") != null
								&& !trackingInfo.get("etd").toString().trim().isEmpty()) {
							response.setExpectedDeliveryDate(trackingInfo.get("etd").toString().trim());
						}
						else if (trackingInfo.get("edd") != null
								&& !trackingInfo.get("edd").toString().trim().isEmpty()) {
							response.setExpectedDeliveryDate(trackingInfo.get("edd").toString().trim());
						}
						if (trackingInfo.get("shipment_status") != null) {
							response.setShipmentStatus(trackingInfo.get("shipment_status").toString());
						}
						if (trackingInfo.get("track_url") != null
								&& !trackingInfo.get("track_url").toString().trim().isEmpty()) {
							response.setTrackUrl(trackingInfo.get("track_url").toString().trim());
						}

						// courier_company_id AND the estimated delivery date both live inside
						// shipment_track[0] per Shiprocket's track/awb response shape
						// (top-level courier_company_id is a fallback for other response
						// variants).
						Integer liveCcId = null;
						Object shipmentTrackObj = trackingInfo.get("shipment_track");
						if (shipmentTrackObj instanceof List && !((List) shipmentTrackObj).isEmpty()) {
							Object firstTrack = ((List) shipmentTrackObj).get(0);
							if (firstTrack instanceof Map) {
								Map firstTrackMap = (Map) firstTrack;
								liveCcId = toInteger(firstTrackMap.get("courier_company_id"));
								if (liveCcId == null && firstTrackMap.get("courier_name") != null
										&& response.getCourierName() == null) {
									response.setCourierName(firstTrackMap.get("courier_name").toString());
								}
								// edd (inside shipment_track[0]) → estimatedDeliveryDate
								Object eddObj = firstTrackMap.get("edd");
								if (eddObj != null && !eddObj.toString().trim().isEmpty()) {
									response.setEstimatedDeliveryDate(eddObj.toString().trim());
								}
							}
						}
						if (liveCcId == null) {
							liveCcId = toInteger(trackingInfo.get("courier_company_id"));
						}
						if (liveCcId != null) {
							response.setCourierCompanyId(liveCcId);
						}
					}
				}
				catch (Exception ex) {
					logger.warn(
							"getShiprocketPutPayloadByOrderNumber: could not fetch live AWB tracking from Shiprocket for orderNumber={}, awb={} — {}",
							orderNumber, response.getAwbCode(), ex.getMessage());
				}
			}

			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Fetched live Shiprocket shipment payload successfully.");
		}
		catch (Exception e) {
			logger.error("getShiprocketPutPayloadByOrderNumber: error for orderNumber={} — {}", orderNumber,
					e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(
					"An error occurred while fetching the Shiprocket shipment payload: " + e.getMessage());
		}
		return response;
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Retrigger shipping process (admin manual retry after a failure)
	// ──────────────────────────────────────────────────────────────────────────

	@Override
	public RetriggerShippingResponseDTO retriggerShippingProcess(String orderNumber) {
		logger.info("retriggerShippingProcess called for orderNumber={}", orderNumber);
		RetriggerShippingResponseDTO response = new RetriggerShippingResponseDTO();
		response.setOrderNumber(orderNumber);
		List<ShipmentRetriggerResultDTO> results = new ArrayList<>();
		response.setResults(results);

		if (orderNumber == null || orderNumber.isBlank()) {
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("orderNumber must not be null/blank");
			return response;
		}

		try {
			OrderEO order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
			if (order == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("No order found for orderNumber=" + orderNumber);
				return response;
			}
			response.setOrderId(order.getOrderId() != null ? order.getOrderId().longValue() : null);

			List<ShippingEO> shipments = shippingRepository.findByOrder(order);
			List<ShippingEO> retriableShipments = shipments == null ? Collections.emptyList()
					: shipments.stream()
						.filter(s -> Constants.SHIPMENT_TYPE_FORWARD.equals(s.getType()))
						.filter(s -> !Constants.SHIPMENT_STATUS_CANCELLED.equalsIgnoreCase(s.getShipmentStatus())
								&& !Constants.SHIPMENT_STATUS_DELIVERED.equalsIgnoreCase(s.getShipmentStatus()))
						.collect(Collectors.toList());

			if (retriableShipments.isEmpty()) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage(
						"No active FORWARD shipment found to retrigger for orderNumber=" + orderNumber
								+ " (shipment may not exist yet, or is already CANCELLED/DELIVERED)");
				return response;
			}

			int retriggeredCount = 0;
			int alreadyProcessedCount = 0;
			for (ShippingEO shippingEO : retriableShipments) {
				String previousStatus = shippingEO.getShipmentStatus();
				ShipmentRetriggerResultDTO.ShipmentRetriggerResultDTOBuilder resultBuilder = ShipmentRetriggerResultDTO
					.builder()
					.shipmentId(shippingEO.getShipmentId())
					.trackingNumber(shippingEO.getTrackingNumber())
					.previousStatus(previousStatus);

				try {
					// ── Already-processed guard: don't reprocess a shipment that has ──
					// ── already completed the full Shiprocket flow successfully.    ──
					if (isShipmentFullyProcessed(shippingEO)) {
						alreadyProcessedCount++;
						results.add(resultBuilder.currentStatus(previousStatus)
							.action("SKIPPED")
							.message("Shipment is already fully processed (AWB=" + shippingEO.getAwb()
									+ ", pickup scheduled, label & tracking generated); retrigger is not needed")
							.build());
						logger.info(
								"retriggerShippingProcess: shipmentId={} already fully processed, skipping",
								shippingEO.getShipmentId());
						continue;
					}

					// ── Cooldown guard: avoid re-hitting Shiprocket too soon ──
					Optional<ShiprocketOrderLogEO> lastLog = shiprocketOrderLogRepository
						.findFirstByShipmentIdOrderByCreatedAtDesc(shippingEO.getShipmentId());
					if (lastLog.isPresent() && lastLog.get().getCreatedAt() != null) {
						LocalDateTime cooldownUntil = lastLog.get()
							.getCreatedAt()
							.plusMinutes(Constants.RETRIGGER_SHIPPING_COOLDOWN_MINUTES);
						if (LocalDateTime.now().isBefore(cooldownUntil)) {
							results.add(resultBuilder.currentStatus(previousStatus)
								.action("SKIPPED")
								.message("Please wait before retrying; last attempt was at " + lastLog.get()
									.getCreatedAt()
									+ ". Try again after " + cooldownUntil)
								.build());
							logger.info(
									"retriggerShippingProcess: shipmentId={} still in cooldown until {}, skipping",
									shippingEO.getShipmentId(), cooldownUntil);
							continue;
						}
					}

					Long warehouseId = shippingEO.getWarehouse() != null ? shippingEO.getWarehouse().getWarehouseId()
							: null;
					ShiprocketOrderEvent event = ShiprocketOrderEvent.builder()
						.shipmentId(shippingEO.getShipmentId())
						.orderId(order.getOrderId() != null ? order.getOrderId().longValue() : null)
						.warehouseId(warehouseId)
						.build();

					saveStepLog(event, "RETRIGGER", "IN_PROGRESS", "Manually retriggered by admin from status="
							+ previousStatus + " on orderNumber=" + orderNumber);
					logger.info("retriggerShippingProcess: retriggering shipmentId={} (previousStatus={})",
							shippingEO.getShipmentId(), previousStatus);

					// processShiprocketOrderEvent internally skips CREATE_ORDER if a
					// Shiprocket order already exists for this shipment (see
					// idempotency guard in Step 1), so this safely resumes from the
					// courier-selection step onwards without duplicating the order. All
					// downstream steps (AWB, pickup, label, tracking) persist their own
					// updates to the shippingEO/ShiprocketOrderLog/tracking-history
					// tables as part of that call, so retriggering keeps every internal
					// table in sync for the shipment that actually got processed.
					this.processShiprocketOrderEvent(event);

					ShippingEO refreshed = shippingRepository.findById(shippingEO.getShipmentId()).orElse(shippingEO);
					String refreshedStatus = refreshed.getShipmentStatus();

					// ── Determine real outcome: processShiprocketOrderEvent swallows its
					// ── own internal step errors (it never rethrows), so a successful
					// ── return here does NOT necessarily mean the shipment actually
					// ── progressed. Check the resulting state and, if it still isn't
					// ── fully processed (and isn't the intentional
					// ── MANUAL_PROCESSING_REQUIRED stop-state), look up the most recent
					// ── FAILED step log for this shipment to surface a detailed reason.
					if (isShipmentFullyProcessed(refreshed)
							|| Constants.SHIPMENT_STATUS_MANUAL_PROCESSING_REQUIRED.equalsIgnoreCase(refreshedStatus)) {
						results.add(resultBuilder.currentStatus(refreshedStatus)
							.action("RETRIGGERED")
							.message(shippingEO.getShipOrderId() != null
									? "Resumed processing for existing Shiprocket order_id=" + shippingEO.getShipOrderId()
									: "Restarted Shiprocket order creation from scratch")
							.build());
						retriggeredCount++;
					}
					else {
						Optional<ShiprocketOrderLogEO> failedLog = shiprocketOrderLogRepository
							.findFirstByShipmentIdAndStatusOrderByCreatedAtDesc(shippingEO.getShipmentId(), "FAILED");
						String failedStep = failedLog.map(ShiprocketOrderLogEO::getStep).orElse("UNKNOWN");
						String reason = failedLog.map(ShiprocketOrderLogEO::getErrorMessage)
							.filter(m -> m != null && !m.isBlank())
							.orElse("Retrigger did not complete successfully and no detailed error was captured. "
									+ "Please check shiprocket_order_log for shipmentId=" + shippingEO.getShipmentId()
									+ " or contact support.");
						results.add(resultBuilder.currentStatus(refreshedStatus)
							.action("FAILED")
							.failedStep(failedStep)
							.failureReason(reason)
							.message("Retrigger attempt did not complete successfully at step '" + failedStep
									+ "'. Reason: " + reason)
							.build());
						logger.warn(
								"retriggerShippingProcess: shipmentId={} still not fully processed after retrigger; failedStep={}, reason={}",
								shippingEO.getShipmentId(), failedStep, reason);
					}
				}
				catch (Exception shipmentEx) {
					Throwable rootCause = shipmentEx;
					while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
						rootCause = rootCause.getCause();
					}
					String reason = rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.toString();
					logger.error("retriggerShippingProcess: error retriggering shipmentId={}: {}",
							shippingEO.getShipmentId(), reason, shipmentEx);
					results.add(resultBuilder.currentStatus(previousStatus)
						.action("FAILED")
						.failedStep("RETRIGGER")
						.failureReason(reason)
						.message("Unexpected error while retriggering: " + reason)
						.build());
				}
			}

			if (retriggeredCount > 0) {
				response.setResponseStatus(Constants.SUCCESS_STATUS);
				response.setResponseMessage(
						"Retriggered shipping process for " + retriggeredCount + " of " + retriableShipments.size()
								+ " shipment(s) under orderNumber=" + orderNumber
								+ (alreadyProcessedCount > 0
										? " (" + alreadyProcessedCount + " shipment(s) skipped, already processed)"
										: ""));
			}
			else if (alreadyProcessedCount == retriableShipments.size()) {
				response.setResponseStatus(Constants.SUCCESS_STATUS);
				response.setResponseMessage(
						"All " + alreadyProcessedCount + " shipment(s) under orderNumber=" + orderNumber
								+ " are already fully processed; nothing to retrigger");
			}
			else {
				// ── Aggregate the individual failure reasons into the top-level
				// ── message so the UI doesn't have to dig through `results` to show
				// ── the admin something actionable.
				String aggregatedReasons = results.stream()
					.filter(r -> "FAILED".equals(r.getAction()))
					.map(r -> "shipmentId=" + r.getShipmentId() + " [" + r.getFailedStep() + "]: "
							+ r.getFailureReason())
					.collect(Collectors.joining("; "));
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage(
						"No shipment could be retriggered for orderNumber=" + orderNumber
								+ (aggregatedReasons.isEmpty() ? " (see results for reasons)"
										: ". Reason(s): " + aggregatedReasons));
			}
		}
		catch (Exception e) {
			Throwable rootCause = e;
			while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
				rootCause = rootCause.getCause();
			}
			String reason = rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.toString();
			logger.error("retriggerShippingProcess: unexpected error for orderNumber={}: {}", orderNumber, reason, e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while retriggering the shipping process: " + reason);
		}
		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public FailedShiprocketOrdersResponseDTO getConfirmedOrReadyToShipOrdersWithFailedShiprocketStep() {
		FailedShiprocketOrdersResponseDTO response = new FailedShiprocketOrdersResponseDTO();
		logger.info(
				"getConfirmedOrReadyToShipOrdersWithFailedShiprocketStep called: fetching Confirmed/Ready to Ship orders with any failed Shiprocket step or no shipment.");
		try {
			// Step 1: fetch all orders with status "Confirmed"
			List<OrderEO> orders = new ArrayList<>(
					orderRepository.findByOrderStatus(Constants.ORDER_STATUS_CONFIRMED));

			// Step 2: fetch all orders with status "Ready to Ship" and append to the same list
			orders.addAll(orderRepository.findByOrderStatus(Constants.ORDER_STATUS_READY_TO_SHIP));

			List<OrderShipmentFailureDTO> results = new ArrayList<>();
			for (OrderEO order : orders) {
				// Step 3: fetch the respective shipment(s) for this order, preferring an
				// active FORWARD shipment when more than one shipment record exists.
				List<ShippingEO> shipments = shippingRepository.findByOrder(order);
				// Filter out cancelled shipments
				List<ShippingEO> nonCancelledShipments = shipments.stream()
					.filter(s -> !Constants.SHIPMENT_STATUS_CANCELLED.equals(s.getShipmentStatus()))
					.collect(Collectors.toList());
				ShippingEO shipping = nonCancelledShipments.stream()
					.filter(s -> Constants.SHIPMENT_TYPE_FORWARD.equals(s.getType()))
					.findFirst()
					.orElse(nonCancelledShipments.isEmpty() ? null : nonCancelledShipments.get(0));

				List<String> failedSteps = new ArrayList<>();
				if (shipping == null) {
					failedSteps.add("no_shipment");
				}
				else {
					if (Constants.FAILURE_STATUS.equals(shipping.getShiprocketOrderStatus())) {
						failedSteps.add("shiprocket_order_status");
					}
					if (Constants.FAILURE_STATUS.equals(shipping.getGenerateAwbStatus())) {
						failedSteps.add("generate_awb_status");
					}
					if (Constants.FAILURE_STATUS.equals(shipping.getRequestPickupStatus())) {
						failedSteps.add("request_pickup_status");
					}
					if (Constants.FAILURE_STATUS.equals(shipping.getGenerateLabelStatus())) {
						failedSteps.add("generate_label_status");
					}
					if (Constants.FAILURE_STATUS.equals(shipping.getTrackShipmentStatus())) {
						failedSteps.add("track_shipment_status");
					}
					if (Constants.FAILURE_STATUS.equals(shipping.getEstimateStatus())) {
						failedSteps.add("estimate_status");
					}
				}


				OrderShipmentFailureDTO.OrderDetails.OrderDetailsBuilder orderDetailsBuilder = OrderShipmentFailureDTO.OrderDetails
					.builder()
					.orderId(order.getOrderId() != null ? order.getOrderId().longValue() : null)
					.orderNumber(order.getOrderNumber())
					.orderStatus(order.getOrderStatus())
					.paymentStatus(order.getPaymentStatus())
					.totalAmount(order.getTotalAmount())
					.orderCreatedAt(order.getCreatedAt());
				if (order.getCustomer() != null) {
					CustomerEO customer = order.getCustomer();
					String fullName = (customer.getFirstName() != null ? customer.getFirstName() : "")
							+ (customer.getLastName() != null ? " " + customer.getLastName() : "");
					orderDetailsBuilder.customerName(fullName.trim())
						.customerEmail(customer.getEmail())
						.customerMobile(customer.getMobileNumber());
				}

			OrderShipmentFailureDTO.ShippingDetails shippingDetails = null;
			if (shipping != null) {
				// Fetch status history logs for each Shiprocket step
				List<ShiprocketOrderStatusHistoryEO> shiprocketOrderStatuslog =
					shiprocketOrderStatusHistoryRepository.findByShipmentOrderByCreatedAtAsc(shipping);
				List<GenerateAwbStatusHistoryEO> generateAwbStatuslog =
					generateAwbStatusHistoryRepository.findByShipmentOrderByCreatedAtAsc(shipping);
				List<RequestPickupStatusHistoryEO> requestPickupStatuslog =
					requestPickupStatusHistoryRepository.findByShipmentOrderByCreatedAtAsc(shipping);
				List<GenerateLabelStatusHistoryEO> generateLabelStatuslog =
					generateLabelStatusHistoryRepository.findByShipmentOrderByCreatedAtAsc(shipping);
				List<TrackShipmentStatusHistoryEO> trackShipmentStatuslog =
					trackShipmentStatusHistoryRepository.findByShipmentOrderByCreatedAtAsc(shipping);
				List<EstimateStatusHistoryEO> estimateStatuslog =
					estimateStatusHistoryRepository.findByShipmentOrderByCreatedAtAsc(shipping);

				// Convert entities to DTOs
				List<StatusHistoryLogDTO> shiprocketOrderStatuslogDTOs = shiprocketOrderStatuslog.stream()
					.map(this::convertToDTO)
					.collect(Collectors.toList());
				List<StatusHistoryLogDTO> generateAwbStatuslogDTOs = generateAwbStatuslog.stream()
					.map(this::convertToDTO)
					.collect(Collectors.toList());
				List<StatusHistoryLogDTO> requestPickupStatuslogDTOs = requestPickupStatuslog.stream()
					.map(this::convertToDTO)
					.collect(Collectors.toList());
				List<StatusHistoryLogDTO> generateLabelStatuslogDTOs = generateLabelStatuslog.stream()
					.map(this::convertToDTO)
					.collect(Collectors.toList());
				List<StatusHistoryLogDTO> trackShipmentStatuslogDTOs = trackShipmentStatuslog.stream()
					.map(this::convertToDTO)
					.collect(Collectors.toList());
				List<StatusHistoryLogDTO> estimateStatuslogDTOs = estimateStatuslog.stream()
					.map(this::convertToDTO)
					.collect(Collectors.toList());

				// Fetch shipment logs (complete audit trail from shiprocket_order_log)
				List<ShiprocketOrderLogEO> shipmentLogEOs = shiprocketOrderLogRepository
					.findByShipmentIdOrderByCreatedAtAsc(shipping.getShipmentId());
				List<ShipmentLogDTO> shipmentlogDTOs = shipmentLogEOs.stream()
					.map(this::convertToDTO)
					.collect(Collectors.toList());

				shippingDetails = OrderShipmentFailureDTO.ShippingDetails.builder()
					.shipmentId(shipping.getShipmentId())
					.trackingNumber(shipping.getTrackingNumber())
					.shipmentType(shipping.getType())
					.shipmentStatus(shipping.getShipmentStatus())
					.awb(shipping.getAwb())
					.courierName(shipping.getCourierName())
					.courierCompanyId(shipping.getCourierCompanyId())
					.shippingPrice(shipping.getShippingPrice())
					.shippedDate(shipping.getShippedDate())
					.deliveredDate(shipping.getDeliveredDate())
					.shipmentCreatedAt(shipping.getCreatedAt())
					.shipmentUpdatedAt(shipping.getUpdatedAt())
					.cartonId(shipping.getCarton() != null ? shipping.getCarton().getId() : null)
					.length(shipping.getLength())
					.breadth(shipping.getBreadth())
					.height(shipping.getHeight())
					.weight(shipping.getWeight())
					.labelUrl(shipping.getLabelUrl())
					.shipOrderId(shipping.getShipOrderId())
					.shipShipmentId(shipping.getShipShipmentId())
					.pickupId(shipping.getPickupId())
					.pickupToken(shipping.getPickupToken())
					.estimatedDeliveryDate(shipping.getEstimatedDeliveryDate())
					.expectedDeliveryDate(shipping.getExpectedDeliveryDate())
					.trackUrl(shipping.getTrackUrl())
					.shiprocketOrderStatus(shipping.getShiprocketOrderStatus())
					.generateAwbStatus(shipping.getGenerateAwbStatus())
					.requestPickupStatus(shipping.getRequestPickupStatus())
					.generateLabelStatus(shipping.getGenerateLabelStatus())
					.trackShipmentStatus(shipping.getTrackShipmentStatus())
					.estimateStatus(shipping.getEstimateStatus())
					.shiprocketOrderStatuslog(shiprocketOrderStatuslogDTOs)
					.generateAwbStatuslog(generateAwbStatuslogDTOs)
					.requestPickupStatuslog(requestPickupStatuslogDTOs)
					.generateLabelStatuslog(generateLabelStatuslogDTOs)
					.trackShipmentStatuslog(trackShipmentStatuslogDTOs)
					.estimateStatuslog(estimateStatuslogDTOs)
					.shipmentlogs(shipmentlogDTOs)
					.build();
			}

				results.add(OrderShipmentFailureDTO.builder()
					.orderDetails(orderDetailsBuilder.build())
					.shippingDetails(shippingDetails)
					.failedSteps(failedSteps)
					.build());
			}

			response.setOrders(results);
			response.setTotalCount(results.size());
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Fetched " + results.size()
					+ " Confirmed/Ready to Ship order(s) with at least one failed Shiprocket step or no shipment.");
			logger.info("getConfirmedOrReadyToShipOrdersWithFailedShiprocketStep fetched {} record(s)",
					results.size());
		}
		catch (Exception e) {
			logger.error("Error in getConfirmedOrReadyToShipOrdersWithFailedShiprocketStep: {}", e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(
					"An error occurred while fetching failed Shiprocket orders: " + e.getMessage());
			response.setOrders(new ArrayList<>());
			response.setTotalCount(0);
		}
		return response;
	}

	/**
	 * GET — Fetch the list of available courier services (excluding blocklisted
	 * couriers) for the given order id. Pickup postcode is resolved from the
	 * order's existing shipment's warehouse (falling back to the default
	 * warehouse), delivery postcode is resolved from the order's shipping
	 * address, and weight/dimensions are taken from the existing shipment record
	 * if one exists — the same inputs used internally by
	 * {@link #executeFindBestCourierStep(ShiprocketOrderEvent, ShiprocketEventContext)}
	 * during automated Shiprocket processing.
	 */
	@Override
	@Transactional(readOnly = true)
	public AvailableCourierServicesResponseDTO getAvailableCourierServicesByOrderId(Long orderId) {
		logger.info("getAvailableCourierServicesByOrderId called for orderId={}", orderId);
		if (orderId == null) {
			return AvailableCourierServicesResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("orderId must not be null")
				.totalCount(0)
				.build();
		}
		try {
			OrderEO order = orderRepository.findById(orderId).orElse(null);
			if (order == null) {
				return AvailableCourierServicesResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage("Order not found for orderId=" + orderId)
					.totalCount(0)
					.build();
			}

			OrderAddressEO orderAddress = orderAddressRepository.findByOrder(order).orElse(null);
			String deliveryPostcode = orderAddress != null ? orderAddress.getPostalCode() : null;
			if (deliveryPostcode == null || deliveryPostcode.trim().isEmpty()) {
				return AvailableCourierServicesResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage("No shipping address / delivery postcode found for orderId=" + orderId)
					.totalCount(0)
					.build();
			}

		// Prefer an active, non-cancelled FORWARD shipment (same preference used by
		// getConfirmedOrReadyToShipOrdersWithFailedShiprocketStep) so weight/dimensions
		// and pickup warehouse reflect the shipment actually being processed.
		List<ShippingEO> shipments = shippingRepository.findByOrder(order);
		// Filter out cancelled shipments first
		List<ShippingEO> nonCancelledShipments = shipments.stream()
			.filter(s -> !Constants.SHIPMENT_STATUS_CANCELLED.equals(s.getShipmentStatus()))
			.collect(Collectors.toList());
		ShippingEO shipping = nonCancelledShipments.stream()
			.filter(s -> Constants.SHIPMENT_TYPE_FORWARD.equals(s.getType()))
			.findFirst()
			.orElse(nonCancelledShipments.isEmpty() ? null : nonCancelledShipments.get(0));

			String warehouseName = (shipping != null && shipping.getWarehouse() != null)
					? shipping.getWarehouse().getWarehouseName() : null;
			String pickupPostcode = getWarehousePostalCode(warehouseName);
			if (pickupPostcode == null || pickupPostcode.trim().isEmpty()) {
				pickupPostcode = getWarehousePostalCode(Constants.DEFAULT_WAREHOUSE_NAME);
			}
			if (pickupPostcode == null || pickupPostcode.trim().isEmpty()) {
				return AvailableCourierServicesResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage("Unable to resolve a pickup postcode for orderId=" + orderId)
					.totalCount(0)
					.build();
			}

			// Weight: use the shipment's recorded weight (already stored in kg — see
			// finalizeShiprocketOrderRequest) if present, otherwise fall back to the
			// same 1.1 kg minimum used by executeFindBestCourierStep.
			Double weight = (shipping != null && shipping.getWeight() != null) ? shipping.getWeight() : 1.1;
			if (weight <= 1.1) {
				weight = 1.1;
			}

			ServiceabilityRequestDTO serviceabilityReq = ServiceabilityRequestDTO.builder()
				.orderId(shipping != null ? shipping.getShipOrderId() : null)
				.pickupPostcode(Integer.parseInt(pickupPostcode.trim()))
				.deliveryPostcode(Integer.parseInt(deliveryPostcode.trim()))
				.cod(order.getPaymentStatus() != null && order.getPaymentStatus().equalsIgnoreCase("PAID") ? 0 : 1)
				.weight(String.valueOf(weight))
				.length(shipping != null && shipping.getLength() != null && shipping.getLength() > 0
						? shipping.getLength().intValue() : null)
				.breadth(shipping != null && shipping.getBreadth() != null && shipping.getBreadth() > 0
						? shipping.getBreadth().intValue() : null)
				.height(shipping != null && shipping.getHeight() != null && shipping.getHeight() > 0
						? shipping.getHeight().intValue() : null)
				.build();

			List<CourierServiceDTO> couriers = shiprocketService
				.getAvailableCourierServicesExcludingBlocklisted(serviceabilityReq);

			logger.info("getAvailableCourierServicesByOrderId: found {} available courier(s) for orderId={}",
					couriers.size(), orderId);

			Integer currentlyUsedCourierId = shipping != null ? shipping.getCourierCompanyId() : null;

			return AvailableCourierServicesResponseDTO.builder()
				.responseStatus(Constants.SUCCESS_STATUS)
				.responseMessage("Fetched " + couriers.size() + " available courier service(s).")
				.totalCount(couriers.size())
				.currentlyUsedCourierId(currentlyUsedCourierId)
				.courierServices(couriers)
				.build();
		}
		catch (NumberFormatException nfe) {
			logger.warn("getAvailableCourierServicesByOrderId: invalid postcode for orderId={}: {}", orderId,
					nfe.getMessage());
			return AvailableCourierServicesResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("Invalid pickup/delivery postcode: " + nfe.getMessage())
				.totalCount(0)
				.build();
		}
		catch (Exception e) {
			logger.error("Error in getAvailableCourierServicesByOrderId for orderId={}: {}", orderId, e.getMessage(),
					e);
			return AvailableCourierServicesResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("An error occurred while fetching available courier services: " + e.getMessage())
				.totalCount(0)
				.build();
		}
	}

	/**
	 * A shipment is considered fully/successfully processed by Shiprocket once it has
	 * an AWB assigned, a pickup scheduled, a shipping label generated and a tracking
	 * URL captured. Once all of these are present there is nothing left for a retrigger
	 * to do, so it should be reported as SKIPPED rather than re-run (which would just
	 * repeat no-op API calls against Shiprocket, or worse, request a fresh pickup for an
	 * already-picked-up shipment).
	 */
	private boolean isShipmentFullyProcessed(ShippingEO shippingEO) {
		if (shippingEO == null) {
			return false;
		}
		return shippingEO.getAwb() != null && !shippingEO.getAwb().isBlank() && shippingEO.getLabelUrl() != null
				&& shippingEO.getPickupScheduledDate() != null && shippingEO.getTrackUrl() != null;
	}

	/** Best-effort conversion of a numeric-like Object (Integer/Long/Double/String) to Integer. */
	private Integer toInteger(Object value) {

		if (value == null)
			return null;
		if (value instanceof Integer)
			return (Integer) value;
		if (value instanceof Number)
			return ((Number) value).intValue();
		try {
			return Integer.parseInt(value.toString().trim());
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	/** Best-effort conversion of a numeric-like Object (Integer/Long/Double/String) to Double. */
	private Double toDouble(Object value) {
		if (value == null)
			return null;
		if (value instanceof Double)
			return (Double) value;
		if (value instanceof Number)
			return ((Number) value).doubleValue();
		try {
			return Double.parseDouble(value.toString().trim());
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	/**
	 * Extracts the list of order maps from the raw Shiprocket "search orders" response
	 * (GET /orders?search=...). Shiprocket's response shape can vary slightly across
	 * accounts/API versions, so this defensively looks for a List either directly under
	 * "data", nested one level deeper under "data" -> "data"/"orders", or under a
	 * top-level "orders" key — falling back to an empty list if nothing usable is found.
	 */
	private List<Map<String, Object>> extractOrderList(Map<String, Object> searchResult) {
		List<Map<String, Object>> orders = new ArrayList<>();
		if (searchResult == null)
			return orders;
		orders.addAll(extractMapsFromAnyShape(searchResult.get("data")));
		if (orders.isEmpty()) {
			orders.addAll(extractMapsFromAnyShape(searchResult.get("orders")));
		}
		return orders;
	}

	/** Pulls a List<Map> out of an Object that may be a List directly, or a Map wrapping one. */
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> extractMapsFromAnyShape(Object obj) {
		List<Map<String, Object>> result = new ArrayList<>();
		if (obj instanceof List) {
			for (Object item : (List<?>) obj) {
				if (item instanceof Map)
					result.add((Map<String, Object>) item);
			}
		}
		else if (obj instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) obj;
			Object nested = map.get("data");
			if (nested == null)
				nested = map.get("orders");
			if (nested instanceof List) {
				for (Object item : (List<?>) nested) {
					if (item instanceof Map)
						result.add((Map<String, Object>) item);
				}
			}
		}
		return result;
	}

	/**
	 * Best-effort population of the response DTO using the order summary object returned
	 * by Shiprocket's "search orders" API (GET /orders?search=...). Any shipment/AWB/
	 * courier/status/dimension fields set here are provisional and will be overwritten
	 * later by the live "order/show" and "track/awb" calls, when available.
	 */
	private void populateFromShiprocketOrderSummary(ShipmentPutPayloadResponseDTO response,
			Map<String, Object> matchedOrder) {
		if (matchedOrder == null)
			return;

		if (matchedOrder.get("status") != null) {
			response.setShipmentStatus(matchedOrder.get("status").toString());
		}
		Double length = toDouble(matchedOrder.get("length"));
		if (length != null)
			response.setLength(length);
		Double breadth = toDouble(matchedOrder.get("breadth"));
		if (breadth != null)
			response.setBreadth(breadth);
		Double height = toDouble(matchedOrder.get("height"));
		if (height != null)
			response.setHeight(height);
		Double weight = toDouble(matchedOrder.get("weight"));
		if (weight != null)
			response.setWeight(weight);

		Object shipmentsObj = matchedOrder.get("shipments");
		Map<?, ?> shipment = null;
		if (shipmentsObj instanceof List && !((List<?>) shipmentsObj).isEmpty()) {
			Object first = ((List<?>) shipmentsObj).get(0);
			if (first instanceof Map)
				shipment = (Map<?, ?>) first;
		}
		if (shipment != null) {
			if (shipment.get("id") != null)
				response.setShiprocketShipmentId(toInteger(shipment.get("id")));
			if (shipment.get("awb") != null && !shipment.get("awb").toString().trim().isEmpty())
				response.setAwbCode(shipment.get("awb").toString().trim());
			if (shipment.get("courier") != null)
				response.setCourierName(shipment.get("courier").toString());
			if (shipment.get("status") != null)
				response.setShipmentStatus(shipment.get("status").toString());
		}
	}

	// ─── Private helpers shared by create / update ────────────────────────────

	/** Applies all non-null fields from the request onto the given ShippingEO. */
	private void applyShippingOrderRequest(ShippingEO eo, ShippingOrderRequestDTO req) {
		if (req.getShiprocketOrderId() != null)
			eo.setShipOrderId(req.getShiprocketOrderId());
		if (req.getShiprocketShipmentId() != null)
			eo.setShipShipmentId(req.getShiprocketShipmentId());
		if (req.getAwbCode() != null && !req.getAwbCode().trim().isEmpty())
			eo.setAwb(req.getAwbCode().trim());
		if (req.getCourierName() != null && !req.getCourierName().trim().isEmpty())
			eo.setCourierName(req.getCourierName().trim());
		if (req.getCourierCompanyId() != null)
			eo.setCourierCompanyId(req.getCourierCompanyId());
		if (req.getShipmentStatus() != null && !req.getShipmentStatus().trim().isEmpty())
			eo.setShipmentStatus(req.getShipmentStatus().trim());
		if (req.getShipmentType() != null && !req.getShipmentType().trim().isEmpty())
			eo.setType(req.getShipmentType().trim());
		if (req.getTrackingNumber() != null && !req.getTrackingNumber().trim().isEmpty())
			eo.setTrackingNumber(req.getTrackingNumber().trim());
		if (req.getLength() != null)
			eo.setLength(req.getLength());
		if (req.getBreadth() != null)
			eo.setBreadth(req.getBreadth());
		if (req.getHeight() != null)
			eo.setHeight(req.getHeight());
		if (req.getWeight() != null)
			eo.setWeight(req.getWeight());
		if (req.getShippingPrice() != null)
			eo.setShippingPrice(req.getShippingPrice());
		if (req.getLabelUrl() != null && !req.getLabelUrl().trim().isEmpty())
			eo.setLabelUrl(req.getLabelUrl().trim());
		if (req.getTrackUrl() != null && !req.getTrackUrl().trim().isEmpty())
			eo.setTrackUrl(req.getTrackUrl().trim());
		if (req.getPickupId() != null)
			eo.setPickupId(req.getPickupId());
		if (req.getPickupToken() != null && !req.getPickupToken().trim().isEmpty())
			eo.setPickupToken(req.getPickupToken().trim());
		if (req.getPickupScheduledDate() != null && !req.getPickupScheduledDate().trim().isEmpty()) {
			LocalDateTime d = parseDateFlexible(req.getPickupScheduledDate().trim());
			if (d != null)
				eo.setPickupScheduledDate(d);
		}
		if (req.getEstimatedDeliveryDate() != null && !req.getEstimatedDeliveryDate().trim().isEmpty()) {
			LocalDateTime d = parseDateFlexible(req.getEstimatedDeliveryDate().trim());
			if (d != null)
				eo.setEstimatedDeliveryDate(d);
		}
		if (req.getExpectedDeliveryDate() != null && !req.getExpectedDeliveryDate().trim().isEmpty()) {
			LocalDateTime d = parseDateFlexible(req.getExpectedDeliveryDate().trim());
			if (d != null)
				eo.setExpectedDeliveryDate(d);
		}
		if (req.getShippedDate() != null && !req.getShippedDate().trim().isEmpty()) {
			LocalDateTime d = parseDateFlexible(req.getShippedDate().trim());
			if (d != null)
				eo.setShippedDate(d);
		}
		if (req.getDeliveredDate() != null && !req.getDeliveredDate().trim().isEmpty()) {
			LocalDateTime d = parseDateFlexible(req.getDeliveredDate().trim());
			if (d != null)
				eo.setDeliveredDate(d);
		}
	}

	/**
	 * Keeps the {@code courier_selection_log} rows for a shipment in sync with the
	 * shipment's <b>current</b> {@code courierCompanyId} after a manual create/update
	 * via {@link #updateShippingByOrderNumber} or {@link #createShippingByOrderNumber}.
	 * <p>
	 * Without this, the "Courier Options" list shown in the admin Shipments popup keeps
	 * flagging whichever courier was selected at the time of the original AWB
	 * assignment as "Selected" — even after an admin manually changes the shipment's
	 * courier via the PUT endpoint — because {@link #applyShippingOrderRequest} only
	 * updates the {@code shipping} table, never the {@code courier_selection_log}
	 * table.
	 * <p>
	 * This marks the row matching the shipment's current courierCompanyId as selected
	 * (refreshing its AWB/shipping price too) and un-marks every other row for the same
	 * shipment so at most one candidate is ever flagged as selected. If the current
	 * courier isn't among the logged candidates at all (e.g. a manual/non-integrated
	 * courier not returned by Shiprocket's recommend-courier API), a new log row is
	 * inserted so it still shows up — correctly marked as selected — in the UI.
	 */
	private void syncCourierSelectionLogSelection(ShippingEO saved) {
		if (saved == null || saved.getShipmentId() == null || saved.getCourierCompanyId() == null) {
			return;
		}
		try {
			Integer currentCourierCompanyId = saved.getCourierCompanyId();
			List<CourierSelectionLogEO> entries = courierSelectionLogRepository
				.findByShipmentIdOrderByRankAsc(saved.getShipmentId());

			boolean matched = false;
			for (CourierSelectionLogEO entry : entries) {
				boolean isCurrent = currentCourierCompanyId.equals(entry.getCourierCompanyId());
				if (isCurrent) {
					matched = true;
					entry.setIsSelected(true);
					if (saved.getAwb() != null && !saved.getAwb().trim().isEmpty()) {
						entry.setAwbCode(saved.getAwb().trim());
					}
					if (saved.getShippingPrice() != null) {
						entry.setShippingPrice(saved.getShippingPrice());
					}
				}
				else if (Boolean.TRUE.equals(entry.getIsSelected())) {
					entry.setIsSelected(false);
				}
				else {
					continue;
				}
				courierSelectionLogRepository.save(entry);
			}

			if (!matched) {
				CourierSelectionLogEO newEntry = CourierSelectionLogEO.builder()
					.orderId(saved.getOrder() != null && saved.getOrder().getOrderId() != null
							? saved.getOrder().getOrderId().longValue() : null)
					.orderNumber(saved.getOrder() != null ? saved.getOrder().getOrderNumber() : null)
					.shipmentId(saved.getShipmentId())
					.shipShipmentId(saved.getShipShipmentId())
					.courierCompanyId(currentCourierCompanyId)
					.courierName(saved.getCourierName())
					.rank(entries.size() + 1)
					.isSelected(true)
					.awbCode(saved.getAwb())
					.shippingPrice(saved.getShippingPrice())
					.build();
				courierSelectionLogRepository.save(newEntry);
			}
		}
		catch (Exception e) {
			logger.warn("syncCourierSelectionLogSelection: could not sync courier_selection_log for shipmentId={}: {}",
					saved.getShipmentId(), e.getMessage());
		}
	}

	/**
	 * Inserts a tracking history row for this shipment.
	 * <p>
	 * If {@code historyStatus} is explicitly provided in the request, that value is
	 * used (as before). Otherwise, this falls back to auto-recording the shipment's
	 * <b>current</b> status ({@code saved.getShipmentStatus()}) as a history entry —
	 * but only if a record with that exact status doesn't already exist for this
	 * shipment, so the Track Order history list doesn't show duplicate entries and
	 * the current status is never silently missing from the history table.
	 */
	private boolean saveTrackingHistoryIfRequested(ShippingEO saved, ShippingOrderRequestDTO req) {
		String statusToRecord;
		String location;
		String remarks;
		if (req.getHistoryStatus() != null && !req.getHistoryStatus().trim().isEmpty()) {
			statusToRecord = req.getHistoryStatus().trim();
			location = req.getHistoryLocation();
			remarks = req.getHistoryRemarks() != null ? req.getHistoryRemarks() : "Manual action by admin.";
		}
		else if (saved.getShipmentStatus() != null && !saved.getShipmentStatus().trim().isEmpty()) {
			statusToRecord = saved.getShipmentStatus().trim();
			location = null;
			remarks = "Auto-recorded: current shipment status.";
		}
		else {
			return false;
		}

		// Idempotency guard — don't insert a duplicate row if this status is already
		// present in the history table for this shipment.
		boolean alreadyRecorded = shipmentTrackingHistoryRepository.existsByShipmentAndStatusIgnoreCase(saved,
				statusToRecord);
		if (alreadyRecorded) {
			logger.info(
					"saveTrackingHistoryIfRequested: shipmentId={} already has a history record with status='{}'. Skipping duplicate insert.",
					saved.getShipmentId(), statusToRecord);
			return false;
		}

		ShipmentTrackingHistoryEO history = ShipmentTrackingHistoryEO.builder()
			.shipment(saved)
			.status(statusToRecord)
			.location(location)
			.remarks(remarks)
			.updatedAt(LocalDateTime.now())
			.build();
		shipmentTrackingHistoryRepository.save(history);
		return true;
	}

	/**
	 * Keeps the parent {@link OrderEO#getOrderStatus()} in sync with the shipment's
	 * current status after a manual create/update, mirroring the mapping used by the
	 * Shiprocket-webhook-driven {@link #shipmentStatusUpdate} flow. Only writes to the
	 * order if its status actually differs from the derived value, so we don't perform
	 * unnecessary DB writes when the order is already up to date.
	 */
	private void syncOrderStatusWithShipment(ShippingEO saved) {
		try {
			if (saved == null || saved.getOrder() == null || saved.getShipmentStatus() == null
					|| saved.getShipmentStatus().trim().isEmpty()) {
				return;
			}
			OrderEO order = saved.getOrder();
			String shipmentStatus = saved.getShipmentStatus().trim();
			String derivedOrderStatus = Constants.SHIPMENT_STATUS_DELIVERED.equalsIgnoreCase(shipmentStatus)
					? Constants.ORDER_STATUS_DELIVERED : shipmentStatus;

			if (!derivedOrderStatus.equalsIgnoreCase(order.getOrderStatus())) {
				logger.info("syncOrderStatusWithShipment: updating orderId={} status from '{}' to '{}'",
						order.getOrderId(), order.getOrderStatus(), derivedOrderStatus);
				order.setOrderStatus(derivedOrderStatus);
				orderRepository.save(order);
			}
			else {
				logger.debug("syncOrderStatusWithShipment: orderId={} status already '{}'. No update needed.",
						order.getOrderId(), derivedOrderStatus);
			}
		}
		catch (Exception ex) {
			logger.warn("syncOrderStatusWithShipment: failed to sync order status for shipmentId={} — {}",
					saved != null ? saved.getShipmentId() : null, ex.getMessage());
		}
	}

	/** Writes an entry to shiprocket_order_log for audit purposes. */
	private void logShiprocketOrderLog(ShippingEO saved, String step, String notes) {
		try {
			Long logOrderId = saved.getOrder() != null && saved.getOrder().getOrderId() != null
					? saved.getOrder().getOrderId().longValue() : null;
			Long logWarehouseId = saved.getWarehouse() != null ? saved.getWarehouse().getWarehouseId() : null;
			ShiprocketOrderLogEO logEntry = ShiprocketOrderLogEO.builder()
				.shipmentId(saved.getShipmentId())
				.orderId(logOrderId)
				.warehouseId(logWarehouseId)
				.step(step)
				.status("MANUAL_SUCCESS")
				.shiprocketOrderId(saved.getShipOrderId())
				.shiprocketShipmentId(saved.getShipShipmentId())
				.awbCode(saved.getAwb())
				.labelUrl(saved.getLabelUrl())
				.errorMessage(notes)
				.build();
			shiprocketOrderLogRepository.save(logEntry);
		}
		catch (Exception ex) {
			logger.warn("logShiprocketOrderLog: failed to write audit log — {}", ex.getMessage());
		}
	}

	/** Populates a ManualShiprocketUpdateResponseDTO from a saved ShippingEO. */
	private void buildManualUpdateResponse(ManualShiprocketUpdateResponseDTO response, ShippingEO saved,
			boolean historyCreated, String step) {
		response.setResponseStatus(Constants.SUCCESS_STATUS);
		response.setShipmentId(saved.getShipmentId());
		response.setOrderNumber(saved.getOrder() != null ? saved.getOrder().getOrderNumber() : null);
		response.setShipmentStatus(saved.getShipmentStatus());
		response.setShiprocketOrderId(saved.getShipOrderId());
		response.setShiprocketShipmentId(saved.getShipShipmentId());
		response.setAwbCode(saved.getAwb());
		response.setCourierName(saved.getCourierName());
		response.setCourierCompanyId(saved.getCourierCompanyId());
		response.setLabelUrl(saved.getLabelUrl());
		response.setTrackUrl(saved.getTrackUrl());
		response.setShippingPrice(saved.getShippingPrice());
		response.setUpdatedAt(saved.getUpdatedAt());
		response.setHistoryEntryCreated(historyCreated);
		response.setStepLogged(step);
	}

	// ─── Helper: build courier candidate DTO list from courier_selection_log ─────

	/**
	 * Convert a ShiprocketOrderStatusHistoryEO entity to a StatusHistoryLogDTO.
	 */
	private StatusHistoryLogDTO convertToDTO(ShiprocketOrderStatusHistoryEO entity) {
		if (entity == null) return null;
		return StatusHistoryLogDTO.builder()
			.id(entity.getId())
			.status(entity.getStatus())
			.remarks(entity.getRemarks())
			.createdAt(entity.getCreatedAt())
			.build();
	}

	/**
	 * Convert a GenerateAwbStatusHistoryEO entity to a StatusHistoryLogDTO.
	 */
	private StatusHistoryLogDTO convertToDTO(GenerateAwbStatusHistoryEO entity) {
		if (entity == null) return null;
		return StatusHistoryLogDTO.builder()
			.id(entity.getId())
			.status(entity.getStatus())
			.remarks(entity.getRemarks())
			.createdAt(entity.getCreatedAt())
			.build();
	}

	/**
	 * Convert a RequestPickupStatusHistoryEO entity to a StatusHistoryLogDTO.
	 */
	private StatusHistoryLogDTO convertToDTO(RequestPickupStatusHistoryEO entity) {
		if (entity == null) return null;
		return StatusHistoryLogDTO.builder()
			.id(entity.getId())
			.status(entity.getStatus())
			.remarks(entity.getRemarks())
			.createdAt(entity.getCreatedAt())
			.build();
	}

	/**
	 * Convert a GenerateLabelStatusHistoryEO entity to a StatusHistoryLogDTO.
	 */
	private StatusHistoryLogDTO convertToDTO(GenerateLabelStatusHistoryEO entity) {
		if (entity == null) return null;
		return StatusHistoryLogDTO.builder()
			.id(entity.getId())
			.status(entity.getStatus())
			.remarks(entity.getRemarks())
			.createdAt(entity.getCreatedAt())
			.build();
	}

	/**
	 * Convert a TrackShipmentStatusHistoryEO entity to a StatusHistoryLogDTO.
	 */
	private StatusHistoryLogDTO convertToDTO(TrackShipmentStatusHistoryEO entity) {
		if (entity == null) return null;
		return StatusHistoryLogDTO.builder()
			.id(entity.getId())
			.status(entity.getStatus())
			.remarks(entity.getRemarks())
			.createdAt(entity.getCreatedAt())
			.build();
	}

	/**
	 * Convert an EstimateStatusHistoryEO entity to a StatusHistoryLogDTO.
	 */
	private StatusHistoryLogDTO convertToDTO(EstimateStatusHistoryEO entity) {
		if (entity == null) return null;
		return StatusHistoryLogDTO.builder()
			.id(entity.getId())
			.status(entity.getStatus())
			.remarks(entity.getRemarks())
			.createdAt(entity.getCreatedAt())
			.build();
	}

	/**
	 * Convert a ShiprocketOrderLogEO entity to a ShipmentLogDTO.
	 */
	private ShipmentLogDTO convertToDTO(ShiprocketOrderLogEO entity) {
		if (entity == null) return null;
		return ShipmentLogDTO.builder()
			.id(entity.getId())
			.shipmentId(entity.getShipmentId())
			.orderId(entity.getOrderId())
			.warehouseId(entity.getWarehouseId())
			.step(entity.getStep())
			.status(entity.getStatus())
			.shiprocketOrderId(entity.getShiprocketOrderId())
			.shiprocketShipmentId(entity.getShiprocketShipmentId())
			.awbCode(entity.getAwbCode())
			.labelUrl(entity.getLabelUrl())
			.errorMessage(entity.getErrorMessage())
			.createdAt(entity.getCreatedAt())
			.updatedAt(entity.getUpdatedAt())
			.build();
	}

	/**
	 * Loads all {@code CourierSelectionLogEO} rows for the given internal shipment ID and
	 * maps them to {@link CourierSelectionLogDTO} objects sorted by rank.
	 */
	private List<CourierSelectionLogDTO> buildCourierCandidateDTOs(Long shipmentId) {
		if (shipmentId == null)
			return java.util.Collections.emptyList();
		List<CourierSelectionLogEO> rows = courierSelectionLogRepository.findByShipmentIdOrderByRankAsc(shipmentId);
		List<CourierSelectionLogDTO> dtos = new ArrayList<>();
		for (CourierSelectionLogEO row : rows) {
			CourierSelectionLogDTO dto = CourierSelectionLogDTO.builder()
				.id(row.getId())
				.courierCompanyId(row.getCourierCompanyId())
				.courierName(row.getCourierName())
				.rate(row.getRate())
				.estimatedDeliveryDays(row.getEstimatedDeliveryDays())
				.rank(row.getRank())
				.isSelected(false)
				.awbCode(row.getAwbCode())
				.shippingPrice(row.getShippingPrice())
				.createdAt(row.getCreatedAt())
				.build();
			dtos.add(dto);
		}
		return dtos;
	}

	// ──────────────────────────────────────────────────────────────────────────
	// Order ID-based Shipping CRUD APIs
	// ──────────────────────────────────────────────────────────────────────────

	/**
	 * GET: Fetch all shipping records for a given orderId.
	 * Called by: GET /api/order/{orderId}/shipping
	 */
	public ShippingEntityResponseDTO getShippingsByOrderId(Long orderId) {
		ShippingEntityResponseDTO response = new ShippingEntityResponseDTO();
		logger.info("getShippingsByOrderId called for orderId={}", orderId);
		try {
			if (orderId == null || orderId <= 0) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Order ID must be a valid positive number");
				return response;
			}

			// Verify order exists
			OrderEO order = orderRepository.findById(orderId).orElse(null);
			if (order == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Order not found with orderId=" + orderId);
				return response;
			}

			// Fetch all shipping records for this order
			List<ShippingEO> shippingList = shippingRepository.findByOrderId(orderId);
			
			List<ShippingDetailDTO> shippingDTOs = new ArrayList<>();
			for (ShippingEO shipping : shippingList) {
				ShippingDetailDTO dto = convertShippingToDTO(shipping);
				shippingDTOs.add(dto);
			}

			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Shipping records fetched successfully. Total: " + shippingDTOs.size());
			response.setData(shippingDTOs);
			response.setCount(shippingDTOs.size());
			logger.info("getShippingsByOrderId: fetched {} shipping records for orderId={}", shippingDTOs.size(), orderId);
		}
		catch (Exception e) {
			logger.error("Error in getShippingsByOrderId for orderId={}: {}", orderId, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while fetching shipping records: " + e.getMessage());
		}
		return response;
	}

	/**
	 * POST: Create or update a shipping record for a given orderId.
	 * If a shipping record already exists for the order, it will be updated.
	 * Otherwise, a new record will be created.
	 * Called by: POST /api/order/{orderId}/shipping
	 */
	public ShippingEntityResponseDTO createShippingForOrder(Long orderId, CreateShippingRequestDTO request) {
		ShippingEntityResponseDTO response = new ShippingEntityResponseDTO();
		logger.info("createShippingForOrder called for orderId={}", orderId);
		try {
			// Validate orderId
			if (orderId == null || orderId <= 0) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Order ID must be a valid positive number");
				return response;
			}

			// Validate request
			if (request == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Request body must not be null");
				return response;
			}

			// Verify order exists
			OrderEO order = orderRepository.findById(orderId).orElse(null);
			if (order == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Order not found with orderId=" + orderId);
				return response;
			}

			// Validate carton ID
			if (request.getCartonId() == null || request.getCartonId() <= 0) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Carton ID is required and must be a valid positive number");
				return response;
			}

			// Verify carton exists
			CartonEO carton = cartonRepository.findById(request.getCartonId()).orElse(null);
			if (carton == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Carton not found with cartonId=" + request.getCartonId());
				return response;
			}

			// Check and update order status if provided and different
			String currentOrderStatus = order.getOrderStatus();
			String inputOrderStatus = request.getOrderStatus();
			boolean orderStatusChanged = false;

			if (inputOrderStatus != null && !inputOrderStatus.trim().isEmpty()
					&& !inputOrderStatus.equals(currentOrderStatus)) {
				// Order status has changed
				logger.info("Order status changed for orderId={} from '{}' to '{}'", orderId, currentOrderStatus, inputOrderStatus);
				order.setOrderStatus(inputOrderStatus);
				orderRepository.save(order);
				orderStatusChanged = true;
				logger.info("Order status updated successfully for orderId={}", orderId);
			}

			// Check if a shipping record already exists for this order
			java.util.Optional<ShippingEO> existingShipping = shippingRepository.findFirstByOrderId(orderId);
			ShippingEO shipping;
			boolean isUpdate = false;

			if (existingShipping.isPresent()) {
				// Update existing record
				shipping = existingShipping.get();
				isUpdate = true;
				logger.info("Found existing shipping record for orderId={}, shipmentId={}, updating...", orderId, shipping.getShipmentId());
			} else {
				// Create new record
				shipping = new ShippingEO();
				shipping.setOrder(order);
				logger.info("Creating new shipping record for orderId={}", orderId);
			}

			// Generate tracking number if not provided
			// Format: TRK_{orderNumber}_{sequenceNumber}
			// Sequence number is based on count of all shipments (including cancelled)
			if (request.getTrackingNumber() == null || request.getTrackingNumber().trim().isEmpty()) {
				long shipmentCount = shippingRepository.countByOrderId(orderId);
				long sequenceNumber = shipmentCount + 1;
				String orderNumber = order.getOrderNumber();
				String generatedTrackingNumber = "TRK_" + orderNumber + "_" + sequenceNumber;

				// Verify tracking number is unique (check across all shipments including cancelled)
				ShippingEO existingWithTrackingNumber = shippingRepository.findByTrackingNumber(generatedTrackingNumber);
				if (existingWithTrackingNumber != null) {
					// Tracking number already exists, find the next available one
					long attemptNumber = sequenceNumber;
					while (existingWithTrackingNumber != null && attemptNumber < sequenceNumber + 1000) {
						attemptNumber++;
						generatedTrackingNumber = "TRK_" + orderNumber + "_" + attemptNumber;
						existingWithTrackingNumber = shippingRepository.findByTrackingNumber(generatedTrackingNumber);
					}
					if (attemptNumber >= sequenceNumber + 1000) {
						response.setResponseStatus(Constants.FAILURE_STATUS);
						response.setResponseMessage("Unable to generate unique tracking number for orderId=" + orderId);
						return response;
					}
					logger.warn("Generated tracking number already existed. Using alternative: {}", generatedTrackingNumber);
				}
				logger.info("Generated tracking number for orderId={}: {}", orderId, generatedTrackingNumber);
				shipping.setTrackingNumber(generatedTrackingNumber);
			} else {
				// Use provided tracking number - validate it's unique
				String providedTrackingNumber = request.getTrackingNumber().trim();
				ShippingEO existingWithTrackingNumber = shippingRepository.findByTrackingNumber(providedTrackingNumber);

				// If updating existing shipment, allow using the same tracking number
				if (existingWithTrackingNumber != null && !isUpdate) {
					response.setResponseStatus(Constants.FAILURE_STATUS);
					response.setResponseMessage("Tracking number already exists: " + providedTrackingNumber);
					return response;
				}
				// Allow update if it's the same shipment
				if (existingWithTrackingNumber != null && isUpdate && !existingWithTrackingNumber.getShipmentId().equals(shipping.getShipmentId())) {
					response.setResponseStatus(Constants.FAILURE_STATUS);
					response.setResponseMessage("Tracking number already exists: " + providedTrackingNumber);
					return response;
				}
				shipping.setTrackingNumber(providedTrackingNumber);
			}

			// ...existing code...
			shipping.setCarton(carton);
			shipping.setCourierName(request.getCourierName());
			shipping.setType(request.getType());
			shipping.setShipmentStatus(request.getShipmentStatus());
			shipping.setShippedDate(request.getShippedDate());
			shipping.setDeliveredDate(request.getDeliveredDate());
			shipping.setLength(request.getLength());
			shipping.setBreadth(request.getBreadth());
			shipping.setHeight(request.getHeight());
			shipping.setWeight(request.getWeight());
			shipping.setAwb(request.getAwb());
			shipping.setLabelUrl(request.getLabelUrl());
			shipping.setShipOrderId(request.getShipOrderId());
			shipping.setShipShipmentId(request.getShipShipmentId());
			shipping.setPickupId(request.getPickupId());
			shipping.setPickupScheduledDate(request.getPickupScheduledDate());
			shipping.setPickupToken(request.getPickupToken());
			shipping.setCourierCompanyId(request.getCourierCompanyId());
			shipping.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
			shipping.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
			shipping.setTrackUrl(request.getTrackUrl());
			shipping.setShippingPrice(request.getShippingPrice());
			shipping.setShiprocketOrderStatus(request.getShiprocketOrderStatus());
			shipping.setGenerateAwbStatus(request.getGenerateAwbStatus());
			shipping.setRequestPickupStatus(request.getRequestPickupStatus());
			shipping.setGenerateLabelStatus(request.getGenerateLabelStatus());
			shipping.setTrackShipmentStatus(request.getTrackShipmentStatus());
			shipping.setEstimateStatus(request.getEstimateStatus());
			
			if (request.getWarehouseId() != null) {
				WarehouseEO warehouse = warehouseRepository.findById(request.getWarehouseId()).orElse(null);
				if (warehouse != null) {
					shipping.setWarehouse(warehouse);
				}
			}

			// Save shipping record
			ShippingEO savedShipping = shippingRepository.save(shipping);
			String message = isUpdate ? "Shipping record updated successfully" : "Shipping record created successfully";
			logger.info("Shipping record {} for orderId={}, shipmentId={}", isUpdate ? "updated" : "created", orderId, savedShipping.getShipmentId());

			// Create shipment tracking history record if order status changed
			if (orderStatusChanged && inputOrderStatus != null) {
				try {
					ShipmentTrackingHistoryEO trackingHistory = ShipmentTrackingHistoryEO.builder()
						.shipment(savedShipping)
						.status(inputOrderStatus)
						.location(request.getType()) // Using type as location (FORWARD/RETURN_PICKUP)
						.remarks("Order status updated: " + currentOrderStatus + " → " + inputOrderStatus)
						.build();
					shipmentTrackingHistoryRepository.save(trackingHistory);
					logger.info("Tracking history record created for shipmentId={} with status='{}'",
						savedShipping.getShipmentId(), inputOrderStatus);
				} catch (Exception e) {
					logger.warn("Failed to create tracking history record for shipmentId={}: {}",
						savedShipping.getShipmentId(), e.getMessage());
					// Log warning but continue - don't fail the entire operation
				}
			}

			// Convert to DTO and set in response
			ShippingDetailDTO shippingDTO = convertShippingToDTO(savedShipping);
			response.setShipping(shippingDTO);
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage(message);
		}
		catch (Exception e) {
			logger.error("Error in createShippingForOrder for orderId={}: {}", orderId, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while creating/updating shipping record: " + e.getMessage());
		}
		return response;
	}

	/**
	 * Convert a ShippingEO entity to a ShippingDetailDTO.
	 */
	private ShippingDetailDTO convertShippingToDTO(ShippingEO shipping) {
		if (shipping == null) return null;

		ShippingDetailDTO dto = ShippingDetailDTO.builder()
			.shipmentId(shipping.getShipmentId())
			.trackingNumber(shipping.getTrackingNumber())
			.courierName(shipping.getCourierName())
			.type(shipping.getType())
			.shipmentStatus(shipping.getShipmentStatus())
			.shippedDate(shipping.getShippedDate())
			.deliveredDate(shipping.getDeliveredDate())
			.length(shipping.getLength())
			.breadth(shipping.getBreadth())
			.height(shipping.getHeight())
			.weight(shipping.getWeight())
			.awb(shipping.getAwb())
			.labelUrl(shipping.getLabelUrl())
			.shipOrderId(shipping.getShipOrderId())
			.shipShipmentId(shipping.getShipShipmentId())
			.pickupId(shipping.getPickupId())
			.pickupScheduledDate(shipping.getPickupScheduledDate())
			.pickupToken(shipping.getPickupToken())
			.courierCompanyId(shipping.getCourierCompanyId())
			.estimatedDeliveryDate(shipping.getEstimatedDeliveryDate())
			.expectedDeliveryDate(shipping.getExpectedDeliveryDate())
			.trackUrl(shipping.getTrackUrl())
			.shippingPrice(shipping.getShippingPrice())
			.shiprocketOrderStatus(shipping.getShiprocketOrderStatus())
			.generateAwbStatus(shipping.getGenerateAwbStatus())
			.requestPickupStatus(shipping.getRequestPickupStatus())
			.generateLabelStatus(shipping.getGenerateLabelStatus())
			.trackShipmentStatus(shipping.getTrackShipmentStatus())
			.estimateStatus(shipping.getEstimateStatus())
			.createdAt(shipping.getCreatedAt())
			.updatedAt(shipping.getUpdatedAt())
			.build();

		if (shipping.getOrder() != null) {
			Integer orderId = shipping.getOrder().getOrderId();
			dto.setOrderId(orderId != null ? orderId.longValue() : null);
			dto.setOrderNumber(shipping.getOrder().getOrderNumber());
		}

		if (shipping.getCarton() != null) {
			dto.setCartonId(shipping.getCarton().getId());
			dto.setCartonNo(shipping.getCarton().getName());
		}

		if (shipping.getWarehouse() != null) {
			dto.setWarehouseId(shipping.getWarehouse().getWarehouseId());
		}

		return dto;
	}

	/** 
	 * POST — Save a shipping record with minimal information.
	 * Used by POST /api/shipping endpoint when no order number/ID is provided in path.
	 */
	@Override
	@Transactional
	public ManualShiprocketUpdateResponseDTO saveShipping(ShippingOrderRequestDTO request) {
		ManualShiprocketUpdateResponseDTO response = new ManualShiprocketUpdateResponseDTO();
		try {
			if (request == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Request body must not be null");
				return response;
			}

			// Resolve warehouse if warehouseId supplied
			WarehouseEO warehouse = null;
			if (request.getWarehouseId() != null) {
				warehouse = warehouseRepository.findById(request.getWarehouseId()).orElse(null);
				if (warehouse == null) {
					logger.warn("saveShipping: warehouseId={} not found, creating record without warehouse.",
							request.getWarehouseId());
				}
			}

			// Create ShippingEO without an order (order can be null for generic shipment records)
			ShippingEO eo = ShippingEO.builder()
				.warehouse(warehouse)
				.type(request.getShipmentType() != null ? request.getShipmentType().trim()
						: Constants.SHIPMENT_TYPE_FORWARD)
				.shipmentStatus(request.getShipmentStatus() != null ? request.getShipmentStatus().trim()
						: Constants.SHIPMENT_STATUS_CREATED)
				.build();

			applyShippingOrderRequest(eo, request);
			ShippingEO saved = shippingRepository.save(eo);

			boolean historyCreated = saveTrackingHistoryIfRequested(saved, request);
			syncCourierSelectionLogSelection(saved);
			logShiprocketOrderLog(saved, "MANUAL_SAVE", request.getNotes());

			buildManualUpdateResponse(response, saved, historyCreated, "MANUAL_SAVE");
			response.setResponseMessage("Shipping record saved successfully.");
		}
		catch (Exception e) {
			logger.error("saveShipping: error — {}", e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while saving shipping record: " + e.getMessage());
		}
		return response;
	}

	/**
	 * GET — Fetch shipping details by shipment ID, including tracking history.
	 */
	@Override
	@Transactional(readOnly = true)
	public ShippingDetailResponseDTO getShippingByShipmentId(Long shipmentId) {
		ShippingDetailResponseDTO response = new ShippingDetailResponseDTO();
		try {
			if (shipmentId == null || shipmentId <= 0) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Shipment ID must be a valid positive number.");
				return response;
			}

			ShippingEO eo = shippingRepository.findById(shipmentId).orElse(null);
			if (eo == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("No shipping record found for shipment ID: " + shipmentId);
				return response;
			}

			// Tracking history (chronological)
			List<ShipmentTrackingHistoryEO> historyEOs = shipmentTrackingHistoryRepository
				.findByShipmentOrderByUpdatedAtAsc(eo);
			List<ShipTrackHistoryDTO> history = historyEOs.stream()
				.map(h -> ShipTrackHistoryDTO.builder()
					.status(h.getStatus())
					.location(h.getLocation())
					.remarks(h.getRemarks())
					.date(h.getUpdatedAt())
					.build())
				.collect(Collectors.toList());

			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Shipping details fetched successfully.");
			response.setShipmentId(eo.getShipmentId());
			response.setOrderNumber(eo.getOrder() != null ? eo.getOrder().getOrderNumber() : null);
			response.setOrderId(eo.getOrder() != null && eo.getOrder().getOrderId() != null
					? eo.getOrder().getOrderId().longValue() : null);
			response.setShiprocketOrderId(eo.getShipOrderId());
			response.setShiprocketShipmentId(eo.getShipShipmentId());
			response.setAwbCode(eo.getAwb());
			response.setCourierName(eo.getCourierName());
			response.setCourierCompanyId(eo.getCourierCompanyId());
			response.setShipmentStatus(eo.getShipmentStatus());
			response.setShipmentType(eo.getType());
			response.setTrackingNumber(eo.getTrackingNumber());
			response.setLength(eo.getLength());
			response.setBreadth(eo.getBreadth());
			response.setHeight(eo.getHeight());
			response.setWeight(eo.getWeight());
			response.setShippingPrice(eo.getShippingPrice());
			response.setLabelUrl(eo.getLabelUrl());
			response.setTrackUrl(eo.getTrackUrl());
			response.setPickupId(eo.getPickupId());
			response.setPickupToken(eo.getPickupToken());
			response.setPickupScheduledDate(eo.getPickupScheduledDate());
			response.setEstimatedDeliveryDate(eo.getEstimatedDeliveryDate());
			response.setExpectedDeliveryDate(eo.getExpectedDeliveryDate());
			response.setShippedDate(eo.getShippedDate());
			response.setDeliveredDate(eo.getDeliveredDate());
			response.setCreatedAt(eo.getCreatedAt());
			response.setUpdatedAt(eo.getUpdatedAt());
			if (eo.getWarehouse() != null) {
				response.setWarehouseId(eo.getWarehouse().getWarehouseId());
				response.setWarehouseName(eo.getWarehouse().getWarehouseName());
			}
			if (eo.getCarton() != null) {
				response.setCartonId(eo.getCarton().getId());
				response.setCartonNo(eo.getCarton().getName());
			}
			response.setTrackingHistory(history);
		}
		catch (Exception e) {
			logger.error("getShippingByShipmentId: error for shipmentId={} — {}", shipmentId, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("An error occurred while fetching shipping details: " + e.getMessage());
		}
		return response;
	}

	/**
	 * Creates an initial shipment record after successful payment.
	 * This method:
	 * 1. Creates a ShippingEO record with status "Pending"
	 * 2. Creates a ShipmentTrackingHistoryEO record with status "Order Confirmed"
	 * 
	 * Used by OrderServiceImpl.updateOrderPaymentStatus after payment is marked as PAID.
	 */
	public void createInitialShipmentAfterPayment(OrderEO order) {
		try {
			if (order == null || order.getOrderId() == null) {
				logger.warn("createInitialShipmentAfterPayment: order is null or missing orderId");
				return;
			}

			logger.info("createInitialShipmentAfterPayment: creating initial shipment for orderId={}, orderNumber={}",
					order.getOrderId(), order.getOrderNumber());

			// Check if shipment already exists for this order
			ShippingEO existingShipment = shippingRepository.findFirstByOrderId(order.getOrderId().longValue()).orElse(null);
			if (existingShipment != null) {
				logger.info("createInitialShipmentAfterPayment: shipment already exists for orderId={}",
						order.getOrderId());
				return;
			}

			// Generate unique tracking number
			// Format: TRK_{orderNumber}_{sequenceNumber}
			// Sequence number is based on count of all shipments (including cancelled)
			long shipmentCount = shippingRepository.countByOrderId(order.getOrderId().longValue());
			long sequenceNumber = shipmentCount + 1;
			String orderNumber = order.getOrderNumber();
			String generatedTrackingNumber = "TRK_" + orderNumber + "_" + sequenceNumber;

			// Verify tracking number is unique (check across all shipments including cancelled)
			ShippingEO existingWithTrackingNumber = shippingRepository.findByTrackingNumber(generatedTrackingNumber);
			if (existingWithTrackingNumber != null) {
				// Tracking number already exists, find the next available one
				long attemptNumber = sequenceNumber;
				while (existingWithTrackingNumber != null && attemptNumber < sequenceNumber + 1000) {
					attemptNumber++;
					generatedTrackingNumber = "TRK_" + orderNumber + "_" + attemptNumber;
					existingWithTrackingNumber = shippingRepository.findByTrackingNumber(generatedTrackingNumber);
				}
				if (attemptNumber >= sequenceNumber + 1000) {
					logger.error("createInitialShipmentAfterPayment: Unable to generate unique tracking number for orderId={}",
							order.getOrderId());
					return;
				}
				logger.warn("createInitialShipmentAfterPayment: Generated tracking number already existed. Using alternative: {}",
						generatedTrackingNumber);
			}
		logger.info("createInitialShipmentAfterPayment: Generated tracking number for orderId={}: {}",
				order.getOrderId(), generatedTrackingNumber);

		// Get default warehouse for address
		WarehouseEO warehouse = warehouseRepository
				.findByWarehouseNameIgnoreCaseAndStatus(Constants.DEFAULT_WAREHOUSE_NAME, Constants.STATUS_ACTIVE)
				.orElse(null);
		String warehouseLocation = "";
		if (warehouse != null) {
			warehouseLocation = (warehouse.getAddressLine1() != null ? warehouse.getAddressLine1() : "")
					+ " " + (warehouse.getCity() != null ? warehouse.getCity() : "")
					+ " " + (warehouse.getState() != null ? warehouse.getState() : "")
					+ " " + (warehouse.getPostalCode() != null ? warehouse.getPostalCode() : "");
			warehouseLocation = warehouseLocation.trim();
		}
		if (warehouseLocation.isEmpty()) {
			warehouseLocation = Constants.DEFAULT_WAREHOUSE_NAME;
		}

		// Create initial ShippingEO record
		ShippingEO shipment = new ShippingEO();
		shipment.setOrder(order);
		shipment.setTrackingNumber(generatedTrackingNumber); // Set the generated tracking number
		shipment.setShipmentStatus(Constants.SHIPMENT_STATUS_INITIALIZED); // Pending status
		shipment.setType(Constants.SHIPMENT_TYPE_FORWARD);
		shipment.setCreatedAt(LocalDateTime.now());
		shipment.setUpdatedAt(LocalDateTime.now());

		ShippingEO savedShipment = shippingRepository.save(shipment);
		logger.info("createInitialShipmentAfterPayment: shipment created with shipmentId={} for orderId={}",
				savedShipment.getShipmentId(), order.getOrderId());

		// Create ShipmentTrackingHistoryEO record
		ShipmentTrackingHistoryEO trackingHistory = new ShipmentTrackingHistoryEO();
		trackingHistory.setShipment(savedShipment);
		trackingHistory.setStatus(Constants.SHIPMENT_ORDER_STATUS_CREATED); // "Order Confirmed"
		trackingHistory.setLocation(warehouseLocation);
		trackingHistory.setRemarks(Constants.SHIPMENT_ORDER_STATUS_CREATED_REMARK); // "Order Submitted shipment will be created."
		trackingHistory.setUpdatedAt(LocalDateTime.now());

		shipmentTrackingHistoryRepository.save(trackingHistory);
		logger.info("createInitialShipmentAfterPayment: tracking history created for shipmentId={}",
				savedShipment.getShipmentId());

		} catch (Exception e) {
			logger.error("createInitialShipmentAfterPayment: error creating shipment for orderId={} — {}",
					order != null ? order.getOrderId() : "null", e.getMessage(), e);
			// Don't throw exception - this is a best-effort operation and shouldn't fail payment confirmation
		}
	}

}
