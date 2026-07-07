package com.user.service;

import com.user.utility.Constants;
import com.user.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;

import com.user.dto.ServiceabilityRequestDTO;
import com.user.dto.VariantServiceabilityRequestDTO;
import com.user.dto.VariantServiceabilityResponseDTO;
import com.user.model.InventoryEO;
import com.user.model.WarehouseEO;
import com.user.repository.InventoryRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ShiprocketService {

	private static final Logger logger = LoggerFactory.getLogger(ShiprocketService.class);

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private InventoryRepository inventoryRepository;

	/**
	 * Returns the postal code of the given warehouse by name, or empty string if not
	 * found. If warehouseName is null or blank, falls back to
	 * {@link Constants#DEFAULT_WAREHOUSE_NAME}.
	 */
	private String getWarehousePostalCode(String warehouseName) {
		if (warehouseName == null || warehouseName.isBlank()) {
			warehouseName = Constants.DEFAULT_WAREHOUSE_NAME;
			logger.debug("getWarehousePostalCode: warehouseName is blank, using default '{}'", warehouseName);
		}
		return warehouseRepository.findByWarehouseNameIgnoreCaseAndStatus(warehouseName, Constants.STATUS_ACTIVE)
			.map(w -> w.getPostalCode() != null ? w.getPostalCode() : "")
			.orElse("");
	}

	@Value("${shiprocket.base-url}")
	private String baseUrl;

	private final RestTemplate restTemplate;

	private final ShiprocketAuthService authService;

	public ShiprocketService(RestTemplate restTemplate, ShiprocketAuthService authService) {
		this.restTemplate = restTemplate;
		this.authService = authService;
	}

	private HttpHeaders getAuthHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(authService.getToken());
		return headers;
	}

	// ✅ Create Order
	public Map createOrder(Map<String, Object> orderData) {
		String url = baseUrl + "/orders/create/adhoc";
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(orderData, getAuthHeaders());
		ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
		return response.getBody();
	}

	// ✅ Create Return / Reverse Order on Shiprocket
	// API: POST /orders/create/return
	// Pickup = customer address, Shipping/Delivery = warehouse address
	public Map createReturnOrder(Map<String, Object> returnOrderData) {
		String url = baseUrl + "/orders/create/return";
		logger.info("createReturnOrder: POST {} | body={}", url, returnOrderData);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(returnOrderData, getAuthHeaders());
		ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
		logger.info("createReturnOrder: response status={}, body={}", response.getStatusCode(), response.getBody());
		return response.getBody();
	}

	// ✅ Check Courier Serviceability / Rate (simple — backward compatible)
	public Map checkServiceability(String deliveryPostcode, String warehouseName) {
		String pickupPostcode = getWarehousePostalCode(warehouseName);
		double weight = 1;
		int codAmount = 0;
		String url = baseUrl + "/courier/serviceability/?pickup_postcode=" + pickupPostcode + "&delivery_postcode="
				+ deliveryPostcode + "&weight=" + weight + "&cod=" + codAmount;

		HttpEntity<Void> request = new HttpEntity<>(getAuthHeaders());
		ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
		return response.getBody();
	}

	// ✅ Check Courier Serviceability / Rate (full params)
	public Map checkServiceAvailability(ServiceabilityRequestDTO req) {
		StringBuilder url = new StringBuilder(baseUrl).append("/courier/serviceability/?");

		// Required params
		String pickupPostcode = (req.getPickupPostcode() != null) ? req.getPickupPostcode().toString()
				: getWarehousePostalCode(req.getWarehouseName());
		url.append("pickup_postcode=").append(pickupPostcode);
		url.append("&delivery_postcode=").append(req.getDeliveryPostcode());

		// Conditional / optional params
		if (req.getCod() != null)
			url.append("&cod=").append(req.getCod());
		if (req.getWeight() != null)
			url.append("&weight=").append(req.getWeight());
		if (req.getOrderId() != null)
			url.append("&order_id=").append(req.getOrderId());
		if (req.getLength() != null)
			url.append("&length=").append(req.getLength());
		if (req.getBreadth() != null)
			url.append("&breadth=").append(req.getBreadth());
		if (req.getHeight() != null)
			url.append("&height=").append(req.getHeight());
		if (req.getDeclaredValue() != null)
			url.append("&declared_value=").append(req.getDeclaredValue());
		if (req.getMode() != null)
			url.append("&mode=").append(req.getMode());
		if (req.getIsReturn() != null)
			url.append("&is_return=").append(req.getIsReturn());
		if (req.getCouriersType() != null)
			url.append("&couriers_type=").append(req.getCouriersType());
		if (req.getOnlyLocal() != null)
			url.append("&only_local=").append(req.getOnlyLocal());
		if (req.getQcCheck() != null)
			url.append("&qc_check=").append(req.getQcCheck());

		HttpEntity<Void> request = new HttpEntity<>(getAuthHeaders());
		ResponseEntity<Map> response = restTemplate.exchange(url.toString(), HttpMethod.GET, request, Map.class);
		return response.getBody();
	}

	// ─────────────────────────────────────────────────────────────────────────
	// ✅ Variant-aware serviceability check
	// For each product variant, resolves all warehouses via inventory records,
	// then calls Shiprocket serviceability for every (warehouse → delivery) pair.
	// Falls back to the default warehouse when no inventory / warehouse / postal
	// code is found for a variant.
	// Returns serviceable=true only when ALL pairs are serviceable.
	// ─────────────────────────────────────────────────────────────────────────
	public VariantServiceabilityResponseDTO checkVariantServiceability(VariantServiceabilityRequestDTO req) {

		List<Long> variantIds = req.getProductVariantIds();

		// ── Optimisation 1: Batch-load ALL inventory for all variants in ONE query ──
		// Replaces the previous per-variant findAllByProductVariant_Id() call inside the
		// loop
		// (N queries → 1 query). The warehouse association is EAGER on InventoryEO so it
		// comes along for free.
		List<InventoryEO> allInventory = inventoryRepository.findAllByProductVariantIdIn(variantIds);
		Map<Long, List<InventoryEO>> inventoryByVariantId = allInventory.stream()
			.collect(Collectors.groupingBy(i -> i.getProductVariant().getId().longValue()));

		// ── Optimisation 2: Load the default warehouse once before the loop ──
		// Previously getDefaultWarehouse() was called on every problematic inventory
		// record,
		// potentially hitting the DB dozens of times for a multi-variant request.
		WarehouseEO cachedDefaultWarehouse = getDefaultWarehouse();

		// ── Optimisation 3: Cache Shiprocket API results by pickup postcode ──
		// Since the delivery postcode is constant for the whole request, any two variants
		// served from the same warehouse postal code only need ONE external API call.
		// value = courierCount; -1 signals a cached exception result.
		Map<String, Integer> shiprocketResultCache = new HashMap<>();

		List<VariantServiceabilityResponseDTO.VariantDetail> variantDetails = new ArrayList<>();
		boolean overallServiceable = true;

		for (Long variantId : variantIds) {

			// ── 1. Resolve inventory for this variant (from the pre-loaded map) ──
			List<InventoryEO> inventoryList = inventoryByVariantId.getOrDefault(variantId, Collections.emptyList());

			List<VariantServiceabilityResponseDTO.WarehouseDetail> warehouseDetails = new ArrayList<>();
			boolean variantServiceable = true;

			// Build the list of (warehouse, usingDefault) pairs to check
			List<Object[]> warehousesToCheck = new ArrayList<>();

			if (inventoryList.isEmpty()) {
				logger.warn(
						"checkVariantServiceability: no inventory found for variantId={}, falling back to default warehouse",
						variantId);
				if (cachedDefaultWarehouse != null) {
					warehousesToCheck.add(new Object[] { cachedDefaultWarehouse, Boolean.TRUE });
				}
				else {
					logger.warn("checkVariantServiceability: default warehouse not found either, variantId={}",
							variantId);
					variantServiceable = false;
					overallServiceable = false;
					warehouseDetails.add(VariantServiceabilityResponseDTO.WarehouseDetail.builder()
						.serviceable(false)
						.reason("No inventory found and default warehouse is not configured")
						.build());
				}
			}
			else {
				for (InventoryEO inv : inventoryList) {
					WarehouseEO wh = inv.getWarehouse();
					if (wh == null) {
						logger.warn(
								"checkVariantServiceability: inventory id={} has no warehouse, falling back to default",
								inv.getId());
						if (cachedDefaultWarehouse != null) {
							warehousesToCheck.add(new Object[] { cachedDefaultWarehouse, Boolean.TRUE });
						}
						else {
							warehouseDetails.add(VariantServiceabilityResponseDTO.WarehouseDetail.builder()
								.serviceable(false)
								.reason("Warehouse not linked to inventory record and default warehouse is not configured")
								.build());
						}
					}
					else {
						warehousesToCheck.add(new Object[] { wh, Boolean.FALSE });
					}
				}
			}

			// ── 2. Check serviceability for each resolved warehouse ──────────────
			for (Object[] entry : warehousesToCheck) {
				WarehouseEO warehouse = (WarehouseEO) entry[0];
				boolean usingDefault = (Boolean) entry[1];

				String pickupPostcode = warehouse.getPostalCode();

				// If warehouse has no postal code, fall back to default warehouse
				if (pickupPostcode == null || pickupPostcode.isBlank()) {
					logger.warn(
							"checkVariantServiceability: warehouse id={} has no postal code, falling back to default warehouse",
							warehouse.getWarehouseId());
					if (cachedDefaultWarehouse != null && cachedDefaultWarehouse.getPostalCode() != null
							&& !cachedDefaultWarehouse.getPostalCode().isBlank()) {
						pickupPostcode = cachedDefaultWarehouse.getPostalCode();
						warehouse = cachedDefaultWarehouse;
						usingDefault = true;
					}
					else {
						variantServiceable = false;
						overallServiceable = false;
						warehouseDetails.add(VariantServiceabilityResponseDTO.WarehouseDetail.builder()
							.warehouseId(warehouse.getWarehouseId())
							.warehouseName(warehouse.getWarehouseName())
							.warehousePostalCode(null)
							.serviceable(false)
							.availableCourierCount(0)
							.reason("No postal code configured for this warehouse and default warehouse has no postal code either")
							.build());
						continue;
					}
				}

				// ── 3. Call Shiprocket serviceability API (with cache) ────────────
				boolean pairServiceable;
				int courierCount;
				String reason = null;

				Integer cachedCount = shiprocketResultCache.get(pickupPostcode);
				if (cachedCount != null) {
					// Cache hit — reuse result without a new external API call
					if (cachedCount < 0) {
						pairServiceable = false;
						courierCount = 0;
						reason = "Shiprocket API error (result cached from previous call)";
					}
					else {
						courierCount = cachedCount;
						pairServiceable = courierCount > 0;
						if (!pairServiceable)
							reason = "No courier companies available for this route";
					}
				}
				else {
					// Cache miss — call Shiprocket and store the result
					courierCount = 0;
					try {
						String apiUrl = baseUrl + "/courier/serviceability/?pickup_postcode=" + pickupPostcode
								+ "&delivery_postcode=" + req.getDeliveryPostcode() + "&weight=1&cod=0";

						HttpEntity<Void> httpReq = new HttpEntity<>(getAuthHeaders());
						ResponseEntity<Map> apiResp = restTemplate.exchange(apiUrl, HttpMethod.GET, httpReq, Map.class);

						Map body = apiResp.getBody();
						if (body != null) {
							Object dataObj = body.get("data");
							if (dataObj instanceof Map) {
								Object couriersObj = ((Map) dataObj).get("available_courier_companies");
								if (couriersObj instanceof List) {
									courierCount = ((List<?>) couriersObj).size();
								}
							}
						}
						shiprocketResultCache.put(pickupPostcode, courierCount);
						pairServiceable = courierCount > 0;
						if (!pairServiceable)
							reason = "No courier companies available for this route";
					}
					catch (Exception e) {
						logger.error("checkVariantServiceability: Shiprocket API error for pickup={} delivery={}: {}",
								pickupPostcode, req.getDeliveryPostcode(), e.getMessage());
						shiprocketResultCache.put(pickupPostcode, -1); // cache the error
																		// to skip retries
						pairServiceable = false;
						reason = "Shiprocket API error: " + e.getMessage();
					}
				}

				if (!pairServiceable) {
					variantServiceable = false;
					overallServiceable = false;
				}

				warehouseDetails.add(VariantServiceabilityResponseDTO.WarehouseDetail.builder()
					.warehouseId(warehouse.getWarehouseId())
					.warehouseName(warehouse.getWarehouseName())
					.warehousePostalCode(pickupPostcode)
					.serviceable(pairServiceable)
					.availableCourierCount(courierCount)
					.usingDefaultWarehouse(usingDefault)
					.reason(reason)
					.build());
			}

			variantDetails.add(VariantServiceabilityResponseDTO.VariantDetail.builder()
				.productVariantId(variantId)
				.serviceable(variantServiceable)
				.warehouses(warehouseDetails)
				.build());
		}

		String message = overallServiceable ? "Delivery is available from all warehouses to the requested postcode"
				: "Delivery is not available from one or more warehouses to the requested postcode";

		return VariantServiceabilityResponseDTO.builder()
			.serviceable(overallServiceable)
			.deliveryPostcode(req.getDeliveryPostcode())
			.message(message)
			.variants(variantDetails)
			.build();
	}

	/** Returns the default warehouse entity, or null if not found. */
	private WarehouseEO getDefaultWarehouse() {
		return warehouseRepository
			.findByWarehouseNameIgnoreCaseAndStatus(Constants.DEFAULT_WAREHOUSE_NAME, Constants.STATUS_ACTIVE)
			.orElse(null);
	}

	// ✅ Track Shipment
	public Map trackShipment(String awbCode) {
		String url = baseUrl + "/courier/track/awb/" + awbCode;
		HttpEntity<Void> request = new HttpEntity<>(getAuthHeaders());
		ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
		return response.getBody();
	}

	// ✅ Generate AWB (assign courier) — auto-assign
	public Map generateAWB(Integer shipmentId) {
		return generateAWB(shipmentId, null);
	}

	// ✅ Generate AWB (assign courier) — with optional specific courier
	public Map generateAWB(Integer shipmentId, Integer courierCompanyId) {
		String url = baseUrl + "/courier/assign/awb";
		Map<String, Object> body = new HashMap<>();
		body.put("shipment_id", shipmentId.toString());
		if (courierCompanyId != null) {
			// Send as Integer (JSON number) — Shiprocket requires courier_company_id as a
			// number

			// Also send courier_id as some Shiprocket versions use this field name
			body.put("courier_id", courierCompanyId);
		}
		logger.info("generateAWB: POST {} | shipmentId={}, courierCompanyId={}, fullBody={}", url, shipmentId,
				courierCompanyId, body);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, getAuthHeaders());
		ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
		logger.info("generateAWB: response status={}, body={}", response.getStatusCode(), response.getBody());
		return response.getBody();
	}

	/**
	 * Selects the best courier service from Shiprocket serviceability API using a
	 * balanced score of lowest price + fastest estimated delivery days.
	 * @param req ServiceabilityRequestDTO with delivery details
	 * @return courier_company_id of the best courier, or null if unable to determine
	 */
	public Integer getBestCourierService(ServiceabilityRequestDTO req) {
		try {
			Map serviceabilityResponse = checkServiceAvailability(req);
			if (serviceabilityResponse == null) {
				logger.warn("getBestCourierService: null response from Shiprocket serviceability API");
				return null;
			}

			// Extract available_courier_companies from data
			Object dataObj = serviceabilityResponse.get("data");
			if (!(dataObj instanceof Map)) {
				logger.warn("getBestCourierService: 'data' key missing or not a Map in serviceability response");
				return null;
			}
			Object couriersObj = ((Map) dataObj).get("available_courier_companies");
			if (!(couriersObj instanceof List)) {
				logger.warn("getBestCourierService: 'available_courier_companies' missing or not a List");
				return null;
			}
			List<Map> couriers = (List<Map>) couriersObj;
			if (couriers.isEmpty()) {
				logger.warn("getBestCourierService: no available courier companies returned");
				return null;
			}

			// If only one courier available, return it directly
			if (couriers.size() == 1) {
				Object id = couriers.get(0).get("courier_company_id");
				if (id == null)
					id = couriers.get(0).get("id");
				Integer result = id instanceof Number ? ((Number) id).intValue() : null;
				logger.info("getBestCourierService: only 1 courier available, id={}, name={}", result,
						couriers.get(0).get("courier_name"));
				return result;
			}

			// Collect rate and delivery days for each courier
			List<Double> rates = new ArrayList<>();
			List<Double> days = new ArrayList<>();
			for (Map courier : couriers) {
				double rate = extractDouble(courier, "rate");
				if (rate <= 0)
					rate = extractDouble(courier, "freight_charge");
				double deliveryDays = extractDouble(courier, "estimated_delivery_days");
				rates.add(rate);
				days.add(deliveryDays);
				// Log each available courier for debugging
				Object cId = courier.get("courier_company_id");
				if (cId == null)
					cId = courier.get("id");
				logger.info("getBestCourierService: available courier id={}, name={}, rate={}, days={}", cId,
						courier.get("courier_name"), rate, deliveryDays);
			}

			double minRate = rates.stream().mapToDouble(Double::doubleValue).min().orElse(0);
			double maxRate = rates.stream().mapToDouble(Double::doubleValue).max().orElse(0);
			double minDays = days.stream().mapToDouble(Double::doubleValue).min().orElse(0);
			double maxDays = days.stream().mapToDouble(Double::doubleValue).max().orElse(0);

			double bestScore = Double.MAX_VALUE;
			Integer bestCourierId = null;
			String bestCourierName = null;

			for (int i = 0; i < couriers.size(); i++) {
				Map courier = couriers.get(i);
				double normRate = (maxRate > minRate) ? (rates.get(i) - minRate) / (maxRate - minRate) : 0.0;
				double normDays = (maxDays > minDays) ? (days.get(i) - minDays) / (maxDays - minDays) : 0.0;
				// Balanced score: 50% weight on price, 50% on delivery speed
				double score = 0.5 * normRate + 0.5 * normDays;

				logger.debug("getBestCourierService: courier={}, rate={}, days={}, score={}",
						courier.get("courier_name"), rates.get(i), days.get(i), score);

				if (score < bestScore) {
					bestScore = score;
					Object idObj = courier.get("courier_company_id");
					if (idObj == null)
						idObj = courier.get("id"); // fallback to "id" field
					bestCourierId = idObj instanceof Number ? ((Number) idObj).intValue() : null;
					bestCourierName = (String) courier.get("courier_name");
				}
			}

			logger.info("getBestCourierService: selected courierCompanyId={}, name={}, score={}", bestCourierId,
					bestCourierName, bestScore);
			return bestCourierId;

		}
		catch (Exception e) {
			logger.error("getBestCourierService: error selecting best courier: {}", e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Returns an ordered list of the top {@code maxCount} best courier company IDs ranked
	 * by a balanced score of lowest price + fastest estimated delivery.
	 * @param req serviceability request parameters
	 * @param maxCount maximum number of candidates to return (e.g. 10)
	 * @return ordered list of courier_company_id (best first), may be empty but never
	 * null
	 */
	public List<Integer> getBestCourierServices(ServiceabilityRequestDTO req, int maxCount) {
		return getBestCourierServices(req, maxCount, null, null);
	}

	public List<Integer> getBestCourierServices(ServiceabilityRequestDTO req, int maxCount,
			Map<Integer, Double> courierRateOut) {
		return getBestCourierServices(req, maxCount, courierRateOut, null);
	}

	/**
	 * Full overload: returns ranked courier IDs, populates rate map, and optionally fills
	 * {@code courierDetailsOut} with one entry per candidate containing:
	 * courierCompanyId, courierName, rate, estimatedDeliveryDays, rank
	 */
	public List<Integer> getBestCourierServices(ServiceabilityRequestDTO req, int maxCount,
			Map<Integer, Double> courierRateOut, List<Map<String, Object>> courierDetailsOut) {
		List<Integer> result = new ArrayList<>();
		try {
			Map serviceabilityResponse = checkServiceAvailability(req);
			if (serviceabilityResponse == null) {
				logger.warn("getBestCourierServices: null response from serviceability API");
				return result;
			}

			Object dataObj = serviceabilityResponse.get("data");
			if (!(dataObj instanceof Map)) {
				logger.warn("getBestCourierServices: 'data' missing or not a Map");
				return result;
			}
			Object couriersObj = ((Map) dataObj).get("available_courier_companies");
			if (!(couriersObj instanceof List)) {
				logger.warn("getBestCourierServices: 'available_courier_companies' missing");
				return result;
			}
			List<Map> couriers = (List<Map>) couriersObj;
			if (couriers.isEmpty())
				return result;

			// Compute min/max for normalisation
			List<Double> rates = new ArrayList<>();
			List<Double> days = new ArrayList<>();
			for (Map c : couriers) {
				double rate = extractDouble(c, "rate");
				if (rate <= 0)
					rate = extractDouble(c, "freight_charge");
				rates.add(rate);
				days.add(extractDouble(c, "estimated_delivery_days"));
			}
			double minRate = rates.stream().mapToDouble(Double::doubleValue).min().orElse(0);
			double maxRate = rates.stream().mapToDouble(Double::doubleValue).max().orElse(0);
			double minDays = days.stream().mapToDouble(Double::doubleValue).min().orElse(0);
			double maxDays = days.stream().mapToDouble(Double::doubleValue).max().orElse(0);

			// Build scored entries
			List<double[]> scored = new ArrayList<>(); // [index, score]
			for (int i = 0; i < couriers.size(); i++) {
				double normRate = (maxRate > minRate) ? (rates.get(i) - minRate) / (maxRate - minRate) : 0.0;
				double normDays = (maxDays > minDays) ? (days.get(i) - minDays) / (maxDays - minDays) : 0.0;
				double score = 0.5 * normRate + 0.5 * normDays;
				scored.add(new double[] { i, score });
				Object cId = couriers.get(i).get("courier_company_id");
				if (cId == null)
					cId = couriers.get(i).get("id");
				logger.info("getBestCourierServices: courier id={}, name={}, rate={}, days={}, score={}", cId,
						couriers.get(i).get("courier_name"), rates.get(i), days.get(i), score);
			}

			// Sort ascending by score (best = lowest score)
			scored.sort(Comparator.comparingDouble(e -> e[1]));

			int limit = Math.min(maxCount, scored.size());
			for (int i = 0; i < limit; i++) {
				int idx = (int) scored.get(i)[0];
				Map courier = couriers.get(idx);
				Object idObj = courier.get("courier_company_id");
				if (idObj == null)
					idObj = courier.get("id");
				if (idObj instanceof Number) {
					int courierId = ((Number) idObj).intValue();
					result.add(courierId);
					// Populate rate map if requested
					if (courierRateOut != null) {
						courierRateOut.put(courierId, rates.get(idx));
					}
					// Populate full details list if requested
					if (courierDetailsOut != null) {
						Map<String, Object> detail = new HashMap<>();
						detail.put("courierCompanyId", courierId);
						Object nameObj = courier.get("courier_name");
						detail.put("courierName", nameObj instanceof String ? nameObj : null);
						detail.put("rate", rates.get(idx));
						detail.put("estimatedDeliveryDays", days.get(idx));
						detail.put("rank", i + 1);
						courierDetailsOut.add(detail);
					}
				}
			}
			logger.info("getBestCourierServices: returning {} candidates: {}", result.size(), result);

		}
		catch (Exception e) {
			logger.error("getBestCourierServices: error: {}", e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Transfers (reassigns) a Shiprocket shipment to a different courier. Shiprocket API:
	 * POST /courier/reassign
	 * @param shipmentId Shiprocket shipment_id
	 * @param courierCompanyId new courier_company_id to assign
	 * @return raw Shiprocket response map
	 */
	public Map transferCourier(Integer shipmentId, Integer courierCompanyId) {
		String url = baseUrl + "/courier/reassign";
		Map<String, Object> body = new HashMap<>();
		body.put("shipment_id", shipmentId);
		body.put("courier_company_id", courierCompanyId);
		logger.info("transferCourier: POST {} | shipmentId={}, newCourierCompanyId={}", url, shipmentId,
				courierCompanyId);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, getAuthHeaders());
		ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
		logger.info("transferCourier: response status={}, body={}", response.getStatusCode(), response.getBody());
		return response.getBody();
	}

	private double extractDouble(Map map, String key) {
		Object val = map.get(key);
		if (val instanceof Number)
			return ((Number) val).doubleValue();
		if (val instanceof String) {
			try {
				return Double.parseDouble((String) val);
			}
			catch (NumberFormatException ignored) {
			}
		}
		return 0.0;
	}

	// ✅ Request Pickup
	// Shiprocket returns 400 "Already in Pickup Queue" when the pickup is already
	// scheduled.
	// This is a valid "already done" state, so we return a synthetic success map in that
	// case
	// instead of letting the HttpClientErrorException propagate.
	public Map requestPickup(String shipmentId) {
		String url = baseUrl + "/courier/generate/pickup";
		Map<String, Object> body = Map.of("shipment_id", List.of(shipmentId));
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, getAuthHeaders());
		try {
			ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
			return response.getBody();
		}
		catch (org.springframework.web.client.HttpClientErrorException ex) {
			String responseBody = ex.getResponseBodyAsString();
			if (ex.getStatusCode().value() == 400 && responseBody != null
					&& responseBody.contains("Already in Pickup Queue")) {
				logger.info("requestPickup: shipmentId={} is already in pickup queue – treating as PICKUP_SCHEDULED",
						shipmentId);
				// Return a synthetic response so the caller knows pickup is already
				// scheduled
				Map<String, Object> alreadyQueued = new java.util.HashMap<>();
				alreadyQueued.put("already_in_pickup_queue", true);
				alreadyQueued.put("message", "Already in Pickup Queue");
				return alreadyQueued;
			}
			throw ex;
		}
	}

	public Map generateLabel(List<String> shipmentIds) {
		String url = baseUrl + "/courier/generate/label";
		Map<String, Object> body = Map.of("shipment_id", shipmentIds);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, getAuthHeaders());
		ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
		return response.getBody();
	}

	// ✅ Cancel Order(s) on Shiprocket
	// Shiprocket API: POST /orders/cancel Body: { "ids": [shiprocket_order_id, ...] }
	public Map cancelOrder(List<Integer> shiprocketOrderIds) {
		String url = baseUrl + "/orders/cancel";
		Map<String, Object> body = Map.of("ids", shiprocketOrderIds);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, getAuthHeaders());
		ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
		return response.getBody();
	}

}
