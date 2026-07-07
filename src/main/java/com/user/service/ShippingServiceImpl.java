package com.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.communication.event.EmailDetails;
import com.user.communication.event.Event;
import com.user.communication.event.OrderEvent;
import com.user.communication.event.RefundInitiatedEvent;
import com.user.communication.event.ShiprocketOrderEvent;
import com.user.communication.service.NotificationService;
import com.user.dto.*;
import com.user.model.*;
import com.user.repository.*;

import com.user.utility.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
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
	private CourierSelectionLogRepository courierSelectionLogRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

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
	@Async("shipmentTaskExecutor")
	public void processCreateShipmentEvent(OrderEvent shippingDTO) {
		if (shippingDTO == null || shippingDTO.getOrderId() == null
				|| !Constants.ORDER_EVENT_TYPE_SHIPPED.equals(shippingDTO.getEventType())) {
			return;
		}
		// Fetch entities by ID to avoid LazyInitializationException
		OrderEO order = null;

		if (shippingDTO.getOrderId() != null) {
			order = orderRepository.findById(shippingDTO.getOrderId()).orElse(null);

		}
		// 1. create Shipment Entity Note one shipment for one warehouse.
		if (order != null) {
			try {
				// Idempotency guard: skip if a FORWARD shipment already exists for this
				// order
				List<ShippingEO> existingShipments = shippingRepository.findByOrder(order);
				boolean forwardShipmentExists = existingShipments != null && existingShipments.stream()
					.anyMatch(s -> Constants.SHIPMENT_TYPE_FORWARD.equals(s.getType())
							&& !Constants.SHIPMENT_STATUS_CANCELLED.equals(s.getShipmentStatus()));
				if (forwardShipmentExists) {
					logger.warn(
							"processCreateShipmentEvent: active FORWARD shipment already exists for orderId={}, skipping duplicate creation",
							order.getOrderId());
					return;
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
					return;
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
					ShipmentTrackingHistoryEO shipmentTrackingHistoryEO = new ShipmentTrackingHistoryEO();
					shipmentTrackingHistoryEO.setShipment(savedShippingEO);
					shipmentTrackingHistoryEO.setStatus(Constants.SHIPMENT_ORDER_STATUS_CREATED);
					shipmentTrackingHistoryEO.setLocation(warehouseEO.getAddressLine1() + ", "
							+ warehouseEO.getAddressLine2() + "," + warehouseEO.getCity() + ", "
							+ warehouseEO.getState() + " - " + warehouseEO.getPostalCode());
					shipmentTrackingHistoryEO.setRemarks(Constants.SHIPMENT_ORDER_STATUS_CREATED_REMARK);
					shipmentTrackingHistoryRepository.save(shipmentTrackingHistoryEO);

					// Save ShipmentItemEO records so the Kafka listener can fetch them
					for (OrderItemEO item : itemsForWarehouse) {
						ShipmentItemEO shipmentItemEO = new ShipmentItemEO();
						shipmentItemEO.setShipment(savedShippingEO);
						shipmentItemEO.setOrderItem(item);
						shipmentItemEO.setQuantity(item.getQuantity());
						shippingItemRepository.save(shipmentItemEO);
					}

					// Publish Kafka event to trigger Shiprocket order creation
					ShiprocketOrderEvent shiprocketEvent = ShiprocketOrderEvent.builder()
						.shipmentId(savedShippingEO.getShipmentId() != null
								? savedShippingEO.getShipmentId().longValue() : null)
						.orderId(order.getOrderId() != null ? order.getOrderId().longValue() : null)
						.warehouseId(warehouseEO.getWarehouseId())
						.build();
					// Directly trigger Shiprocket order creation
					this.processShiprocketOrderEvent(shiprocketEvent);
					logger.info("Triggered Shiprocket order creation for shipmentId={}, orderId={}, warehouseId={}",
							shiprocketEvent.getShipmentId(), shiprocketEvent.getOrderId(),
							shiprocketEvent.getWarehouseId());
				}
			}
			catch (Exception e) {
				logger.error("processCreateShipmentEvent: error creating shipment for orderId={}: {}",
						order.getOrderId(), e.getMessage(), e);
			}

		}
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

	@Override
	public void processShiprocketOrderEvent(ShiprocketOrderEvent event) {
		if (event == null || event.getShipmentId() == null) {
			logger.warn("processShiprocketOrderEvent: null or incomplete event received");
			return;
		}
		logger.info("Processing ShiprocketOrderEvent for shipmentId={}, orderId={}", event.getShipmentId(),
				event.getOrderId());

		// ── Pre-flight: save an IN_PROGRESS marker so the event is always traceable ──
		saveStepLog(event, "CREATE_ORDER", "IN_PROGRESS", null);

		try {
			ShippingEO shippingEO = shippingRepository.findById(event.getShipmentId()).orElse(null);
			if (shippingEO == null) {
				saveStepLog(event, "CREATE_ORDER", "FAILED",
						"ShippingEO not found for shipmentId=" + event.getShipmentId());
				logger.error("processShiprocketOrderEvent: ShippingEO not found for shipmentId={}",
						event.getShipmentId());
				return;
			}
			OrderEO order = orderRepository.findById(event.getOrderId()).orElse(null);
			if (order == null) {
				saveStepLog(event, "CREATE_ORDER", "FAILED", "OrderEO not found for orderId=" + event.getOrderId());
				logger.error("processShiprocketOrderEvent: OrderEO not found for orderId={}", event.getOrderId());
				return;
			}

			// Fetch shipment items for this shipment
			List<ShipmentItemEO> shipmentItems = shippingItemRepository.findByShipment(shippingEO);
			List<OrderItemEO> itemsForWarehouse = new ArrayList<>();
			for (ShipmentItemEO si : shipmentItems) {
				if (si.getOrderItem() != null) {
					itemsForWarehouse.add(si.getOrderItem());
				}
			}

			OrderAddressEO orderAddress = orderAddressRepository.findByOrder(order).orElse(null);

			// Resolve warehouse name and channel ID from the inventory-associated
			// warehouse for this shipment
			String shipmentWarehouseName = null;
			String shipmentChannelId = null;
			if (event.getWarehouseId() != null) {
				WarehouseEO shipmentWarehouse = warehouseRepository.findById(event.getWarehouseId()).orElse(null);
				if (shipmentWarehouse != null) {
					shipmentWarehouseName = shipmentWarehouse.getWarehouseName();
					shipmentChannelId = shipmentWarehouse.getChannelId();
				}
			}

			Map<String, Object> shiprocketOrderRequest = new HashMap<>();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-M-yyyy");
			String orderDate = LocalDate.now().format(formatter);
			shiprocketOrderRequest.put("order_id", order.getOrderNumber());
			shiprocketOrderRequest.put("order_date", orderDate);
			shiprocketOrderRequest.put("pickup_location",
					shipmentWarehouseName != null ? shipmentWarehouseName : "warehouse");
			shiprocketOrderRequest.put("channel_id", shipmentChannelId != null ? shipmentChannelId : "10576563");
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
			double weight = 0.0;
			for (OrderItemEO item : itemsForWarehouse) {
				Map<String, Object> itemMap = new HashMap<>();
				ProductVariantEO variant = item.getProductVar();
				if (variant != null) {
					weight += variant.getWeight();
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

			CartonEO selectedCarton = cartonSelectionService.selectCarton(itemsForWarehouse);
			shiprocketOrderRequest.put("order_items", orderItemsList);
			shiprocketOrderRequest.put("payment_method",
					order.getPaymentStatus() != null && order.getPaymentStatus().equalsIgnoreCase("PAID") ? "Prepaid"
							: "COD");
			shiprocketOrderRequest.put("sub_total", order.getTotalAmount());
			shiprocketOrderRequest.put("length", selectedCarton.getLength());
			shiprocketOrderRequest.put("breadth", selectedCarton.getBreadth());
			shiprocketOrderRequest.put("height", selectedCarton.getHeight());
			shiprocketOrderRequest.put("weight", (selectedCarton.getEmptyWeight() + weight) / 1000.0);
			shippingEO.setLength(selectedCarton.getLength());
			shippingEO.setBreadth(selectedCarton.getBreadth());
			shippingEO.setHeight(selectedCarton.getHeight());
			shippingEO.setWeight((selectedCarton.getEmptyWeight() + weight) / 1000.0);

			// Step 1: Create Order on Shiprocket
			Integer shipOrderId = null;
			Integer shipmentId = null;
			try {
				Map response = shiprocketService.createOrder(shiprocketOrderRequest);
				if (response != null) {
					shipOrderId = (Integer) response.get("order_id");
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
						.status("SUCCESS")
						.shiprocketOrderId(shipOrderId)
						.shiprocketShipmentId(shipmentId)
						.build());
					logger.info("Step CREATE_ORDER SUCCESS: order_id={}, shipment_id={}", shipOrderId, shipmentId);
				}
				else {
					saveStepLog(event, "CREATE_ORDER", "FAILED", "Shiprocket createOrder returned null response");
					logger.error("Step CREATE_ORDER FAILED: null response from Shiprocket");
					return;
				}
			}
			catch (Exception ex) {
				saveStepLog(event, "CREATE_ORDER", "FAILED", ex.getMessage());
				logger.error("Step CREATE_ORDER FAILED for shipmentId={}: {}", event.getShipmentId(), ex.getMessage(),
						ex);
				return;
			}

			// Step 1.5: Find Top-N Best Courier Services via Serviceability API
			List<Integer> bestCourierIds = new ArrayList<>();
			Map<Integer, Double> courierRateMap = new HashMap<>();
			List<Map<String, Object>> courierDetailsList = new ArrayList<>();
			try {
				String deliveryPostcode = orderAddress != null ? orderAddress.getPostalCode() : null;
				if (deliveryPostcode != null && !deliveryPostcode.isEmpty() && shipmentId != null) {
					Double weighttemp1 = shippingEO.getWeight() != null ? shippingEO.getWeight() : 1.1;
					if (weighttemp1 <= 1.1) {
						weighttemp1 = 1.1;
					}

					String weighttemp = shippingEO.getWeight() != null ? String.valueOf(shippingEO.getWeight()) : "1.0";
					ServiceabilityRequestDTO serviceabilityReq = ServiceabilityRequestDTO.builder()
						.orderId(shipOrderId)
						.pickupPostcode(Integer.parseInt(getWarehousePostalCode(shipmentWarehouseName)))
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
					String fcStatus = bestCourierIds.isEmpty() ? "NOT_FOUND" : "SUCCESS";
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

			// Step 2: Generate AWB — try best couriers in order; fall back to auto-assign
			// if list is empty
			String awbCode = null;
			int usedCourierIndex = -1;
			try {
				if (shipmentId == null) {
					saveStepLog(event, "GENERATE_AWB", "FAILED",
							"Cannot generate AWB without shipment_id from Shiprocket");
					logger.error("Step GENERATE_AWB FAILED: shipment_id is null, cannot proceed");
					return;
				}

				// Determine couriers to attempt (use ranked list, or null for
				// auto-assign)
				List<Integer> couriersToTry = bestCourierIds.isEmpty() ? java.util.Collections.singletonList(null)
						: bestCourierIds;

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
							usedCourierIndex = ci;

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
					saveStepLog(event, "GENERATE_AWB", "FAILED", "AWB code not received from Shiprocket after trying "
							+ couriersToTry.size() + " courier(s)");
					logger.error("Step GENERATE_AWB FAILED for shipmentId={} after trying {} couriers",
							event.getShipmentId(), couriersToTry.size());
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
						.status("SUCCESS")
						.awbCode(awbCode)
						.build();
					shiprocketOrderLogRepository.save(awbLog);
				}
			}
			catch (Exception ex) {
				saveStepLog(event, "GENERATE_AWB", "FAILED", ex.getMessage());
				logger.error("Step GENERATE_AWB FAILED for shipmentId={}: {}", event.getShipmentId(), ex.getMessage(),
						ex);
			}

			// Step 3: Request Pickup
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
				shippingEO = shippingRepository.save(shippingEO);

				// ── Separate log record for REQUEST_PICKUP success ──
				String pickupNote = alreadyInQueue ? "Already in Pickup Queue – treated as PICKUP_SCHEDULED" : null;
				saveStepLog(event, "REQUEST_PICKUP", "SUCCESS", pickupNote);

				// ── Save PICKUP_SCHEDULED entry in shipment_tracking_history ──
				ShipmentTrackingHistoryEO pickupHistory = new ShipmentTrackingHistoryEO();
				pickupHistory.setShipment(shippingEO);
				pickupHistory.setStatus("PICKUP_SCHEDULED");
				pickupHistory.setRemarks(alreadyInQueue ? "Pickup already in queue - Pickup Scheduled"
						: "Pickup requested successfully");
				pickupHistory.setUpdatedAt(LocalDateTime.now());
				shipmentTrackingHistoryRepository.save(pickupHistory);

				logger.info("Step REQUEST_PICKUP SUCCESS for shipmentId={}, alreadyInQueue={}", event.getShipmentId(),
						alreadyInQueue);
			}
			catch (Exception ex) {
				saveStepLog(event, "REQUEST_PICKUP", "FAILED", ex.getMessage());
				logger.error("Step REQUEST_PICKUP FAILED for shipmentId={}: {}", event.getShipmentId(), ex.getMessage(),
						ex);
			}

			// Step 4: Generate Label — if label fails, transfer to next best courier and
			// retry once
			String generatedLabelUrl = null;
			try {
				List<String> shipmentIdStrings = new ArrayList<>();
				shipmentIdStrings.add(shipmentId.toString());
				Map response4 = shiprocketService.generateLabel(shipmentIdStrings);
				generatedLabelUrl = extractLabelUrl(response4);
				logger.info("Step GENERATE_LABEL raw response keys={}", response4 != null ? response4.keySet() : null);

				// If label generation failed and AWB was assigned, try transferring to
				// the next best courier
				if (generatedLabelUrl == null && usedCourierIndex >= 0) {
					for (int ci = usedCourierIndex + 1; ci < bestCourierIds.size() && generatedLabelUrl == null; ci++) {
						Integer nextCourierId = bestCourierIds.get(ci);
						logger.info(
								"Step GENERATE_LABEL: label failed, attempting transfer to courierCompanyId={} (candidate #{})",
								nextCourierId, ci + 1);
						try {
							// Transfer shipment to next best courier — separate log
							// record
							shiprocketService.transferCourier(shipmentId, nextCourierId);
							saveStepLog(event, "TRANSFER_COURIER", "SUCCESS",
									"Transferred shipmentId=" + shipmentId + " to courierCompanyId=" + nextCourierId);
							logger.info(
									"Step TRANSFER_COURIER SUCCESS: shipmentId={} transferred to courierCompanyId={}",
									shipmentId, nextCourierId);

							// Update courier details in ShippingEO
							shippingEO.setCourierCompanyId(nextCourierId);
							shippingRepository.save(shippingEO);

							// Re-request pickup with the new courier — separate log
							// record
							try {
								Map pickupResp = shiprocketService.requestPickup(shipmentId.toString());
								boolean rePickupAlreadyInQueue = pickupResp != null
										&& Boolean.TRUE.equals(pickupResp.get("already_in_pickup_queue"));
								if (pickupResp != null && !rePickupAlreadyInQueue) {
									Map pickupData = null;
									Object rObj = pickupResp.get("response");
									pickupData = (rObj instanceof Map) ? (Map) rObj : pickupResp;
									Object pidObj = pickupData.get("pickup_id");
									if (pidObj instanceof Number)
										shippingEO.setPickupId(((Number) pidObj).longValue());
									Object sdObj = pickupData.get("pickup_scheduled_date");
									if (sdObj instanceof String) {
										try {
											shippingEO.setPickupScheduledDate(LocalDateTime.parse((String) sdObj,
													DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
										}
										catch (Exception ignored) {
										}
									}
									Object tokObj = pickupData.get("pickup_token_number");
									if (tokObj instanceof String)
										shippingEO.setPickupToken((String) tokObj);
								}
								shippingEO.setShipmentStatus("PICKUP_SCHEDULED");
								shippingEO = shippingRepository.save(shippingEO);
								// Save tracking history for re-pickup after transfer
								ShipmentTrackingHistoryEO rePickupHistory = new ShipmentTrackingHistoryEO();
								rePickupHistory.setShipment(shippingEO);
								rePickupHistory.setStatus("PICKUP_SCHEDULED");
								rePickupHistory.setRemarks(rePickupAlreadyInQueue
										? "Pickup already in queue after courier transfer - Pickup Scheduled"
										: "Pickup requested after courier transfer");
								rePickupHistory.setUpdatedAt(LocalDateTime.now());
								shipmentTrackingHistoryRepository.save(rePickupHistory);
								saveStepLog(event, "REQUEST_PICKUP_AFTER_TRANSFER", "SUCCESS",
										"Re-pickup after transfer to courierCompanyId=" + nextCourierId
												+ (rePickupAlreadyInQueue ? " (already in queue)" : ""));
								logger.info(
										"Step REQUEST_PICKUP (after transfer) SUCCESS for courierCompanyId={}, alreadyInQueue={}",
										nextCourierId, rePickupAlreadyInQueue);
							}
							catch (Exception pickupEx) {
								saveStepLog(event, "REQUEST_PICKUP_AFTER_TRANSFER", "FAILED",
										"courierCompanyId=" + nextCourierId + ". Error: " + pickupEx.getMessage());
								logger.warn("Step REQUEST_PICKUP (after transfer) FAILED for courierCompanyId={}: {}",
										nextCourierId, pickupEx.getMessage());
							}

							// Retry label generation
							Map retryLabelResp = shiprocketService.generateLabel(shipmentIdStrings);
							generatedLabelUrl = extractLabelUrl(retryLabelResp);
							if (generatedLabelUrl != null) {
								logger.info(
										"Step GENERATE_LABEL SUCCESS after transfer to courierCompanyId={}: labelUrl={}",
										nextCourierId, generatedLabelUrl);
								usedCourierIndex = ci;
							}
							else {
								logger.warn("Step GENERATE_LABEL still failed after transfer to courierCompanyId={}",
										nextCourierId);
							}
						}
						catch (Exception transferEx) {
							saveStepLog(event, "TRANSFER_COURIER", "FAILED",
									"courierCompanyId=" + nextCourierId + ". Error: " + transferEx.getMessage());
							logger.warn("Step TRANSFER_COURIER FAILED for courierCompanyId={}: {}", nextCourierId,
									transferEx.getMessage());
						}
					}
				}

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
						.status("SUCCESS")
						.labelUrl(generatedLabelUrl)
						.build();
					shiprocketOrderLogRepository.save(labelLog);
				}
				shippingEO.setLabelUrl(generatedLabelUrl);
				shippingRepository.save(shippingEO);
				logger.info("Step GENERATE_LABEL {}: labelUrl={}", generatedLabelUrl != null ? "SUCCESS" : "FAILED",
						generatedLabelUrl);
			}
			catch (Exception ex) {
				saveStepLog(event, "GENERATE_LABEL", "FAILED", ex.getMessage());
				logger.error("Step GENERATE_LABEL FAILED for shipmentId={}: {}", event.getShipmentId(), ex.getMessage(),
						ex);
			}

			// Step 5: Track Shipment — populate track_url, etd, edd
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
							saveStepLog(event, "TRACK_SHIPMENT", "SUCCESS", null);
							logger.info("Step TRACK_SHIPMENT SUCCESS for awb={}: trackUrl={}, etd={}, edd={}",
									awbForTracking, shippingEO.getTrackUrl(), shippingEO.getExpectedDeliveryDate(),
									shippingEO.getEstimatedDeliveryDate());

							// ── Send Order Status Update email now that tracking is
							// available ──
							try {
								OrderEO emailOrder = shippingEO.getOrder();
								if (emailOrder != null && emailOrder.getCustomer() != null) {
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
										.emailMessage(String.format("Hi %s, your order %s is now %s. Track: %s",
												customerName, emailOrder.getOrderNumber(), emailOrder.getOrderStatus(),
												shippingEO.getTrackUrl()))
										.smsSubject("Order Status Update")
										.smsMessage(String.format("Order #%s is now %s. Track: %s",
												emailOrder.getOrderNumber(), emailOrder.getOrderStatus(),
												shippingEO.getAwb()))
										.channel(Constants.COMMUNICATION_CHANNEL_BOTH)
										.emailDetails(emailDetails)
										.build();

									notificationService.processEvent(notificationEvent);
									logger.info("Order status update email event sent for orderNumber={}",
											emailOrder.getOrderNumber());
								}
							}
							catch (Exception emailEx) {
								logger.error("Failed to send order status update email for shipmentId={}: {}",
										event.getShipmentId(), emailEx.getMessage());
							}
						}
					}
				}
				else {
					saveStepLog(event, "TRACK_SHIPMENT", "SKIPPED",
							"AWB not available for shipmentId=" + event.getShipmentId());
					logger.warn("Step TRACK_SHIPMENT skipped: AWB not available for shipmentId={}",
							event.getShipmentId());
				}
			}
			catch (Exception ex) {
				saveStepLog(event, "TRACK_SHIPMENT", "FAILED", ex.getMessage());
				logger.warn("Step TRACK_SHIPMENT FAILED for shipmentId={}: {}", event.getShipmentId(), ex.getMessage());
			}

		}
		catch (Exception e) {
			// Outer catch-all: save a generic FAILED record so nothing is silently lost
			saveStepLog(event, "PROCESS_EVENT", "FAILED", "Unexpected error: " + e.getMessage());
			logger.error("Error processing ShiprocketOrderEvent for shipmentId={}: {}", event.getShipmentId(),
					e.getMessage(), e);
		}
	}

	@Override
	public ShipTrackHistoryResponseDTO getShippingHistory(ShipTrackHistoryRequestDTO requestDTO) {
		ShipTrackHistoryResponseDTO responseDTO = new ShipTrackHistoryResponseDTO();
		logger.info("Fetching shipping history for tracking number: {}",
				requestDTO != null ? requestDTO.getTrackId() : null);
		try {
			String trackingNumber = requestDTO.getTrackId();
			ShippingEO shipment = shippingRepository.findByTrackingNumber(trackingNumber);
			if (shipment == null) {
				logger.warn("No shipment found for tracking number: {}", trackingNumber);
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

			// Publish RefundInitiatedEvent to Kafka
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
	// ──────────────────────────────────────────────────────────────────────────

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
							shippingEO.setExpectedDeliveryDate(java.time.LocalDate.parse(etd).atStartOfDay());
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

			response.setResponseStatus("SUCCESS");
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
	// ──────────────────────────────────────────────────────────────────────────

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
			if (request.getLabelUrl() != null && !request.getLabelUrl().trim().isEmpty()) {
				shippingEO.setLabelUrl(request.getLabelUrl().trim());
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
					// Prefer the active FORWARD shipment
					return list.stream()
						.filter(s -> Constants.SHIPMENT_TYPE_FORWARD.equals(s.getType())
								&& !Constants.SHIPMENT_STATUS_CANCELLED.equals(s.getShipmentStatus()))
						.findFirst()
						.orElse(list.get(0));
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
					return all.stream()
						.filter(s -> Constants.SHIPMENT_TYPE_FORWARD.equals(s.getType())
								&& !Constants.SHIPMENT_STATUS_CANCELLED.equals(s.getShipmentStatus()))
						.findFirst()
						.orElse(all.get(0));
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

	/** Inserts a tracking history row if historyStatus is provided in the request. */
	private boolean saveTrackingHistoryIfRequested(ShippingEO saved, ShippingOrderRequestDTO req) {
		if (req.getHistoryStatus() == null || req.getHistoryStatus().trim().isEmpty())
			return false;
		ShipmentTrackingHistoryEO history = ShipmentTrackingHistoryEO.builder()
			.shipment(saved)
			.status(req.getHistoryStatus().trim())
			.location(req.getHistoryLocation())
			.remarks(req.getHistoryRemarks() != null ? req.getHistoryRemarks() : "Manual action by admin.")
			.updatedAt(LocalDateTime.now())
			.build();
		shipmentTrackingHistoryRepository.save(history);
		return true;
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
				.isSelected(row.getIsSelected())
				.awbCode(row.getAwbCode())
				.shippingPrice(row.getShippingPrice())
				.createdAt(row.getCreatedAt())
				.build();
			dtos.add(dto);
		}
		return dtos;
	}

}
