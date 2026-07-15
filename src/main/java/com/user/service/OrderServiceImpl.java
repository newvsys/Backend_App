package com.user.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import com.user.communication.event.Event;
import com.user.communication.event.OrderEvent;
import com.user.communication.event.RefundInitiatedEvent;
import com.user.communication.service.NotificationService;
import com.user.dto.*;
import com.user.model.*;
import com.user.repository.*;
import com.user.utility.Constants;
import com.user.utility.ObjectMapper;
import com.user.utility.ReturnStatus;
import com.user.utility.UserMapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderServiceImpl implements OrderService {

	private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ProductVariantRepository productVariantRepository;

	@Autowired
	private OrderItemRepository orderProductRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private OrderNumberService orderNumberService;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private CustomerAddressRepository customerAddressRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrderAddressRepository orderAddressRepository;

	@Autowired
	private RazorpayClient razorpayClient;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	@Lazy
	private ShippingService shippingService;

	@Autowired
	private ProductImageRepository productImageRepository;

	@Autowired
	private OrderCancelRequestRepository orderCancelRequestRepository;

	@Autowired
	private ReasonMasterRepository reasonMasterRepository;

	@Autowired
	private RefundTransactionRepository refundTransactionRepository;

	@Autowired
	private ReturnRequestRepository returnRequestRepository;

	@Autowired
	private ReturnStatusHistoryRepository returnStatusHistoryRepository;

	@Autowired
	private ReturnImageRepository returnImageRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

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

	@Value("${razorpay.key.id}")
	private String keyId;

	@Value("${return.image.upload-dir}")
	private String returnImageUploadDir;

	@Autowired
	private CloudinaryMediaService cloudinaryMediaService;

	@Autowired
	private ShipmentTrackingHistoryRepository shipmentTrackingHistoryRepository;

	@Autowired
	private ShippingRepository shippingRepository;

	@Autowired
	private ReturnPolicyRepository returnPolicyRepository;

	@Autowired
	private ReturnPolicyConditionRepository returnPolicyConditionRepository;

	@Autowired
	private ReturnPolicyMappingRepository returnPolicyMappingRepository;

	@Autowired
	private ProductCatRepository productCatRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ShiprocketService shiprocketService;

	@Autowired
	private CourierSelectionLogRepository courierSelectionLogRepository;

	@Autowired
	private DeliveryChargeRepository deliveryChargeRepository;

	@Override
	public OrderResponseDTO createOrder(OrderCreateDTO orderCreateDTO) {
		OrderResponseDTO responseDTO = new OrderResponseDTO();
		logger.info("Starting createOrder for userId={}, customerId={}, products={}", orderCreateDTO.getUserId(),
				orderCreateDTO.getCustomerId(), orderCreateDTO.getProducts());
		try {
			// Step 1: create and save CustomerEO
			CustomerEO customer = null;

			if (orderCreateDTO.getCustomerId() != null) {
				customer = customerRepository.findById(orderCreateDTO.getCustomerId()).orElse(null);
			}

			if (customer == null) {
				customer = new CustomerEO();
				UserEO user = null;
				if (orderCreateDTO.getUserId() != null && !orderCreateDTO.getUserId().trim().isEmpty()) {
					// You can fetch User entity from UserRepository if needed
					user = userRepository.findById(Long.valueOf(orderCreateDTO.getUserId())).orElse(null);
				}
				if (user != null) {
					customer.setUser(user);
					customer.setCustomerType(Constants.CUSTOMER_TYPE_REGISTERED);
				}
				else {

					UserEO user1 = new UserEO();

					user1.setRole(Constants.ROLE_USER); // Always set role as "user"
					if (orderCreateDTO.getPhone() != null) {
						user1.setPhone(orderCreateDTO.getPhone());
						customer.setMobileNumber(orderCreateDTO.getPhone());
						user1.setCreatedBy(orderCreateDTO.getPhone());

					}
					else {
						responseDTO.setMessage(Constants.PHONE_NO_MISSING_CUST_USER);
						responseDTO.setStatus(Constants.FAILURE_STATUS);
						return responseDTO;
					}
					if (orderCreateDTO.getName() != null) {
						user1.setFirstName(orderCreateDTO.getName());
						customer.setFirstName(orderCreateDTO.getName());
					}
					if (orderCreateDTO.getEmail() != null) {
						customer.setEmail(orderCreateDTO.getEmail());
						user1.setEmail(orderCreateDTO.getEmail());
					}
					user1.setStatus(Constants.STATUS_ACTIVE);
					user1.setCreatedAt(OffsetDateTime.now());

					UserEO savedUser = userRepository.save(user1);
					customer.setCustomerType(Constants.CUSTOMER_TYPE_GUEST);
					customer.setUser(savedUser);
					customer.setCreatedBy(savedUser.getPhone());
				}

				customer.setStatus(Constants.STATUS_ACTIVE);
				customer.setCreatedAt(OffsetDateTime.now());
				customer = customerRepository.save(customer);

			}

			// Step 2: Calculate subtotal from product variants (server-side; client total
			// is ignored)
			List<OrderProductRequestDTO> productsForPricing = orderCreateDTO.getProducts();
			if (productsForPricing == null)
				productsForPricing = Collections.emptyList();

			BigDecimal subtotalAmount = BigDecimal.ZERO;
			for (OrderProductRequestDTO p : productsForPricing) {
				ProductVariantEO pv = productVariantRepository.findById(Long.valueOf(p.getProductId()))
					.orElseThrow(() -> new RuntimeException("Product variant not found: " + p.getProductId()));
				subtotalAmount = subtotalAmount.add(pv.getSellingPrice().multiply(BigDecimal.valueOf(p.getQuantity())));
			}

			// Look up the applicable delivery charge rule
			BigDecimal shippingFee = BigDecimal.ZERO;
			boolean isFreeDelivery = true;
			List<DeliveryChargeEO> chargeRules = deliveryChargeRepository.findMatchingRules(subtotalAmount);
			if (!chargeRules.isEmpty()) {
				DeliveryChargeEO matchedRule = chargeRules.get(0);
				shippingFee = matchedRule.getDeliveryCharge();
				isFreeDelivery = matchedRule.getIsFreeDelivery();
				logger.info("Delivery charge rule matched: id={}, ruleName={}, shippingFee={}, subtotal={}",
						matchedRule.getId(), matchedRule.getRuleName(), shippingFee, subtotalAmount);
			}
			else {
				logger.warn("No delivery charge rule found for subtotal={}. Defaulting shippingFee=0", subtotalAmount);
			}

			BigDecimal grandTotal = subtotalAmount.add(shippingFee);

			// Step 2b: create and save OrderEO linked to the customer
			OrderEO order = new OrderEO();
			order.setCustomer(customer);
			order.setOrderNumber(orderNumberService.generateOrderNumber());
			order.setOrderStatus(
					orderCreateDTO.getStatus() != null ? orderCreateDTO.getStatus() : Constants.ORDER_STATUS_CREATED);
			order.setPaymentStatus(Constants.ORDER_PAYMENT_STATUS_PENDING);
			order.setCurrency(Constants.PAYMENT_CURRENCY);
			order.setSubtotalAmount(subtotalAmount);
			order.setShippingFee(shippingFee);
			order.setTaxAmount(BigDecimal.ZERO);
			order.setDiscountAmount(BigDecimal.ZERO);
			order.setTotalAmount(grandTotal);
			OrderEO savedOrder = orderRepository.save(order);

			// Step 3: create and save OrderAddressEO for this order
			CustomerAddressEO customerAddress = null;
			if (orderCreateDTO.getOrderAddressId() != null) {
				customerAddress = customerAddressRepository.findById(orderCreateDTO.getOrderAddressId()).orElse(null);
			}
			OrderAddressEO address = new OrderAddressEO();
			address.setOrder(savedOrder);
			if (customerAddress == null) {
				// Populate and persist CustomerAddressEO from inline address fields
				CustomerAddressEO newCustomerAddress = new CustomerAddressEO();
				newCustomerAddress.setCustomer(customer);
				newCustomerAddress.setAddressType(Constants.ADDRESS_TYPE_BOTH);
				if (orderCreateDTO.getName() != null)
					newCustomerAddress.setRecipientName(orderCreateDTO.getName());
				if (orderCreateDTO.getAddress1() != null)
					newCustomerAddress.setAddressLine1(orderCreateDTO.getAddress1());
				if (orderCreateDTO.getAddress2() != null)
					newCustomerAddress.setAddressLine2(orderCreateDTO.getAddress2());
				if (orderCreateDTO.getCity() != null)
					newCustomerAddress.setCity(orderCreateDTO.getCity());
				if (orderCreateDTO.getState() != null)
					newCustomerAddress.setState(orderCreateDTO.getState());
				if (orderCreateDTO.getLandmark() != null)
					newCustomerAddress.setLandMark(orderCreateDTO.getLandmark());
				if (orderCreateDTO.getPostalCode() != null)
					newCustomerAddress.setPostalCode(orderCreateDTO.getPostalCode());
				if (orderCreateDTO.getCountry() != null)
					newCustomerAddress.setCountry(orderCreateDTO.getCountry());
				if (orderCreateDTO.getPhone() != null)
					newCustomerAddress.setContactNumber(orderCreateDTO.getPhone());
				customerAddress = customerAddressRepository.save(newCustomerAddress);
				logger.info("Created new CustomerAddressEO from inline address for customerId={}",
						customer.getCustomerId());

				// Populate OrderAddressEO from the saved customer address
				address.setAddressType(customerAddress.getAddressType());
				if (customerAddress.getRecipientName() != null)
					address.setRecipientName(customerAddress.getRecipientName());
				if (customerAddress.getAddressLine1() != null)
					address.setAddressLine1(customerAddress.getAddressLine1());
				if (customerAddress.getAddressLine2() != null)
					address.setAddressLine2(customerAddress.getAddressLine2());
				if (customerAddress.getCity() != null)
					address.setCity(customerAddress.getCity());
				if (customerAddress.getState() != null)
					address.setState(customerAddress.getState());
				if (customerAddress.getLandMark() != null)
					address.setLandMark(customerAddress.getLandMark());
				if (customerAddress.getPostalCode() != null)
					address.setPostalCode(customerAddress.getPostalCode());
				if (customerAddress.getCountry() != null)
					address.setCountry(customerAddress.getCountry());
				if (customerAddress.getContactNumber() != null)
					address.setContactNumber(customerAddress.getContactNumber());
			}
			else {
				address.setAddressType(customerAddress.getAddressType());
				if (customerAddress.getRecipientName() != null)
					address.setRecipientName(customerAddress.getRecipientName());
				if (customerAddress.getAddressLine1() != null)
					address.setAddressLine1(customerAddress.getAddressLine1());
				if (customerAddress.getAddressLine2() != null)
					address.setAddressLine2(customerAddress.getAddressLine2());
				if (customerAddress.getCity() != null)
					address.setCity(customerAddress.getCity());
				if (customerAddress.getState() != null)
					address.setState(customerAddress.getState());
				if (customerAddress.getLandMark() != null)
					address.setLandMark(customerAddress.getLandMark());
				if (customerAddress.getPostalCode() != null)
					address.setPostalCode(customerAddress.getPostalCode());
				if (customerAddress.getCountry() != null)
					address.setCountry(customerAddress.getCountry());
				if (customerAddress.getContactNumber() != null)
					address.setContactNumber(customerAddress.getContactNumber());
			}
			orderAddressRepository.save(address);

			// Step 4: create and save OrderItemEOs for each product in the order
			List<OrderProductRequestDTO> products = orderCreateDTO.getProducts();
			if (products == null) {
				products = Collections.emptyList();
			}

			List<OrderItemEO> orderItems = products.stream().map(productDTO -> {
				OrderItemEO orderItem = new OrderItemEO();
				orderItem.setOrder(savedOrder);

				ProductVariantEO productVariant = productVariantRepository
					.findById(Long.valueOf(productDTO.getProductId()))
					.orElseThrow(() -> new RuntimeException("Product variant not found: " + productDTO.getProductId()));

				orderItem.setProductVar(productVariant);
				orderItem.setQuantity(productDTO.getQuantity());
				orderItem.setSkuCode(productVariant.getSkuCode());
				orderItem.setProductVarName(productVariant.getProduct().getName());

				// Pricing
				orderItem.setUnitPrice(productVariant.getSellingPrice());
				orderItem.setTotalPrice(
						productVariant.getSellingPrice().multiply(BigDecimal.valueOf(productDTO.getQuantity())));

				return orderItem;
			}).collect(Collectors.toList());

			// persist all order items
			List<OrderItemEO> savedorderItems = orderProductRepository.saveAll(orderItems);

			// Create razorpay payment order — amount is grand total (subtotal + shipping)
			// in paise
			JSONObject orderRequest = new JSONObject();
			orderRequest.put("amount", savedOrder.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue());
			orderRequest.put("currency", "INR");
			orderRequest.put("receipt", "order_rcptid_" + System.currentTimeMillis());
			// Auto-capture payment so it can be refunded later
			orderRequest.put("payment_capture", 1);
			// Pass delivery charge breakdown as notes for reference
			JSONObject notes = new JSONObject();
			notes.put("subtotal", savedOrder.getSubtotalAmount().toPlainString());
			notes.put("shipping_fee", savedOrder.getShippingFee().toPlainString());
			notes.put("order_number", savedOrder.getOrderNumber());
			orderRequest.put("notes", notes);

			Order payOrder = razorpayClient.orders.create(orderRequest);

			// update payment
			PaymentEO payment = new PaymentEO();
			payment.setPaymentStatus("CREATED");
			payment.setOrder(savedOrder);
			payment.setPaymentProviderOrderId((String) payOrder.get("id"));

			// Razorpay returns amount in paise (integer).
			BigDecimal paidAmount = new BigDecimal(((Number) payOrder.get("amount")).longValue())
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
			payment.setAmount(paidAmount);

			PaymentEO savedPayment = paymentRepository.save(payment);
			// 3. Update Inventory (reduce stock for cancelled item)
			for (OrderItemEO orderItem : savedorderItems) {
				ProductVariantEO productVariant = orderItem.getProductVar();
				if (productVariant != null) {
					InventoryEO inventory = inventoryRepository.findByProductVariant(productVariant);
					inventory.setAvailableQty(inventory.getAvailableQty() - orderItem.getQuantity());
					inventory.setTotalQty(Math.max(0,
							(inventory.getTotalQty() != null ? inventory.getTotalQty() : 0) - orderItem.getQuantity()));
					inventoryRepository.save(inventory);
				}
			}
			responseDTO.setOrderNumber(savedOrder.getOrderNumber());
			responseDTO.setPaymentOrderId(savedPayment.getPaymentProviderOrderId());
			responseDTO.setSubtotalAmount(savedOrder.getSubtotalAmount());
			responseDTO.setShippingFee(savedOrder.getShippingFee());
			responseDTO.setIsFreeDelivery(savedOrder.getShippingFee().compareTo(BigDecimal.ZERO) == 0);
			responseDTO.setAmount(savedOrder.getTotalAmount());
			responseDTO.setCurrency(Constants.PAYMENT_CURRENCY);
			responseDTO.setStoreName(Constants.STORE_NAME);
			responseDTO.setDescription(Constants.ORDER_DESCRIPTION);
			responseDTO.setPaymentGatewayKey(keyId);
			responseDTO.setMessage(Constants.ORDER_CREATED_SUCCESS);
			responseDTO.setStatus(Constants.STATUS_SUCCESS);

			logger.info(
					"Order created successfully for userId={}, customerId={}, orderNumber={}, subtotal={}, shippingFee={}, total={}",
					orderCreateDTO.getUserId(), orderCreateDTO.getCustomerId(), order.getOrderNumber(), subtotalAmount,
					shippingFee, grandTotal);

		}
		catch (Exception e) {
			responseDTO.setMessage(Constants.ORDER_CREATED_FAILURE);
			responseDTO.setStatus(Constants.FAILURE_STATUS);
			logger.error("Error creating order for userId={}, customerId={}: {}", orderCreateDTO.getUserId(),
					orderCreateDTO.getCustomerId(), e.getMessage(), e);
			return responseDTO;
		}
		return responseDTO;
	}

	@Override
	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public OrderStatusDTO getOrderStatus(String orderId) {
		// Fetch order by order number
		OrderEO order = orderRepository.findByOrderNumber(orderId)
			.orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

		// Fetch order items
		List<OrderItemEO> orderItems = orderProductRepository.findByOrder(order);

		// Fetch customer address
		OrderAddressEO address = orderAddressRepository.findByOrder(order)
			.orElseThrow(() -> new RuntimeException("Order address not found for order: " + orderId));

		// Fetch payment
		PaymentEO payment = paymentRepository.findByOrder(order)
			.orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));

		OrderStatusDTO dto = new OrderStatusDTO();

		// ── Order Info ──────────────────────────────────────────────────────
		dto.setOrderNumber(order.getOrderNumber());
		dto.setStatus(order.getOrderStatus());
		dto.setCurrency(order.getCurrency());

		// ── Payment Details ──────────────────────────────────────────────────
		dto.setPaymentStatus(payment.getPaymentStatus());
		dto.setTransactionId(payment.getTransactionId());
		dto.setPaymentMethod(payment.getPaymentMethod());
		dto.setPaymentTime(payment.getPaymentTime());
		dto.setTotal(payment.getAmount());

		// ── Address Info ─────────────────────────────────────────────────────
		dto.setName(address.getRecipientName());
		dto.setAddress1(address.getAddressLine1());
		dto.setAddress2(address.getAddressLine2());
		dto.setLandmark(address.getLandMark());
		dto.setCity(address.getCity());
		dto.setState(address.getState());
		dto.setPostalCode(address.getPostalCode());
		dto.setCountry(address.getCountry());

		// Build a single-line delivery address summary for display
		StringBuilder addrSummary = new StringBuilder();
		if (address.getAddressLine1() != null && !address.getAddressLine1().isBlank())
			addrSummary.append(address.getAddressLine1());
		if (address.getAddressLine2() != null && !address.getAddressLine2().isBlank())
			addrSummary.append(", ").append(address.getAddressLine2());
		if (address.getCity() != null && !address.getCity().isBlank())
			addrSummary.append(", ").append(address.getCity());
		if (address.getState() != null && !address.getState().isBlank())
			addrSummary.append(", ").append(address.getState());
		if (address.getPostalCode() != null && !address.getPostalCode().isBlank())
			addrSummary.append(" ").append(address.getPostalCode());
		dto.setDeliveryAddressSummary(addrSummary.toString());

		// ── Shipment / Delivery Info ─────────────────────────────────────────
		List<ShippingEO> shipments = shippingRepository.findByOrder(order);
		if (shipments != null && !shipments.isEmpty()) {
			// Use the most-recently created FORWARD shipment if multiple exist
			ShippingEO shipment = shipments.stream()
				.filter(s -> !"RETURN_PICKUP".equals(s.getType()))
				.reduce((first, second) -> second) // last in list = most recent
				.orElse(shipments.get(0));

			dto.setAwbCode(shipment.getAwb());
			dto.setTrackOrderUrl(shipment.getTrackUrl());

			// Format estimated delivery range: estimatedDeliveryDate →
			// expectedDeliveryDate
			DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
			if (shipment.getEstimatedDeliveryDate() != null) {
				String start = shipment.getEstimatedDeliveryDate().format(displayFmt);
				if (shipment.getExpectedDeliveryDate() != null && !shipment.getExpectedDeliveryDate()
					.toLocalDate()
					.equals(shipment.getEstimatedDeliveryDate().toLocalDate())) {
					String end = shipment.getExpectedDeliveryDate().format(displayFmt);
					dto.setEstimatedDelivery(start + " – " + end);
				}
				else {
					dto.setEstimatedDelivery(start);
				}
			}
			else if (shipment.getExpectedDeliveryDate() != null) {
				dto.setEstimatedDelivery(shipment.getExpectedDeliveryDate().format(displayFmt));
			}
		}

		// ── Order Items ──────────────────────────────────────────────────────
		// N+1 fix: batch-load all product images in a single query, then group by variant
		// ID
		List<ProductVariantEO> itemVariants = orderItems.stream()
			.map(OrderItemEO::getProductVar)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());

		Map<Integer, String> firstImageByVariantId = productImageRepository.findByProductVarIn(itemVariants)
			.stream()
			.filter(img -> img.getProductVar() != null)
			.collect(Collectors.toMap(img -> img.getProductVar().getId(), ProductImageEO::getImage, (a, b) -> a)); // keep
																													// the
																													// first
																													// image
																													// when
																													// duplicates
																													// exist

		List<OrderStatusProd> products = orderItems.stream().map(item -> {
			OrderStatusProd prod = new OrderStatusProd();
			ProductVariantEO pv = item.getProductVar();
			if (pv != null) {
				prod.setProductId(String.valueOf(pv.getId()));
				prod.setMainImagePath(firstImageByVariantId.get(pv.getId()));
			}
			prod.setTitle(item.getProductVarName());
			prod.setQuantity(item.getQuantity());
			return prod;
		}).collect(Collectors.toList());

		dto.setProducts(products);

		/**
		 * // send notification event to kafka String email =
		 * order.getCustomer().getEmail(); String mobile =
		 * order.getCustomer().getMobileNumber(); String customerName =
		 * order.getCustomer().getFirstName(); Double amount =
		 * order.getTotalAmount().doubleValue(); String emailsubject = "Order Status
		 * Update - " + order.getOrderNumber(); String emailMessage = String.format( "Hi
		 * %s,\n\nYour order with order number %s is currently in %s status.\nOrder
		 * Amount: %.2f\n\nThank you for shopping with us!", customerName,
		 * order.getOrderNumber(), order.getOrderStatus(), amount ); String smsSubject =
		 * "Order Status Update"; String smsMessage = String.format( "Order #%s is now %s.
		 * Amount: %.2f. Thank you, %s!", order.getOrderNumber(), order.getOrderStatus(),
		 * amount, customerName );
		 *
		 * // Build EmailDetails for the Order Status Update template
		 * com.user.communication.event.EmailDetails emailDetails =
		 * com.user.communication.event.EmailDetails.builder()
		 * .orderId(order.getOrderNumber()) .customerName(customerName)
		 * .orderStatus(order.getOrderStatus()) .trackingNumber(dto.getAwbCode())
		 * .expectedDelivery(dto.getEstimatedDelivery())
		 * .trackingUrl(dto.getTrackOrderUrl()) .build();
		 *
		 * // Build Event directly (no ObjectMapper) and attach EmailDetails
		 * com.user.communication.event.Event notificationEvent =
		 * com.user.communication.event.Event.builder() .email(email) .mobile(mobile)
		 * .purpose(Constants.COMMUNICATION_PURPOSE_ORDER_CONFIRMATION)
		 * .emailSubject(emailsubject) .emailMessage(emailMessage) .smsSubject(smsSubject)
		 * .smsMessage(smsMessage) .channel(Constants.COMMUNICATION_CHANNEL_BOTH)
		 * .emailDetails(emailDetails) .build();
		 *
		 * kafkaTemplate.send(Constants.CUSTOMER_EVENTS_TOPIC, notificationEvent);
		 */

		return dto;
	}

	@Override
	public OrderDTO getOrderByOrderNumber(String orderId) {
		OrderEO order = orderRepository.findByOrderNumber(orderId)
			.orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

		return UserMapper.toOrderDTO(order);
	}

	@Override
	public OrderDTO updateOrder(String orderId, OrderCreateDTO orderUpdateDTO) {
		// 1) load order by order number (or change to findById if needed)
		OrderEO existingOrder = orderRepository.findByOrderNumber(orderId)
			.orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

		// 2) update customer details
		CustomerEO customer = existingOrder.getCustomer();
		if (customer == null) {
			throw new RuntimeException("Customer not linked to order: " + orderId);
		}

		if (orderUpdateDTO.getName() != null) {
			customer.setFirstName(orderUpdateDTO.getName());
		}
		if (orderUpdateDTO.getEmail() != null) {
			customer.setEmail(orderUpdateDTO.getEmail());
		}
		if (orderUpdateDTO.getPhone() != null) {
			customer.setMobileNumber(orderUpdateDTO.getPhone());
		}

		customerRepository.save(customer);

		// 4) update order details
		if (orderUpdateDTO.getStatus() != null) {
			existingOrder.setOrderStatus(orderUpdateDTO.getStatus());
		}

		OrderEO updatedOrder = orderRepository.save(existingOrder);
		return UserMapper.toOrderDTO(updatedOrder);
	}

	@Override
	public OrderResponseDTO updateOrderStatus(String orderId, String status) {
		Long id = Long.valueOf(orderId);
		OrderEO existingOrder = orderRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

		existingOrder.setOrderStatus(status);
		OrderEO updatedOrder = orderRepository.save(existingOrder);

		return OrderResponseDTO.builder()
			.orderNumber(updatedOrder.getOrderNumber())
			.message("Order status updated successfully")
			.status("success")
			.build();
	}

	@Override
	public OrderHistoryResponseDTO getOrderHistory(OrderHistoryRequestDTO orderHistoryRequestDTO) {

		OrderHistoryResponseDTO returnobject = new OrderHistoryResponseDTO();
		List<OrderDetailsDTO> orders = new ArrayList<>();

		String searchstring = orderHistoryRequestDTO.getSearch() != null
				? orderHistoryRequestDTO.getSearch().toLowerCase() : null;
		String orderid = isValidOrderId(searchstring) ? searchstring : null;
		String productname = isValidOrderId(searchstring) ? null : searchstring;
		try {
			UserEO existingUser = null;
			if (orderHistoryRequestDTO.getUserId() != null) {
				existingUser = userRepository.findById(orderHistoryRequestDTO.getUserId()).orElse(null);
			}
			if (existingUser != null && existingUser.getStatus().equals(Constants.STATUS_ACTIVE)) {
				CustomerEO customer = customerRepository.findByUser(existingUser).orElse(null);

				if (customer != null) {
					List<OrderEO> orderEOList = null;
					if (orderid != null)
						orderEOList = orderRepository.findByCustomerAndOrderNumber(customer, orderid);
					else
						orderEOList = orderRepository.findByCustomer(customer);

					for (OrderEO order : orderEOList) {
						OrderDetailsDTO dto = new OrderDetailsDTO();
						List<OrderItemEO> orderItems = orderProductRepository.findByOrder(order);

						// Fetch customer address
						OrderAddressEO address = orderAddressRepository.findByOrder(order)
							.orElseThrow(() -> new RuntimeException("Order address not found for order: "));
						// Fetch payment
						PaymentEO payment = paymentRepository.findByOrder(order).orElse(null);
						List<ShippingEO> shipping = shippingRepository.findByOrder(order);
						List<OrderShippingDTO> shippinglist = new ArrayList<>();
						for (ShippingEO ship : shipping) {
							OrderShippingDTO shippingDTO = new OrderShippingDTO();
							shippingDTO.setShipmentId(ship.getShipmentId());
							shippingDTO.setTrackingNumber(ship.getTrackingNumber());
							shippingDTO.setAwb(ship.getAwb());

							shippinglist.add(shippingDTO);
						}
						dto.setShippingProducts(shippinglist);
						// order info
						dto.setOrderNumber(order.getOrderNumber());
						dto.setStatus(order.getOrderStatus());
						dto.setCurrency(order.getCurrency());
						dto.setOrderId(order.getOrderId() + "");
						dto.setOrderDate(order.getCreatedAt());
						OrderAddressDTO shippingAddress = new OrderAddressDTO();
						shippingAddress.setName(address.getRecipientName());
						shippingAddress.setAddress1(address.getAddressLine1());
						shippingAddress.setAddress2(address.getAddressLine2());
						shippingAddress.setLandmark(address.getLandMark());
						shippingAddress.setCity(address.getCity());
						shippingAddress.setState(address.getState());
						shippingAddress.setPostalCode(address.getPostalCode());
						shippingAddress.setCountry(address.getCountry());
						dto.setShippingAddress(shippingAddress);
						// address info
						dto.setTotalAmount(payment.getAmount());

						List<OrderStatusProd> products = orderItems.stream().map(item -> {
							OrderStatusProd prod = new OrderStatusProd();
							prod.setProductId(item.getProductVar().getId() + "");
							prod.setTitle(item.getProductVarName());
							prod.setQuantity(item.getQuantity());
							if (item.getProductVar() != null) {
								List<ProductImageEO> images = productImageRepository
									.findByProductVar(item.getProductVar());
								if (images != null && !images.isEmpty()) {
									prod.setMainImagePath(images.get(0).getImage());
								}
								ReturnPolicyDetailDTO returnPolicy = getReturnPolicyByProductVariantId(
										item.getProductVar().getId().longValue());
								if (returnPolicy != null && returnPolicy.getIsReturnable() != null) {
									prod.setIsReturnable(returnPolicy.getIsReturnable() ? "Y" : "N");
									prod.setReturnPolicy(returnPolicy);
								}
								else {
									prod.setIsReturnable("N");
								}
							}
							return prod;
						}).collect(Collectors.toList());

						dto.setProducts(products);
						boolean matchesSearch = true;
						if (productname != null) {
							boolean productMatch = filterOrderItemsByProductName(orderItems, productname).size() > 0;
							boolean addressMatch = matchesAddress(address, productname);
							matchesSearch = productMatch || addressMatch;
						}
						if (matchesSearch) {
							if (orderHistoryRequestDTO.getStatus() != null
									&& orderHistoryRequestDTO.getStatus().size() > 0) {
								if (orderHistoryRequestDTO.getStatus().contains(order.getOrderStatus()))
									orders.add(dto);
							}
							else {
								orders.add(dto);
							}
						}
					}

				}
			}
			returnobject.setOrders(orders);
		}
		catch (Exception e) {
			logger.error("Error fetching order history for userId: {}, search: {}, status: {}",
					orderHistoryRequestDTO.getUserId(), orderHistoryRequestDTO.getSearch(),
					orderHistoryRequestDTO.getStatus(), e);

		}
		return returnobject;
	}

	@Override
	@org.springframework.transaction.annotation.Transactional
	public ResponseDTO updateOrderPaymentStatus(PaymentStatusUpdateDTO paymentStatusUpdateDTO) {
		ResponseDTO responseDTO = new ResponseDTO();
		String razorpayOrderId = paymentStatusUpdateDTO.getRazorpayOrderId();
		String razorpayPaymentId = paymentStatusUpdateDTO.getRazorpayPaymentId();
		String paymentStatus = paymentStatusUpdateDTO.getPaymentStatus();

		// Update payment status in payment table
		PaymentEO payment = paymentRepository.findByPaymentProviderOrderId(razorpayOrderId);
		if (payment != null) {
			payment.setPaymentStatus(paymentStatus);
			payment.setTransactionId(razorpayPaymentId);
			paymentRepository.save(payment);

			// Update order payment status (same transaction)
			OrderEO order = payment.getOrder();

			if (Constants.ORDER_PAYMENT_STATUS_PAID.equals(paymentStatus)) {
				// Payment succeeded – confirm the order
				order.setPaymentStatus(Constants.ORDER_PAYMENT_STATUS_PAID);
				order.setOrderStatus(Constants.ORDER_STATUS_CONFIRMED);
				orderRepository.save(order);

				// ── Async: Razorpay capture (best-effort, does not block response) ──
				final BigDecimal captureAmount = payment.getAmount();
				final String capturePaymentId = razorpayPaymentId;
				java.util.concurrent.CompletableFuture.runAsync(() -> {
					try {
						JSONObject captureRequest = new JSONObject();
						captureRequest.put("amount", captureAmount.multiply(BigDecimal.valueOf(100)).longValue());
						captureRequest.put("currency", "INR");
						razorpayClient.payments.capture(capturePaymentId, captureRequest);
						logger.info("Razorpay payment captured successfully for paymentId={}", capturePaymentId);
					}
					catch (Exception captureEx) {
						// If already captured (auto-capture was on), Razorpay returns an
						// error — safe to ignore
						logger.warn("Razorpay capture attempt for paymentId={}: {} (may already be captured)",
								capturePaymentId, captureEx.getMessage());
					}
				});

				// ── Async: trigger shipment creation (does not block response) ──
				OrderEvent shipmentEvent = OrderEvent.builder()
					.orderId(order.getOrderId() != null ? order.getOrderId().longValue() : null)
					.eventType(Constants.ORDER_EVENT_TYPE_SHIPPED)
					.build();
				if (shipmentEvent.getOrderId() != null) {
					shippingService.processCreateShipmentEvent(shipmentEvent);
					logger.info("Shipment creation async-triggered for orderId={}", shipmentEvent.getOrderId());
				}
			}
			else {
				// Payment failed – mark order as payment-failed so retry is still allowed
				order.setPaymentStatus(paymentStatus);
				order.setOrderStatus(Constants.ORDER_STATUS_PAYMENT_FAILED);
				orderRepository.save(order);
				logger.warn("Payment failed for orderId={}, orderStatus set to PAYMENT FAILED. No shipment event sent.",
						order.getOrderId());
			}

			responseDTO.setResponseMessage("Payment status updated successfully");
			responseDTO.setResponseStatus("success");
		}
		else {
			responseDTO.setResponseMessage("Payment not found for order: " + razorpayOrderId);
			responseDTO.setResponseStatus("failed");
		}

		return responseDTO;

	}

	@Override
	public ResponseDTO cancelOrder(OrderCancelRequestDTO orderCancelRequestDTO) {
		ResponseDTO response = new ResponseDTO();
		try {
			logger.info("Starting cancelOrder for" + orderCancelRequestDTO.getOrderNumber());
			// Find the order by order number
			OrderEO order = orderRepository.findByOrderNumber(orderCancelRequestDTO.getOrderNumber()).orElse(null);
			if (order == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage(Constants.ORDER_NOT_ELIGIBLE_FOR_CANCEL);
				logger.warn("Order not found for orderNumber={}", orderCancelRequestDTO.getOrderNumber());
				return response;
			}

			List<OrderItemEO> orderItems = orderProductRepository.findByOrder(order);
			// For each item in the cancel request, create a cancel request EO
			// Here getOrderItems is Product id
			for (OrderItemEO orderItem : orderItems) {

				orderItem.setStatus(Constants.ORDER_STATUS_CANCELLED);
				orderProductRepository.save(orderItem);

			}
			// Resolve reasonCode → reasonDescription from reason_master
			String cancelReasonDescription = orderCancelRequestDTO.getReasonCode();
			if (cancelReasonDescription != null) {
				ReasonMasterEO reasonMaster = reasonMasterRepository.findByReasonCode(cancelReasonDescription)
					.orElse(null);
				if (reasonMaster != null) {
					cancelReasonDescription = reasonMaster.getReasonDescription();
				}
			}

			OrderCancelRequestEO cancelRequest = OrderCancelRequestEO.builder()
				.order(order)
				.reason(cancelReasonDescription)
				.comment(orderCancelRequestDTO.getComment())
				.status(Constants.ORDER_CANCEL_REQUEST_STATUS_REQUESTED)
				.requestedAt(LocalDateTime.now())
				.build();
			OrderCancelRequestEO savedorderCancelRequestEO = orderCancelRequestRepository.save(cancelRequest);
			order.setOrderStatus(Constants.ORDER_STATUS_CANCELLED);

			orderRepository.save(order);

			List<ShippingEO> shipping = shippingRepository.findByOrder(order);

			ShippingEO ship = shipping.getFirst();
			ship.setShipmentStatus(Constants.SHIPMENT_STATUS_CANCELLED);
			shippingRepository.save(ship);

			ShipmentTrackingHistoryEO shipmentTrackingHistoryEO = new ShipmentTrackingHistoryEO();
			shipmentTrackingHistoryEO.setShipment(ship);
			shipmentTrackingHistoryEO.setStatus(Constants.SHIPMENT_STATUS_CANCELLED);
			shipmentTrackingHistoryEO.setRemarks("Order Cancelled ");
			shipmentTrackingHistoryEO.setUpdatedAt(LocalDateTime.now());
			shipmentTrackingHistoryRepository.save(shipmentTrackingHistoryEO);

			// ── Invoke Shiprocket Cancel Order API ──────────────────────────
			try {
				Integer shiprocketOrderId = ship.getShipOrderId();
				if (shiprocketOrderId != null) {
					Map shiprocketCancelResponse = shiprocketService.cancelOrder(List.of(shiprocketOrderId));
					logger.info("Shiprocket cancelOrder response for orderNumber={}: {}",
							orderCancelRequestDTO.getOrderNumber(), shiprocketCancelResponse);

					// Parse response and update OrderCancelRequestEO
					String srStatus = "UNKNOWN";
					String srMessage = null;
					if (shiprocketCancelResponse != null) {
						Object msgObj = shiprocketCancelResponse.get("message");
						if (msgObj instanceof String) {
							srMessage = (String) msgObj;
						}
						// Shiprocket returns HTTP 200 with a "message" on success
						// and may include "status" field (200 / 4xx)
						Object statusObj = shiprocketCancelResponse.get("status");
						if (statusObj instanceof Number) {
							int statusCode = ((Number) statusObj).intValue();
							srStatus = (statusCode >= 200 && statusCode < 300) ? "SUCCESS" : "FAILED";
						}
						else {
							// If no explicit status, treat presence of message as success
							srStatus = srMessage != null ? "SUCCESS" : "FAILED";
						}
					}
					savedorderCancelRequestEO.setShiprocketCancelStatus(srStatus);
					savedorderCancelRequestEO.setShiprocketCancelMessage(srMessage);
					savedorderCancelRequestEO.setShiprocketCancelledAt(LocalDateTime.now());
					orderCancelRequestRepository.save(savedorderCancelRequestEO);
					logger.info("Shiprocket cancel status={} message={} for orderNumber={}", srStatus, srMessage,
							orderCancelRequestDTO.getOrderNumber());
				}
				else {
					logger.warn("shipOrderId is null for shipmentId={}, skipping Shiprocket cancel",
							ship.getShipmentId());
					savedorderCancelRequestEO.setShiprocketCancelStatus("SKIPPED");
					savedorderCancelRequestEO.setShiprocketCancelMessage("Shiprocket order ID not available");
					savedorderCancelRequestEO.setShiprocketCancelledAt(LocalDateTime.now());
					orderCancelRequestRepository.save(savedorderCancelRequestEO);
				}
			}
			catch (Exception ex) {
				logger.error("Shiprocket cancelOrder API call failed for orderNumber={}: {}",
						orderCancelRequestDTO.getOrderNumber(), ex.getMessage(), ex);
				savedorderCancelRequestEO.setShiprocketCancelStatus("FAILED");
				savedorderCancelRequestEO.setShiprocketCancelMessage(ex.getMessage());
				savedorderCancelRequestEO.setShiprocketCancelledAt(LocalDateTime.now());
				orderCancelRequestRepository.save(savedorderCancelRequestEO);
				// Non-fatal — internal cancel already done, don't roll back
			}
			// ────────────────────────────────────────────────────────────────

			// Directly process cancel order event
			OrderEvent event = OrderEvent.builder()
				.orderId(order.getOrderId() != null ? order.getOrderId().longValue() : null)
				.eventType(Constants.ORDER_EVENT_TYPE_CANCELLED)
				.build();
			if (event.getOrderId() != null) {
				this.processCancelOrderEvent(event);
			}

			response.setResponseStatus(Constants.STATUS_SUCCESS);
			response.setResponseMessage(Constants.ORDER_CANCEL_SUCCESSFUL);
		}
		catch (Exception e) {
			logger.error("Error in cancelOrder for orderNumber={}",
					orderCancelRequestDTO != null ? orderCancelRequestDTO.getOrderNumber() : null, e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage(Constants.ORDER_NOT_ELIGIBLE_FOR_CANCEL);

		}
		return response;
	}

	/**
	 * This method processes the cancel order event, updates inventory, triggers refund if
	 * needed, and sends notifications.
	 * @param event
	 */
	@Override
	public void processCancelOrderEvent(OrderEvent event) {

		try {
			if (event == null || event.getOrderId() == null
					|| !Constants.ORDER_EVENT_TYPE_CANCELLED.equals(event.getEventType())) {
				return;
			}
			OrderEO order = orderRepository.findById(event.getOrderId().longValue()).orElse(null);
			if (order == null) {
				return;
			}
			OrderCancelRequestEO cancelRequest = orderCancelRequestRepository.findByOrder(order)
				.stream()
				.findFirst()
				.orElse(null);

			// start cancel order process from here
			if (cancelRequest != null) {
				// 1. Check Order Status
				// fetch Order by id to get order id and then fetch order
				OrderEO order1 = cancelRequest.getOrder();
				if (order1 != null) {

					// 2. Check Shipment Status (if needed, pseudo-code, implement as per
					// your model)
					List<ShippingEO> shipments = shippingRepository.findByOrder(order1);
					boolean isShipped = shipments.stream()
						.anyMatch(s -> s.getShipmentStatus() != null && !s.getShipmentStatus().equals("CREATED"));
					if (isShipped) {
						// Optionally handle if already shipped
					}
					List<OrderItemEO> orderItems = orderProductRepository.findByOrder(order1);

					// 3. Update Inventory (increase stock for cancelled item)
					for (OrderItemEO orderItem : orderItems) {
						ProductVariantEO productVariant = orderItem.getProductVar();
						if (productVariant != null) {
							InventoryEO inventory = inventoryRepository.findByProductVariant(productVariant);
							inventory.setAvailableQty(inventory.getAvailableQty() + orderItem.getQuantity());
							inventory.setTotalQty((inventory.getTotalQty() != null ? inventory.getTotalQty() : 0)
									+ orderItem.getQuantity());
							inventoryRepository.save(inventory);
						}
					}

					// 5. Process Refund (if prepaid)
					if (order1.getPaymentStatus() != null
							&& order1.getPaymentStatus().equals(Constants.ORDER_PAYMENT_STATUS_PAID)) {
						// Get payment transaction
						PaymentEO payment = paymentRepository.findByOrder(order).orElse(null);
						if (payment == null) {
							return;
						}
						// Build RefundTransactionEO
						RefundTransactionEO refundtr = RefundTransactionEO.builder()
							.orderId(order.getOrderId().longValue())
							.cancelRequestId(cancelRequest.getId())
							.paymentTransactionId(payment.getPaymentId().longValue())
							.refundReference("REFUND-" + order.getOrderNumber() + "-" + System.currentTimeMillis())
							.refundType("FULL")
							.refundReason(cancelRequest.getReason())
							.requestedAmount(order.getTotalAmount())
							.currency(order.getCurrency() != null ? order.getCurrency() : "INR")
							.status("INITIATED")
							.initiatedAt(LocalDateTime.now())
							.createdAt(LocalDateTime.now())
							.updatedAt(LocalDateTime.now())
							.build();
						refundTransactionRepository.save(refundtr);

						/**
						 * BigDecimal amount = order.getTotalAmount(); JSONObject
						 * refundRequest = new JSONObject(); if (amount != null) { //
						 * Razorpay expects amount in paise refundRequest.put("amount",
						 * amount.multiply(BigDecimal.valueOf(100)).intValue());
						 * refundRequest.put("receipt", refundtr.getRefundReference()); }
						 * Map<String, Object> notes = generateRefundNotes(cancelRequest,
						 * refundtr); if (notes != null && !notes.isEmpty()) { JSONObject
						 * notesObj = new JSONObject(notes); refundRequest.put("notes",
						 * notesObj); } String paymentId = payment.getTransactionId(); try
						 * { Refund refund = razorpayClient.payments.refund(paymentId,
						 * refundRequest); if (refund != null) {
						 * refundtr.setGatewayRefundId(refund.get("id"));
						 * refundtr.setStatus("SUCCESS"); } else {
						 * refundtr.setStatus("FAILED"); refundtr.setFailureReason("Refund
						 * failed via Razorpay API"); } } catch (Exception e) {
						 * refundtr.setStatus("FAILED");
						 * refundtr.setFailureReason("Exception during refund: " +
						 * e.getMessage()); } refundTransactionRepository.save(refundtr);
						 */

						// 6. Send Notification using Order Cancel template
						CustomerEO customer = order.getCustomer();
						if (customer != null) {
							String email = customer.getEmail();
							String mobile = customer.getMobileNumber();
							String customerName = customer.getFirstName();
							String orderNumber = order.getOrderNumber();
							String amountStr = refundtr.getRequestedAmount() != null
									? refundtr.getRequestedAmount().toPlainString() : "0";
							String smsMessage = String.format(
									"Your order %s has been cancelled. Refund of %s will be processed in 7 business days.",
									orderNumber, amountStr);

							com.user.communication.event.EmailDetails emailDetails = com.user.communication.event.EmailDetails
								.builder()
								.customerName(customerName)
								.orderId(orderNumber)
								.updateType("Order Cancellation")
								.status(order.getOrderStatus())
								.amount(amountStr)
								.message(cancelRequest.getReason() != null ? cancelRequest.getReason()
										: "Your order has been cancelled.")
								.refundDays("7")
								.build();

							if (email != null && !email.isBlank()) {
								Event cancelEvent = Event.builder()
									.email(email)
									.mobile(mobile)
									.purpose(Constants.COMMUNICATION_PURPOSE_REFUND)
									.emailSubject("Order Cancellation Update - " + orderNumber)
									.smsSubject("Order Cancelled")
									.smsMessage(smsMessage)
									.channel(Constants.COMMUNICATION_CHANNEL_BOTH)
									.templateId(Constants.MSG91_EMAIL_TEMPLATE_ORDER_CANCEL)
									.emailDetails(emailDetails)
									.build();
								notificationService.processEvent(cancelEvent);
								logger.info("Cancel email event sent for orderNumber={}", orderNumber);
							}
						}

					}

					// Update cancel request status
					cancelRequest.setStatus(Constants.ORDER_CANCEL_REQUEST_STATUS_APPROVED);
					cancelRequest.setProcessedAt(LocalDateTime.now());
					orderCancelRequestRepository.save(cancelRequest);
				}

			}
		}
		catch (Exception e) {
			logger.error("Error processing cancel order event for orderId: {}",
					event != null ? event.getOrderId() : null, e);
		}

	}

	@Override
	public ResponseDTO createReason(CreateReasonRequestDTO createReasonRequestDTO) {
		ResponseDTO response = new ResponseDTO();
		try {
			ReasonMasterEO reason = ReasonMasterEO.builder()
				.reasonCode(createReasonRequestDTO.getReasonCode())
				.reasonDescription(createReasonRequestDTO.getReasonDescription())
				.status(Constants.STATUS_ACTIVE)
				.type(createReasonRequestDTO.getType())
				.build();
			reasonMasterRepository.save(reason);
			response.setResponseStatus("success");
			response.setResponseMessage("Reason created successfully");
		}
		catch (Exception e) {
			response.setResponseStatus("failed");
			response.setResponseMessage("Failed to create reason: " + e.getMessage());
		}
		return response;
	}

	@Override
	public List<ReasonDetailsDTO> getReasonsByType(String type) {
		List<ReasonDetailsDTO> response = new ArrayList<>();

		List<ReasonMasterEO> list = reasonMasterRepository.findByType(type);
		for (ReasonMasterEO reasonMasterEO : list) {
			ReasonDetailsDTO dto = ReasonDetailsDTO.builder()
				.reasonCode(reasonMasterEO.getReasonCode())
				.reasonDescription(reasonMasterEO.getReasonDescription())
				.type(reasonMasterEO.getType())
				.build();
			response.add(dto);
		}
		return response;
	}

	private Map<String, Object> generateRefundNotes(OrderCancelRequestEO cancelRequest,
			RefundTransactionEO refundTransaction) {
		Map<String, Object> notes = new HashMap<>();
		notes.put("order_id", String.valueOf(refundTransaction.getOrderId()));
		notes.put("refund_transaction_id", String.valueOf(refundTransaction.getId()));
		notes.put("refund_reason",
				refundTransaction.getRefundReason() != null ? refundTransaction.getRefundReason() : "");
		// Razorpay notes values must all be Strings — convert BigDecimal to string
		notes.put("requested_amount", refundTransaction.getRequestedAmount() != null
				? refundTransaction.getRequestedAmount().toPlainString() : "0");
		return notes;
	}

	@Override
	public ResponseDTO returnOrder(ReturnOrderRequestDTO returnOrderRequestDTO, List<MultipartFile> images) {
		ResponseDTO response = new ResponseDTO();
		try {
			logger.info("Starting returnOrder for orderNumber={}, userId={}", returnOrderRequestDTO.getOrderNumber(),
					returnOrderRequestDTO.getUserId());

			// Step 1: Validate required fields
			if (returnOrderRequestDTO.getOrderNumber() == null
					|| returnOrderRequestDTO.getOrderNumber().trim().isEmpty()) {
				logger.warn("returnOrder called without orderNumber");
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Order number is required");
				return response;
			}
			if (returnOrderRequestDTO.getUserId() == null) {
				logger.warn("returnOrder called without userId");
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("User ID is required");
				return response;
			}

			// Step 2: Fetch the order by order number
			OrderEO order = orderRepository.findByOrderNumber(returnOrderRequestDTO.getOrderNumber()).orElse(null);
			if (order == null) {
				logger.warn("Order not found for orderNumber={}", returnOrderRequestDTO.getOrderNumber());
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Order not found: " + returnOrderRequestDTO.getOrderNumber());
				return response;
			}

			// Step 3: Fetch the user
			UserEO user = userRepository.findById(Long.valueOf(returnOrderRequestDTO.getUserId())).orElse(null);
			if (user == null) {
				logger.warn("User not found for userId={}", returnOrderRequestDTO.getUserId());
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("User not found: " + returnOrderRequestDTO.getUserId());
				return response;
			}

			// Step 4: Fetch all order items under this order
			List<OrderItemEO> orderItems = orderProductRepository.findByOrder(order);
			if (orderItems == null || orderItems.isEmpty()) {
				logger.warn("No order items found for orderNumber={}", returnOrderRequestDTO.getOrderNumber());
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response
					.setResponseMessage("No order items found for order: " + returnOrderRequestDTO.getOrderNumber());
				return response;
			}

			// Step 5: Create ONE ReturnRequest at Order level
			String returnId = "RET-" + order.getOrderNumber() + "-" + System.currentTimeMillis();
			ReturnRequestEO returnRequest = new ReturnRequestEO();
			returnRequest.setReturnId(returnId);
			returnRequest.setOrder(order);
			returnRequest.setUser(user);
			returnRequest.setReturnType(Constants.RETURN_TYPE_RETURN);
			returnRequest.setReasonCode(returnOrderRequestDTO.getReasonCode());
			returnRequest.setStatus(ReturnStatus.REQUESTED.name());
			// returnRequest.setCarrier(Constants.COURIER_NAME);
			returnRequest.setUserComments(returnOrderRequestDTO.getComments());
			ReturnRequestEO savedReturn = returnRequestRepository.save(returnRequest);
			logger.info("Saved ReturnRequest returnId={} at order level for orderNumber={}", savedReturn.getReturnId(),
					order.getOrderNumber());

			// Step 6: Save return status history once
			ReturnStatusHistoryEO returnStatusHistory = new ReturnStatusHistoryEO();
			returnStatusHistory.setReturnRequest(savedReturn);
			returnStatusHistory.setNewStatus(ReturnStatus.REQUESTED.name());
			returnStatusHistory
				.setRemarks("Return request created for order: " + returnOrderRequestDTO.getOrderNumber());
			returnStatusHistory.setChangedAt(java.time.LocalDateTime.now());
			returnStatusHistoryRepository.save(returnStatusHistory);

			// Step 7: Update all order items status to "Return Requested"
			logger.info("Updating {} order item(s) status to '{}' for orderNumber={}", orderItems.size(),
					Constants.ORDER_STATUS_RETURN_REQUESTED, order.getOrderNumber());
			for (OrderItemEO orderItem : orderItems) {
				orderItem.setStatus(Constants.ORDER_STATUS_RETURN_REQUESTED);
				orderProductRepository.save(orderItem);
				logger.info("Updated orderItem id={} status to '{}'", orderItem.getOrderItemId(),
						Constants.ORDER_STATUS_RETURN_REQUESTED);
			}

			// Step 8: Update order-level status to "Return Requested"
			order.setOrderStatus(Constants.ORDER_STATUS_RETURN_REQUESTED);
			orderRepository.save(order);
			logger.info("Updated order id={} orderNumber={} status to '{}'", order.getOrderId(), order.getOrderNumber(),
					Constants.ORDER_STATUS_RETURN_REQUESTED);

			// Step 9: Save uploaded images linked to the single return request
			if (images != null && !images.isEmpty()) {
				for (MultipartFile image : images) {
					try {
						Map<String, String> uploadResult = cloudinaryMediaService.uploadImage(image,
								"kuchimittai/returns");
						ReturnImageEO returnImage = new ReturnImageEO();
						returnImage.setReturnRequest(savedReturn);
						returnImage.setImageUrl(uploadResult.get("secure_url"));
						returnImageRepository.save(returnImage);
						logger.info("Saved return image to Cloudinary for returnId={}", savedReturn.getReturnId());
					}
					catch (Exception e) {
						logger.error("Failed to upload return image for returnId={}: {}", savedReturn.getReturnId(),
								e.getMessage(), e);
					}
				}
			}

			// Step 9.5: Update ShippingEO and ShipmentTrackingHistoryEO to "Return
			// Requested"
			List<ShippingEO> shipments = shippingRepository.findByOrder(order);
			if (shipments != null && !shipments.isEmpty()) {
				for (ShippingEO shipment : shipments) {
					shipment.setShipmentStatus(Constants.SHIPMENT_STATUS_RETURN_REQUESTED);
					shippingRepository.save(shipment);
					logger.info("Updated ShippingEO shipmentId={} status to '{}'", shipment.getShipmentId(),
							Constants.SHIPMENT_STATUS_RETURN_REQUESTED);

					ShipmentTrackingHistoryEO trackingHistory = new ShipmentTrackingHistoryEO();
					trackingHistory.setShipment(shipment);
					trackingHistory.setStatus(Constants.SHIPMENT_STATUS_RETURN_REQUESTED);
					trackingHistory.setRemarks("Return requested for order: " + order.getOrderNumber());
					trackingHistory.setUpdatedAt(LocalDateTime.now());
					shipmentTrackingHistoryRepository.save(trackingHistory);
					logger.info("Saved ShipmentTrackingHistory for shipmentId={} with status '{}'",
							shipment.getShipmentId(), Constants.SHIPMENT_STATUS_RETURN_REQUESTED);
				}
			}
			else {
				logger.warn("No shipment found for orderNumber={} while updating return status",
						order.getOrderNumber());
			}

			response.setResponseStatus(Constants.STATUS_SUCCESS);
			response.setResponseMessage(
					"Return request created successfully for order: " + returnOrderRequestDTO.getOrderNumber());

		}
		catch (Exception e) {
			logger.error("Error in returnOrder for orderNumber={}: {}",
					returnOrderRequestDTO != null ? returnOrderRequestDTO.getOrderNumber() : null, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to create return request. Please contact support.");
		}
		return response;

	}

	@Override
	public void processReturnOrderEvent(OrderEvent event) {
		if (event == null || event.getOrderId() == null
				|| !Constants.ORDER_EVENT_TYPE_RETURN_REQUEST.equals(event.getEventType())) {
			logger.warn("processReturnOrderEvent: invalid or non-return event, skipping. event={}", event);
			return;
		}

		try {
			logger.info("Processing return order event for orderId={}", event.getOrderId());

			// Step 1: Fetch the order
			OrderEO order = orderRepository.findById(event.getOrderId()).orElse(null);
			if (order == null) {
				logger.warn("processReturnOrderEvent: order not found for orderId={}", event.getOrderId());
				return;
			}

			// Step 2: Update all order items to RETURN_IN_PROGRESS
			List<OrderItemEO> orderItems = orderProductRepository.findByOrder(order);
			for (OrderItemEO orderItem : orderItems) {
				orderItem.setStatus(Constants.ORDER_ITEM_STATUS_RETURN_IN_PROGRESS);
				orderProductRepository.save(orderItem);
			}
			logger.info("Updated {} order item(s) to '{}' for orderId={}", orderItems.size(),
					Constants.ORDER_ITEM_STATUS_RETURN_IN_PROGRESS, order.getOrderId());

			// Step 3: Update order status to RETURN_IN_PROGRESS
			order.setOrderStatus(Constants.ORDER_ITEM_STATUS_RETURN_IN_PROGRESS);
			orderRepository.save(order);
			logger.info("Updated order id={} status to '{}'", order.getOrderId(),
					Constants.ORDER_ITEM_STATUS_RETURN_IN_PROGRESS);

			// Step 4: Fetch original shipment to initiate return pickup
			List<ShippingEO> shipments = shippingRepository.findByOrder(order);
			if (shipments == null || shipments.isEmpty()) {
				logger.warn(
						"processReturnOrderEvent: no shipment found for orderId={}, " + "cannot initiate return pickup",
						event.getOrderId());
				return;
			}

			// Use the first (or most recent) original shipment as reference
			ShippingEO originalShipment = shipments.get(0);
			logger.info("Initiating return pickup using original shipmentId={}, " + "trackingNumber={}, courierName={}",
					originalShipment.getShipmentId(), originalShipment.getTrackingNumber(),
					originalShipment.getCourierName());

			// Fetch warehouse eagerly by ID to avoid LazyInitializationException
			WarehouseEO warehouse = null;
			if (originalShipment.getWarehouse() != null && originalShipment.getWarehouse().getWarehouseId() != null) {
				warehouse = warehouseRepository.findById(originalShipment.getWarehouse().getWarehouseId()).orElse(null);
			}

			// Step 5: Create a new reverse ShippingEO for the return pickup
			String reverseTrackingNumber = "RTN-" + originalShipment.getTrackingNumber();
			ShippingEO returnShipment = new ShippingEO();
			returnShipment.setOrder(order);
			returnShipment.setWarehouse(warehouse);
			returnShipment.setCourierName(originalShipment.getCourierName());
			returnShipment.setTrackingNumber(reverseTrackingNumber);
			returnShipment.setType(Constants.SHIPMENT_TYPE_RETURN_PICKUP);
			returnShipment.setShipmentStatus(Constants.SHIPMENT_STATUS_RETURN_PICKUP_INITIATED);
			ShippingEO savedReturnShipment = shippingRepository.save(returnShipment);
			logger.info("Created return shipment id={} with reverseTrackingNumber={} for orderId={}",
					savedReturnShipment.getShipmentId(), reverseTrackingNumber, order.getOrderId());

			// Step 6: Update original shipment status to RETURN_PICKUP_INITIATED
			originalShipment.setShipmentStatus(Constants.SHIPMENT_STATUS_RETURN_PICKUP_INITIATED);
			shippingRepository.save(originalShipment);
			logger.info("Updated original shipmentId={} status to '{}'", originalShipment.getShipmentId(),
					Constants.SHIPMENT_STATUS_RETURN_PICKUP_INITIATED);

			// Step 7: Save tracking history for the return shipment
			String warehouseLocation = warehouse != null && warehouse.getAddressLine1() != null
					? warehouse.getAddressLine1() : null;
			ShipmentTrackingHistoryEO trackingHistory = new ShipmentTrackingHistoryEO();
			trackingHistory.setShipment(savedReturnShipment);
			trackingHistory.setStatus(Constants.SHIPMENT_STATUS_RETURN_PICKUP_INITIATED);
			trackingHistory.setRemarks(Constants.SHIPMENT_STATUS_RETURN_PICKUP_INITIATED_REMARK);
			trackingHistory.setLocation(warehouseLocation);
			trackingHistory.setUpdatedAt(LocalDateTime.now());
			shipmentTrackingHistoryRepository.save(trackingHistory);
			logger.info("Saved return shipment tracking history for shipmentId={}",
					savedReturnShipment.getShipmentId());

			// Step 8: Save tracking history on original shipment as well
			ShipmentTrackingHistoryEO originalTrackingHistory = new ShipmentTrackingHistoryEO();
			originalTrackingHistory.setShipment(originalShipment);
			originalTrackingHistory.setStatus(Constants.SHIPMENT_STATUS_RETURN_PICKUP_INITIATED);
			originalTrackingHistory.setRemarks("Return pickup initiated. Reverse tracking: " + reverseTrackingNumber);
			originalTrackingHistory.setUpdatedAt(LocalDateTime.now());
			shipmentTrackingHistoryRepository.save(originalTrackingHistory);

			logger.info("Return pickup initiation completed successfully for orderId={}", order.getOrderId());

			// ── Send Return Update email ──────────────────────────────────────
			CustomerEO customer = order.getCustomer();
			if (customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()) {
				String customerName = customer.getFirstName();
				String orderNumber = order.getOrderNumber();
				String amountStr = order.getTotalAmount() != null ? order.getTotalAmount().toPlainString() : "0";
				String smsMessage = String.format(
						"Your return request for order %s has been initiated. We will process it shortly.",
						orderNumber);

				com.user.communication.event.EmailDetails emailDetails = com.user.communication.event.EmailDetails
					.builder()
					.customerName(customerName)
					.orderId(orderNumber)
					.updateType("Return Request")
					.status(Constants.ORDER_ITEM_STATUS_RETURN_IN_PROGRESS)
					.amount(amountStr)
					.message("Your return request has been received and return pickup has been initiated.")
					.refundDays("7")
					.build();

				Event returnEvent = Event.builder()
					.email(customer.getEmail())
					.mobile(customer.getMobileNumber())
					.purpose("RETURN_REQUEST")
					.emailSubject("Return Request Update - " + orderNumber)
					.smsSubject("Return Request")
					.smsMessage(smsMessage)
					.channel(Constants.COMMUNICATION_CHANNEL_BOTH)
					.templateId(Constants.MSG91_EMAIL_TEMPLATE_ORDER_CANCEL)
					.emailDetails(emailDetails)
					.build();

				notificationService.processEvent(returnEvent);
				logger.info("Return email event sent for orderNumber={}", orderNumber);
			}

			// ── Shiprocket Return Order Integration ──────────────────────────────────

			// Step 9: Create Return Order on Shiprocket
			Integer returnShipOrderId = null;
			Integer returnShipmentId = null;
			try {
				OrderAddressEO orderAddress = orderAddressRepository.findByOrder(order).orElse(null);
				List<OrderItemEO> orderItems2 = orderProductRepository.findByOrder(order);

				Map<String, Object> returnOrderRequest = new HashMap<>();
				String returnOrderRef = "RETURN-" + order.getOrderNumber();
				returnOrderRequest.put("order_id", returnOrderRef);
				returnOrderRequest.put("order_date", LocalDate.now().format(DateTimeFormatter.ofPattern("d-M-yyyy")));
				returnOrderRequest.put("channel_id", "10576563");

				// Pickup = customer delivery address
				String custName = order.getCustomer() != null ? order.getCustomer().getFirstName() : "";
				returnOrderRequest.put("pickup_customer_name", custName);
				returnOrderRequest.put("pickup_last_name", "");
				returnOrderRequest.put("pickup_address", orderAddress != null && orderAddress.getAddressLine1() != null
						? orderAddress.getAddressLine1() : "");
				returnOrderRequest.put("pickup_city", orderAddress != null ? orderAddress.getCity() : "");
				returnOrderRequest.put("pickup_pincode", orderAddress != null ? orderAddress.getPostalCode() : "");
				returnOrderRequest.put("pickup_state", orderAddress != null ? orderAddress.getState() : "");
				returnOrderRequest.put("pickup_country", orderAddress != null && orderAddress.getCountry() != null
						&& !orderAddress.getCountry().isEmpty() ? orderAddress.getCountry() : "India");
				returnOrderRequest.put("pickup_email",
						order.getCustomer() != null ? order.getCustomer().getEmail() : "");
				returnOrderRequest.put("pickup_phone",
						order.getCustomer() != null ? order.getCustomer().getMobileNumber() : "");

				// Shipping/Delivery = warehouse address (return destination)
				returnOrderRequest.put("shipping_customer_name",
						warehouse != null && warehouse.getWarehouseName() != null ? warehouse.getWarehouseName()
								: "Warehouse");
				returnOrderRequest.put("shipping_last_name", "");
				returnOrderRequest.put("shipping_address",
						warehouse != null && warehouse.getAddressLine1() != null ? warehouse.getAddressLine1() : "");
				returnOrderRequest.put("shipping_city", warehouse != null ? warehouse.getCity() : "");
				returnOrderRequest.put("shipping_pincode",
						warehouse != null ? warehouse.getPostalCode() : getWarehousePostalCode(null));
				returnOrderRequest.put("shipping_state", warehouse != null ? warehouse.getState() : "");
				returnOrderRequest.put("shipping_country", "India");
				returnOrderRequest.put("shipping_email", "");
				returnOrderRequest.put("shipping_phone",
						warehouse != null && warehouse.getContactNumber() != null ? warehouse.getContactNumber() : "");

				returnOrderRequest.put("payment_method", "Prepaid"); // returns are always
																		// prepaid
				returnOrderRequest.put("sub_total", order.getTotalAmount());

				// Order items
				List<Map<String, Object>> itemsList = new ArrayList<>();
				for (OrderItemEO item : orderItems2) {
					Map<String, Object> itemMap = new HashMap<>();
					itemMap.put("name", item.getProductVar() != null && item.getProductVar().getProduct() != null
							? item.getProductVar().getProduct().getName() : "");
					itemMap.put("sku", item.getProductVar() != null ? item.getProductVar().getSkuCode() : "");
					itemMap.put("units", item.getQuantity());
					itemMap.put("selling_price", item.getUnitPrice());
					itemMap.put("discount", 0);
					itemMap.put("tax", 0);
					itemMap.put("hsn", "");
					itemsList.add(itemMap);
				}
				returnOrderRequest.put("order_items", itemsList);

				// Dimensions — reuse from original forward shipment if available
				returnOrderRequest.put("weight",
						originalShipment.getWeight() != null ? originalShipment.getWeight() : 0.5);
				returnOrderRequest.put("length",
						originalShipment.getLength() != null ? originalShipment.getLength() : 10);
				returnOrderRequest.put("breadth",
						originalShipment.getBreadth() != null ? originalShipment.getBreadth() : 10);
				returnOrderRequest.put("height",
						originalShipment.getHeight() != null ? originalShipment.getHeight() : 10);

				Map srReturnResponse = shiprocketService.createReturnOrder(returnOrderRequest);
				if (srReturnResponse != null) {
					Object ordId = srReturnResponse.get("order_id");
					Object shpId = srReturnResponse.get("shipment_id");
					if (ordId instanceof Number)
						returnShipOrderId = ((Number) ordId).intValue();
					if (shpId instanceof Number)
						returnShipmentId = ((Number) shpId).intValue();
					savedReturnShipment.setShipOrderId(returnShipOrderId);
					savedReturnShipment.setShipShipmentId(returnShipmentId);
					shippingRepository.save(savedReturnShipment);
					logger.info(
							"Step SHIPROCKET_CREATE_RETURN_ORDER SUCCESS: "
									+ "returnOrderId={}, returnShipmentId={} for orderId={}",
							returnShipOrderId, returnShipmentId, order.getOrderId());
				}
				else {
					logger.warn(
							"Step SHIPROCKET_CREATE_RETURN_ORDER: null response from Shiprocket " + "for orderId={}",
							order.getOrderId());
				}
			}
			catch (Exception ex) {
				logger.error("Step SHIPROCKET_CREATE_RETURN_ORDER FAILED for orderId={}: {}", order.getOrderId(),
						ex.getMessage(), ex);
			}

			// Step 10: Generate AWB for return shipment
			if (returnShipmentId != null) {
				try {
					// Find best courier for the return route (customer → warehouse)
					Integer bestReturnCourierId = null;
					try {
						OrderAddressEO returnOrderAddress = orderAddressRepository.findByOrder(order).orElse(null);
						String customerPostcode = returnOrderAddress != null ? returnOrderAddress.getPostalCode()
								: null;
						if (customerPostcode != null && !customerPostcode.trim().isEmpty()) {
							ServiceabilityRequestDTO returnServiceabilityReq = ServiceabilityRequestDTO.builder()
								.orderId(returnShipOrderId)
								.pickupPostcode(customerPostcode.trim().length() == 6
										? Integer.parseInt(customerPostcode.trim()) : null)
								.deliveryPostcode(Integer.parseInt(getWarehousePostalCode(
										warehouse != null ? warehouse.getWarehouseName() : null)))
								.cod(0) // returns are always prepaid
								.weight(originalShipment.getWeight() != null
										? String.valueOf(originalShipment.getWeight()) : "1.0")
								.length(originalShipment.getLength() != null ? originalShipment.getLength().intValue()
										: null)
								.breadth(originalShipment.getBreadth() != null
										? originalShipment.getBreadth().intValue() : null)
								.height(originalShipment.getHeight() != null ? originalShipment.getHeight().intValue()
										: null)
								.isReturn(1) // flag as return shipment
								.build();
							bestReturnCourierId = shiprocketService.getBestCourierService(returnServiceabilityReq);
							logger.info("Step FIND_BEST_RETURN_COURIER: selected courierCompanyId={} "
									+ "for return orderId={}", bestReturnCourierId, order.getOrderId());
						}
						else {
							logger.warn("Step FIND_BEST_RETURN_COURIER skipped: customer postcode unavailable");
						}
					}
					catch (Exception ex) {
						logger.warn("Step FIND_BEST_RETURN_COURIER FAILED, using auto-assign: {}", ex.getMessage());
					}

					Map awbResponse = shiprocketService.generateAWB(returnShipmentId, bestReturnCourierId);
					String returnAwb = null;
					Integer returnCourierId = null;
					String returnCourierName = null;
					if (awbResponse != null) {
						// Primary: response → data → awb_code
						Object respObj = awbResponse.get("response");
						if (respObj instanceof Map) {
							Object dataObj = ((Map) respObj).get("data");
							if (dataObj instanceof Map) {
								Object awb = ((Map) dataObj).get("awb_code");
								if (awb instanceof String)
									returnAwb = (String) awb;
								Object ccId = ((Map) dataObj).get("courier_company_id");
								if (ccId instanceof Number)
									returnCourierId = ((Number) ccId).intValue();
								Object cn = ((Map) dataObj).get("courier_name");
								if (cn instanceof String)
									returnCourierName = (String) cn;
							}
						}
						// Fallback: top-level awb_code
						if (returnAwb == null) {
							Object topAwb = awbResponse.get("awb_code");
							if (topAwb instanceof String)
								returnAwb = (String) topAwb;
						}
						// Check for awb_generate_error
						if (returnAwb == null) {
							Object err = awbResponse.get("awb_generate_error");
							if (err instanceof String)
								logger.warn("Step SHIPROCKET_RETURN_AWB error: {}", err);
						}
					}
					if (returnAwb != null) {
						savedReturnShipment.setAwb(returnAwb);
						if (returnCourierId != null)
							savedReturnShipment.setCourierCompanyId(returnCourierId);
						if (returnCourierName != null)
							savedReturnShipment.setCourierName(returnCourierName);
						shippingRepository.save(savedReturnShipment);
						logger.info("Step SHIPROCKET_RETURN_AWB SUCCESS: awb={}, courier={} for orderId={}", returnAwb,
								returnCourierName, order.getOrderId());
					}
					else {
						logger.warn("Step SHIPROCKET_RETURN_AWB: AWB not received for returnShipmentId={}",
								returnShipmentId);
					}
				}
				catch (Exception ex) {
					logger.error("Step SHIPROCKET_RETURN_AWB FAILED for returnShipmentId={}: {}", returnShipmentId,
							ex.getMessage(), ex);
				}

				// Step 11: Request Pickup Schedule for return shipment
				try {
					Map pickupResponse = shiprocketService.requestPickup(returnShipmentId.toString());
					if (pickupResponse != null) {
						Map pickupData = null;
						Object respObj = pickupResponse.get("response");
						if (respObj instanceof Map) {
							pickupData = (Map) respObj;
						}
						else {
							pickupData = pickupResponse; // fallback: top-level
						}
						Object pidObj = pickupData.get("pickup_id");
						if (pidObj instanceof Number)
							savedReturnShipment.setPickupId(((Number) pidObj).longValue());
						Object schedObj = pickupData.get("pickup_scheduled_date");
						if (schedObj instanceof String && !((String) schedObj).isEmpty()) {
							try {
								savedReturnShipment.setPickupScheduledDate(LocalDateTime.parse((String) schedObj,
										DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
							}
							catch (Exception ignored) {
								logger.warn("Could not parse return pickup_scheduled_date '{}'", schedObj);
							}
						}
						Object tokenObj = pickupData.get("pickup_token_number");
						if (tokenObj instanceof String)
							savedReturnShipment.setPickupToken((String) tokenObj);

						savedReturnShipment.setShipmentStatus("RETURN_PICKUP_SCHEDULED");
						shippingRepository.save(savedReturnShipment);

						// Add tracking history entry
						ShipmentTrackingHistoryEO pickupHistory = new ShipmentTrackingHistoryEO();
						pickupHistory.setShipment(savedReturnShipment);
						pickupHistory.setStatus("RETURN_PICKUP_SCHEDULED");
						pickupHistory.setRemarks("Return pickup scheduled via Shiprocket");
						pickupHistory.setUpdatedAt(LocalDateTime.now());
						shipmentTrackingHistoryRepository.save(pickupHistory);

						logger.info("Step SHIPROCKET_RETURN_PICKUP_SCHEDULE SUCCESS for orderId={}",
								order.getOrderId());
					}
				}
				catch (Exception ex) {
					logger.error("Step SHIPROCKET_RETURN_PICKUP_SCHEDULE FAILED for returnShipmentId={}: {}",
							returnShipmentId, ex.getMessage(), ex);
				}
			}
			else {
				logger.warn(
						"Skipping AWB + pickup steps: Shiprocket return shipment ID not available " + "for orderId={}",
						order.getOrderId());
			}
			// ─────────────────────────────────────────────────────────────────────────

		}
		catch (Exception e) {
			logger.error("Error in processReturnOrderEvent for orderId={}: {}", event.getOrderId(), e.getMessage(), e);
		}
	}

	private static String normalizedUploadDir(String uploadDir) {
		if (uploadDir == null || uploadDir.isBlank()) {
			return "";
		}
		if (!uploadDir.endsWith("/") && !uploadDir.endsWith("\\")) {
			return uploadDir + "/";
		}
		return uploadDir;
	}

	/**
	 * Validates if the given string is a valid OrderID. Example valid format:
	 * ORD-260317111721-000031
	 * @param orderId the string to validate
	 * @return true if valid OrderID, false otherwise
	 */
	private boolean isValidOrderId(String orderId) {
		if (orderId == null)
			return false;
		// Example pattern: ORD-<digits>-<6 digits>
		return orderId.matches("ORD-\\d{12}-\\d{6}");
	}

	/**
	 * Returns a list of order items where ProductVarName contains the given productName
	 * (case-insensitive).
	 * @param orderItems List of OrderItemEO
	 * @param productName Product name to search for
	 * @return List of matching OrderItemEO
	 */
	public List<OrderItemEO> filterOrderItemsByProductName(List<OrderItemEO> orderItems, String productName) {
		if (orderItems == null || productName == null)
			return Collections.emptyList();
		String lowerProductName = productName.toLowerCase();
		return orderItems.stream()
			.filter(item -> item.getProductVarName() != null
					&& item.getProductVarName().toLowerCase().contains(lowerProductName))
			.collect(Collectors.toList());
	}

	/**
	 * Returns true if the search string matches any address field (recipient name,
	 * address lines, city, state, postal code, country).
	 */
	private boolean matchesAddress(OrderAddressEO address, String searchString) {
		if (address == null || searchString == null)
			return false;
		String q = searchString.toLowerCase();
		return containsIgnoreCase(address.getRecipientName(), q) || containsIgnoreCase(address.getAddressLine1(), q)
				|| containsIgnoreCase(address.getAddressLine2(), q) || containsIgnoreCase(address.getCity(), q)
				|| containsIgnoreCase(address.getState(), q) || containsIgnoreCase(address.getPostalCode(), q)
				|| containsIgnoreCase(address.getCountry(), q);
	}

	private boolean containsIgnoreCase(String field, String query) {
		return field != null && field.toLowerCase().contains(query);
	}

	@Override
	public ResponseDTO processRefundByReference(String refundReference) {
		ResponseDTO response = new ResponseDTO();
		try {
			if (refundReference == null || refundReference.trim().isEmpty()) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Refund reference is required");
				return response;
			}

			RefundTransactionEO refundTransaction = refundTransactionRepository.findByRefundReference(refundReference)
				.orElse(null);
			if (refundTransaction == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage(Constants.PAYMENT_REFUND_REFERENCE_MISSING);
				return response;
			}

			RefundInitiatedEvent event = RefundInitiatedEvent.builder()
				.orderId(refundTransaction.getOrderId())
				.cancelRequestId(refundTransaction.getCancelRequestId())
				.refundReference(refundTransaction.getRefundReference())
				.amount(refundTransaction.getRequestedAmount())
				.currency(refundTransaction.getCurrency())
				.build();

			this.processRefundInitiatedEvent(event);

			refundTransaction.setStatus(Constants.PAYMENT_REFUND_STATUS_INPROGRESS);
			refundTransaction.setUpdatedAt(LocalDateTime.now());
			refundTransactionRepository.save(refundTransaction);

			OrderEO order = orderRepository.findById(refundTransaction.getOrderId()).orElse(null);
			if (order != null && order.getCustomer() != null) {
				CustomerEO customer = order.getCustomer();
				String email = customer.getEmail();
				String mobile = customer.getMobileNumber();
				String customerName = customer.getFirstName();
				String refundStatus = refundTransaction.getStatus();
				String orderNumber = order.getOrderNumber();
				String refundRef = refundTransaction.getRefundReference();
				String amountStr = refundTransaction.getRequestedAmount() != null
						? refundTransaction.getRequestedAmount().toPlainString() : "0";
				String currency = refundTransaction.getCurrency() != null ? refundTransaction.getCurrency()
						: Constants.PAYMENT_CURRENCY;

				String emailSubject = "Refund " + refundStatus + " for Order #" + orderNumber;
				String emailMessage = String.format(
						"Hi %s,\n\nYour refund for order %s is %s.\nRefund Reference: %s\nAmount: %s %s\n\nThank you for shopping with us!",
						customerName, orderNumber, refundStatus, refundRef, amountStr, currency);
				String smsSubject = "Refund Status";
				String smsMessage = String.format("Refund %s for Order %s. Ref: %s. Amount: %s %s", refundStatus,
						orderNumber, refundRef, amountStr, currency);

				notificationService
					.processEvent(ObjectMapper.buildEventObject(email, mobile, Constants.COMMUNICATION_PURPOSE_REFUND,
							emailSubject, emailMessage, smsSubject, smsMessage, Constants.COMMUNICATION_CHANNEL_BOTH));
			}

			response.setResponseStatus(Constants.STATUS_SUCCESS);
			response.setResponseMessage("Refund initiated successfully");
		}
		catch (Exception e) {
			logger.error("Error in processRefundByReference for refundReference={}", refundReference, e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to initiate refund");
		}
		return response;
	}

	@Override
	public void processRefundInitiatedEvent(RefundInitiatedEvent event) {
		if (event == null || event.getRefundReference() == null || event.getRefundReference().trim().isEmpty()) {
			return;
		}
		try {
			RefundTransactionEO refundTransaction = refundTransactionRepository
				.findByRefundReference(event.getRefundReference())
				.orElse(null);
			if (refundTransaction == null) {
				return;
			}

			OrderEO order = orderRepository.findById(refundTransaction.getOrderId()).orElse(null);
			if (order == null) {
				return;
			}

			PaymentEO payment = paymentRepository.findByOrder(order).orElse(null);
			if (payment == null || payment.getTransactionId() == null) {
				refundTransaction.setStatus(Constants.PAYMENT_REFUND_STATUS_FAILED);
				refundTransaction.setFailureReason("Missing payment transaction id");
				refundTransaction.setUpdatedAt(LocalDateTime.now());
				refundTransactionRepository.save(refundTransaction);
				return;
			}

			// Use approvedAmount; fall back to requestedAmount if not yet approved
			BigDecimal amount = refundTransaction.getApprovedAmount() != null ? refundTransaction.getApprovedAmount()
					: refundTransaction.getRequestedAmount();

			// Validate refund amount against the paid amount before calling Razorpay
			BigDecimal paidAmount = payment.getAmount();
			if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
				logger.error("Refund amount is null or zero for refundReference={}", event.getRefundReference());
				refundTransaction.setStatus(Constants.PAYMENT_REFUND_STATUS_FAILED);
				refundTransaction.setFailureReason("Refund amount is null or zero");
				refundTransaction.setUpdatedAt(LocalDateTime.now());
				refundTransactionRepository.save(refundTransaction);
				return;
			}
			if (paidAmount != null && amount.compareTo(paidAmount) > 0) {
				logger.error("Refund amount {} exceeds paid amount {} for refundReference={}", amount, paidAmount,
						event.getRefundReference());
				refundTransaction.setStatus(Constants.PAYMENT_REFUND_STATUS_FAILED);
				refundTransaction.setFailureReason("Refund amount (" + amount.toPlainString()
						+ ") exceeds paid amount (" + paidAmount.toPlainString() + ")");
				refundTransaction.setUpdatedAt(LocalDateTime.now());
				refundTransactionRepository.save(refundTransaction);
				return;
			}

			// ── STEP 1: RESOLVE THE EFFECTIVE (CAPTURED) PAYMENT ID ─────────────────
			// Our DB may store an `authorized` payment ID if the customer retried
			// payment.
			// We must find the actually-captured payment before calling the refund API.
			String effectivePaymentId = payment.getTransactionId();
			try {
				com.razorpay.Payment rzpPayment = razorpayClient.payments.fetch(effectivePaymentId);
				String rzpStatus = rzpPayment.get("status");
				logger.info("Razorpay payment status check: paymentId={}, status={}, refundReference={}",
						effectivePaymentId, rzpStatus, event.getRefundReference());

				if ("authorized".equals(rzpStatus)) {
					// Try to capture the full authorized amount (Razorpay requires full
					// capture amount)
					try {
						JSONObject captureRequest = new JSONObject();
						captureRequest.put("amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue());
						captureRequest.put("currency", "INR");
						razorpayClient.payments.capture(effectivePaymentId, captureRequest);
						logger.info("Pre-refund capture SUCCESS for paymentId={}", effectivePaymentId);
					}
					catch (Exception captureEx) {
						String captureErrMsg = captureEx.getMessage() != null ? captureEx.getMessage() : "";
						logger.warn("Pre-refund capture failed for paymentId={}: {}", effectivePaymentId,
								captureErrMsg);

						// "order is already paid" → the Razorpay order was paid by a
						// DIFFERENT payment.
						// Fetch all payments for the Razorpay order and find the captured
						// one.
						if (captureErrMsg.contains("already paid") || captureErrMsg.contains("order is already paid")) {
							String razorpayOrderId = payment.getPaymentProviderOrderId();
							if (razorpayOrderId != null) {
								try {
									String originalPaymentId = effectivePaymentId;
									java.util.List<com.razorpay.Payment> orderPayments = razorpayClient.orders
										.fetchPayments(razorpayOrderId);
									for (com.razorpay.Payment rzpPay : orderPayments) {
										if ("captured".equals((String) rzpPay.get("status"))) {
											String capturedPayId = rzpPay.get("id");
											logger.info(
													"Found captured payment for Razorpay orderId={}: capturedPaymentId={}, "
															+ "replacing storedPaymentId={} in DB for refundReference={}",
													razorpayOrderId, capturedPayId, originalPaymentId,
													event.getRefundReference());
											effectivePaymentId = capturedPayId;
											// Persist the correct captured payment ID so
											// future refunds/lookups work
											payment.setTransactionId(capturedPayId);
											paymentRepository.save(payment);
											break;
										}
									}
									if (effectivePaymentId.equals(originalPaymentId)) {
										logger.error(
												"No captured payment found for Razorpay orderId={}, "
														+ "refundReference={} — cannot process refund",
												razorpayOrderId, event.getRefundReference());
									}
								}
								catch (Exception fetchOrderEx) {
									logger.error("Failed to fetchPayments for Razorpay orderId={}: {}", razorpayOrderId,
											fetchOrderEx.getMessage());
								}
							}
						}
					}
				}
				else if ("captured".equals(rzpStatus)) {
					logger.info("Payment paymentId={} is already captured, proceeding directly to refund",
							effectivePaymentId);
				}
				else {
					logger.warn(
							"Payment paymentId={} has unexpected Razorpay status='{}', proceeding with refund attempt",
							effectivePaymentId, rzpStatus);
				}
			}
			catch (Exception fetchEx) {
				logger.warn(
						"Could not fetch Razorpay payment status for paymentId={}: {} — proceeding with refund anyway",
						effectivePaymentId, fetchEx.getMessage());
			}
			// ────────────────────────────────────────────────────────────────────────

			JSONObject refundRequest = new JSONObject();
			// Razorpay expects amount in paise (multiply by 100) — the smallest currency
			// unit for INR.
			long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValue();
			logger.info("Refund amount in INR={}, converted to paise={} for refundReference={}", amount.toPlainString(),
					amountInPaise, event.getRefundReference());
			refundRequest.put("amount", amountInPaise);
			// Build notes explicitly as JSONObject with all string values (Razorpay
			// requirement)
			JSONObject notesJson = new JSONObject();
			notesJson.put("order_id", String.valueOf(refundTransaction.getOrderId()));
			notesJson.put("refund_transaction_id", String.valueOf(refundTransaction.getId()));
			notesJson.put("refund_reason",
					refundTransaction.getRefundReason() != null ? refundTransaction.getRefundReason() : "");
			notesJson.put("requested_amount", refundTransaction.getRequestedAmount() != null
					? refundTransaction.getRequestedAmount().toPlainString() : "0");
			refundRequest.put("notes", notesJson);

			logger.info("Calling Razorpay refund for paymentId={} (original storedId={}), refundRequest={}",
					effectivePaymentId, payment.getTransactionId(), refundRequest.toString());

			try {
				Refund refund = razorpayClient.payments.refund(effectivePaymentId, refundRequest);
				if (refund != null) {
					refundTransaction.setGatewayRefundId(refund.get("id"));
					refundTransaction.setStatus(Constants.PAYMENT_REFUND_STATUS_SUCCESS);
					refundTransaction.setRefundedAmount(amount);
					List<ShippingEO> shipmentList = shippingRepository.findByOrder(order);
					if (shipmentList != null && !shipmentList.isEmpty()) {
						ShippingEO savedShippingEO = shipmentList.get(0);
						ShipmentTrackingHistoryEO shipmentTrackingHistoryEO = new ShipmentTrackingHistoryEO();
						shipmentTrackingHistoryEO.setShipment(savedShippingEO);
						shipmentTrackingHistoryEO.setStatus(Constants.SHIPMENT_ORDER_REFUND_PROCESSED);
						shipmentTrackingHistoryEO.setRemarks(Constants.SHIPMENT_ORDER_REFUND_PROCESSED_REMARK);
						shipmentTrackingHistoryEO.setUpdatedAt(LocalDateTime.now());
						shipmentTrackingHistoryRepository.save(shipmentTrackingHistoryEO);
					}
					logger.info("Razorpay refund SUCCESS: refundId={}, refundReference={}", refund.get("id"),
							event.getRefundReference());
				}
				else {
					refundTransaction.setStatus(Constants.PAYMENT_REFUND_STATUS_FAILED);
					refundTransaction.setFailureReason("Refund failed via Razorpay API: null response");
				}
			}
			catch (com.razorpay.RazorpayException re) {
				logger.error("Razorpay API error for paymentId={}, refundReference={}: code={}, message={}",
						payment.getTransactionId(), event.getRefundReference(), re.getMessage(), re);
				refundTransaction.setStatus(Constants.PAYMENT_REFUND_STATUS_FAILED);
				refundTransaction.setFailureReason("Razorpay error: " + re.getMessage());
			}
			catch (Exception e) {
				logger.error("Unexpected error during Razorpay refund for refundReference={}: {}",
						event.getRefundReference(), e.getMessage(), e);
				refundTransaction.setStatus(Constants.PAYMENT_REFUND_STATUS_FAILED);
				refundTransaction.setFailureReason("Exception during refund: " + e.getMessage());
			}

			refundTransaction.setUpdatedAt(LocalDateTime.now());
			refundTransactionRepository.save(refundTransaction);

			CustomerEO customer = order.getCustomer();
			if (customer != null) {
				String email = customer.getEmail();
				String mobile = customer.getMobileNumber();
				String customerName = customer.getFirstName();
				String refundStatus = refundTransaction.getStatus();
				String orderNumber = order.getOrderNumber();
				String refundRef = refundTransaction.getRefundReference();
				String amountStr = refundTransaction.getRequestedAmount() != null
						? refundTransaction.getRequestedAmount().toPlainString() : "0";
				String currency = refundTransaction.getCurrency() != null ? refundTransaction.getCurrency()
						: Constants.PAYMENT_CURRENCY;

				String emailSubject = "Refund " + refundStatus + " for Order #" + orderNumber;
				String emailMessage = String.format(
						"Hi %s,\n\nYour refund for order %s is %s.\nRefund Reference: %s\nAmount: %s %s\n\nThank you for shopping with us!",
						customerName, orderNumber, refundStatus, refundRef, amountStr, currency);
				String smsSubject = "Refund Status";
				String smsMessage = String.format("Refund %s for Order %s. Ref: %s. Amount: %s %s", refundStatus,
						orderNumber, refundRef, amountStr, currency);

				notificationService
					.processEvent(ObjectMapper.buildEventObject(email, mobile, Constants.COMMUNICATION_PURPOSE_REFUND,
							emailSubject, emailMessage, smsSubject, smsMessage, Constants.COMMUNICATION_CHANNEL_BOTH));
			}
		}
		catch (Exception e) {
			logger.error("Error processing refund initiated event for refundReference={}", event.getRefundReference(),
					e);
		}
	}

	@Override
	public List<RefundRecordDTO> getRefunds(RefundSearchRequestDTO request) {
		List<String> statuses = request != null ? trimToNullList(request.getStatus()) : null;
		String searchKey = request != null ? trimToNull(request.getOrderNumber()) : null;
		LocalDateTime createdFrom = request != null ? request.getCreatedFrom() : null;
		LocalDateTime createdTo = request != null ? request.getCreatedTo() : null;

		Long orderId = null;
		if (searchKey != null) {
			String upperKey = searchKey.toUpperCase();
			if (upperKey.startsWith("REFUND")) {
				RefundTransactionEO refund = refundTransactionRepository.findByRefundReference(searchKey).orElse(null);
				if (refund == null) {
					return Collections.emptyList();
				}
				return Collections.singletonList(mapRefundRecord(refund));
			}
			if (upperKey.startsWith("ORD")) {
				OrderEO order = orderRepository.findByOrderNumber(searchKey).orElse(null);
				orderId = order != null && order.getOrderId() != null ? order.getOrderId().longValue() : null;
				if (orderId == null) {
					return Collections.emptyList();
				}
			}
		}

		List<RefundTransactionEO> refunds;
		if (createdFrom == null && createdTo == null) {
			refunds = refundTransactionRepository.findRefundsWithoutDates(statuses, orderId);
		}
		else if (createdFrom != null && createdTo != null) {
			refunds = refundTransactionRepository.findRefundsBetweenDates(statuses, createdFrom, createdTo, orderId);
		}
		else if (createdFrom != null) {
			refunds = refundTransactionRepository.findRefundsFromDate(statuses, createdFrom, orderId);
		}
		else {
			refunds = refundTransactionRepository.findRefundsToDate(statuses, createdTo, orderId);
		}

		return refunds.stream().map(this::mapRefundRecord).collect(Collectors.toList());
	}

	private static List<String> trimToNullList(List<String> values) {
		if (values == null || values.isEmpty()) {
			return null;
		}
		List<String> trimmed = values.stream()
			.map(String::trim)
			.filter(value -> !value.isEmpty())
			.distinct()
			.collect(Collectors.toList());
		return trimmed.isEmpty() ? null : trimmed;
	}

	private RefundRecordDTO mapRefundRecord(RefundTransactionEO refund) {
		OrderEO order = refund != null && refund.getOrderId() != null
				? orderRepository.findById(refund.getOrderId()).orElse(null) : null;
		CustomerEO customer = order != null ? order.getCustomer() : null;
		List<OrderItemEO> items = order != null ? orderProductRepository.findByOrder(order) : Collections.emptyList();

		List<RefundOrderItemDTO> itemDTOs = items.stream()
			.map(item -> RefundOrderItemDTO.builder()
				.productName(item.getProductVarName())
				.skuCode(item.getSkuCode())
				.quantity(item.getQuantity())
				.unitPrice(item.getUnitPrice())
				.totalPrice(item.getTotalPrice())
				.build())
			.collect(Collectors.toList());

		RefundOrderDetailsDTO orderDTO = order == null ? null
				: RefundOrderDetailsDTO.builder()
					.orderNumber(order.getOrderNumber())
					.orderStatus(order.getOrderStatus())
					.totalAmount(order.getTotalAmount())
					.currency(order.getCurrency())
					.orderDate(order.getCreatedAt())
					.items(itemDTOs)
					.build();

		String customerName = customer != null
				? String.format("%s %s", nullToEmpty(customer.getFirstName()), nullToEmpty(customer.getLastName()))
					.trim()
				: null;

		return RefundRecordDTO.builder()
			.refundTransactionId(refund.getId())
			.refundReference(refund.getRefundReference())
			.gatewayRefundId(refund.getGatewayRefundId())
			.status(refund.getStatus())
			.refundType(refund.getRefundType())
			.refundReason(refund.getRefundReason())
			.failureReason(refund.getFailureReason())
			.requestedAmount(refund.getRequestedAmount())
			.approvedAmount(refund.getApprovedAmount())
			.refundedAmount(refund.getRefundedAmount())
			.currency(refund.getCurrency())
			.createdAt(refund.getCreatedAt())
			.customerName(customerName)
			.customerMobile(customer != null ? customer.getMobileNumber() : null)
			.order(orderDTO)
			.build();
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	@Override
	public ResponseDTO updateRefundApprovedAmount(RefundApproveRequestDTO request) {
		ResponseDTO response = new ResponseDTO();
		try {
			if (request == null || request.getRefundReference() == null
					|| request.getRefundReference().trim().isEmpty()) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage(Constants.PAYMENT_REFUND_REFERENCE_MISSING);
				return response;
			}
			if (request.getApprovedAmount() == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Approved amount is required");
				return response;
			}

			RefundTransactionEO refundTransaction = refundTransactionRepository
				.findByRefundReference(request.getRefundReference())
				.orElse(null);
			if (refundTransaction == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage(Constants.PAYMENT_REFUND_REFERENCE_MISSING);
				return response;
			}

			// Fetch the payment to get the actual amount paid by the customer
			OrderEO refundOrder = orderRepository.findById(refundTransaction.getOrderId()).orElse(null);
			PaymentEO refundPayment = refundOrder != null ? paymentRepository.findByOrder(refundOrder).orElse(null)
					: null;
			BigDecimal paidAmount = refundPayment != null ? refundPayment.getAmount() : null;

			// Validate: requested_amount must not exceed the paid amount
			if (paidAmount != null && refundTransaction.getRequestedAmount() != null
					&& refundTransaction.getRequestedAmount().compareTo(paidAmount) > 0) {
				logger.warn("Requested refund amount {} exceeds paid amount {} for refundReference={}",
						refundTransaction.getRequestedAmount(), paidAmount, request.getRefundReference());
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage(
						"Requested refund amount (" + refundTransaction.getRequestedAmount().toPlainString()
								+ ") cannot exceed the paid amount (" + paidAmount.toPlainString() + ")");
				return response;
			}

			// Validate: approved_amount must not exceed the paid amount
			if (paidAmount != null && request.getApprovedAmount().compareTo(paidAmount) > 0) {
				logger.warn("Approved refund amount {} exceeds paid amount {} for refundReference={}",
						request.getApprovedAmount(), paidAmount, request.getRefundReference());
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Approved refund amount (" + request.getApprovedAmount().toPlainString()
						+ ") cannot exceed the paid amount (" + paidAmount.toPlainString() + ")");
				return response;
			}

			// Note: when the refund is sent to Razorpay, the amount is converted to paise
			// (multiplied by 100)
			// as Razorpay processes amounts in the smallest currency unit (paise for
			// INR).
			refundTransaction.setApprovedAmount(request.getApprovedAmount());
			refundTransaction.setStatus(Constants.PAYMENT_REFUND_STATUS_APPROVED);
			refundTransaction.setUpdatedAt(LocalDateTime.now());
			refundTransactionRepository.save(refundTransaction);
			// invoke refund process
			processRefundByReference(request.getRefundReference());

			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Approved amount updated successfully");
		}
		catch (Exception e) {
			logger.error("Error updating approved amount for refundReference={}",
					request != null ? request.getRefundReference() : null, e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to update approved amount");
		}
		return response;
	}

	@Override
	public ResponseDTO createReturnPolicy(ReturnPolicyCreateDTO request) {
		logger.info("Starting createReturnPolicy: {}", request);
		ResponseDTO response = new ResponseDTO();
		try {
			ReturnPolicyEO policy = ReturnPolicyEO.builder()
				.name(request.getName())
				.description(request.getDescription())
				.returnWindowDays(request.getReturnWindowDays())
				.isReturnable(request.getIsReturnable())
				.build();
			ReturnPolicyEO saved = returnPolicyRepository.save(policy);
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Return policy created successfully with id: " + saved.getId());
			logger.info("Created ReturnPolicyEO id={}", saved.getId());
		}
		catch (Exception e) {
			logger.error("Error creating return policy", e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to create return policy");
		}
		return response;
	}

	@Override
	public ResponseDTO updateReturnPolicy(Long policyId, ReturnPolicyCreateDTO request) {
		ResponseDTO responseDTO = new ResponseDTO();
		try {
			ReturnPolicyEO policy = returnPolicyRepository.findById(policyId)
				.orElseThrow(() -> new RuntimeException("Return policy not found: " + policyId));

			// Update policy fields
			policy.setName(request.getName());
			policy.setDescription(request.getDescription());
			policy.setReturnWindowDays(request.getReturnWindowDays());
			policy.setIsReturnable(request.getIsReturnable());

			returnPolicyRepository.save(policy);

			responseDTO.setResponseStatus(Constants.SUCCESS_STATUS);
			responseDTO.setResponseMessage("Return policy updated successfully");
		}
		catch (Exception e) {
			responseDTO.setResponseStatus(Constants.FAILURE_STATUS);
			responseDTO.setResponseMessage("Failed to update return policy: " + e.getMessage());
		}
		return responseDTO;
	}

	@Override
	public List<ReturnPolicyResponseDTO> getReturnPolicies() {
		List<ReturnPolicyEO> policies = returnPolicyRepository.findAll();
		return policies.stream()
			.map(policy -> ReturnPolicyResponseDTO.builder()
				.id(policy.getId())
				.name(policy.getName())
				.description(policy.getDescription())
				.returnWindowDays(policy.getReturnWindowDays())
				.isReturnable(policy.getIsReturnable())
				.refundType(policy.getRefundType())
				.returnMethod(policy.getReturnMethod())
				.build())
			.collect(Collectors.toList());
	}

	@Override
	public ResponseDTO createReturnPolicyCondition(ReturnPolicyConditionCreateDTO request) {
		ResponseDTO response = new ResponseDTO();
		try {
			if (request == null || request.getPolicyId() == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Policy id is required");
				return response;
			}
			ReturnPolicyEO policy = returnPolicyRepository.findById(request.getPolicyId()).orElse(null);
			if (policy == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Return policy not found");
				return response;
			}
			ReturnPolicyConditionEO condition = ReturnPolicyConditionEO.builder()
				.policy(policy)
				.conditionType(request.getConditionType())
				.conditionValue(request.getConditionValue())
				.build();
			ReturnPolicyConditionEO saved = returnPolicyConditionRepository.save(condition);
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Return policy condition created successfully with id: " + saved.getId());
		}
		catch (Exception e) {
			logger.error("Error creating return policy condition", e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to create return policy condition");
		}
		return response;
	}

	@Override
	public ResponseDTO updateReturnPolicyCondition(Long conditionId, ReturnPolicyConditionCreateDTO request) {
		ResponseDTO response = new ResponseDTO();
		try {
			if (conditionId == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Condition id is required");
				return response;
			}
			ReturnPolicyConditionEO condition = returnPolicyConditionRepository.findById(conditionId).orElse(null);
			if (condition == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Return policy condition not found");
				return response;
			}
			if (request != null) {
				if (request.getPolicyId() != null) {
					ReturnPolicyEO policy = returnPolicyRepository.findById(request.getPolicyId()).orElse(null);
					if (policy == null) {
						response.setResponseStatus(Constants.FAILURE_STATUS);
						response.setResponseMessage("Return policy not found for id: " + request.getPolicyId());
						return response;
					}
					condition.setPolicy(policy);
				}
				if (request.getConditionType() != null) {
					condition.setConditionType(request.getConditionType());
				}
				if (request.getConditionValue() != null) {
					condition.setConditionValue(request.getConditionValue());
				}
			}
			returnPolicyConditionRepository.save(condition);
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Return policy condition updated successfully");
		}
		catch (Exception e) {
			logger.error("Error updating return policy condition id={}", conditionId, e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to update return policy condition");
		}
		return response;
	}

	@Override
	public List<ReturnPolicyConditionResponseDTO> getReturnPolicyConditions(Long policyId) {
		List<ReturnPolicyConditionEO> conditions;
		if (policyId != null) {
			conditions = returnPolicyConditionRepository.findByPolicyId(policyId);
		}
		else {
			conditions = returnPolicyConditionRepository.findAll();
		}
		return conditions.stream()
			.map(condition -> ReturnPolicyConditionResponseDTO.builder()
				.id(condition.getId())
				.policyId(policyId)
				.conditionType(condition.getConditionType())
				.conditionValue(condition.getConditionValue())
				.build())
			.collect(Collectors.toList());
	}

	@Override
	public ResponseDTO createReturnPolicyMapping(ReturnPolicyMappingCreateDTO request) {
		ResponseDTO response = new ResponseDTO();
		try {
			if (request == null || request.getPolicyId() == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Policy id is required");
				return response;
			}
			ReturnPolicyEO policy = returnPolicyRepository.findById(request.getPolicyId()).orElse(null);
			if (policy == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Return policy not found");
				return response;
			}
			ReturnPolicyMappingEO mapping = ReturnPolicyMappingEO.builder()
				.policy(policy)
				.entityType(request.getEntityType())
				.entityId(request.getEntityId())
				.priority(request.getPriority() != null ? request.getPriority() : 0)
				.build();
			ReturnPolicyMappingEO saved = returnPolicyMappingRepository.save(mapping);
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Return policy mapping created successfully with id: " + saved.getId());
		}
		catch (Exception e) {
			logger.error("Error creating return policy mapping", e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to create return policy mapping");
		}
		return response;
	}

	@Override
	public ResponseDTO updateReturnPolicyMapping(Long mappingId, ReturnPolicyMappingCreateDTO request) {
		ResponseDTO response = new ResponseDTO();
		try {
			if (mappingId == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Mapping id is required");
				return response;
			}
			ReturnPolicyMappingEO mapping = returnPolicyMappingRepository.findById(mappingId).orElse(null);
			if (mapping == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Return policy mapping not found for id: " + mappingId);
				return response;
			}
			// Update policyId if provided
			if (request.getPolicyId() != null) {
				ReturnPolicyEO policy = returnPolicyRepository.findById(request.getPolicyId()).orElse(null);
				if (policy == null) {
					response.setResponseStatus(Constants.FAILURE_STATUS);
					response.setResponseMessage("Return policy not found for id: " + request.getPolicyId());
					return response;
				}
				mapping.setPolicy(policy);
			}
			// Update priority if provided
			if (request.getPriority() != null) {
				mapping.setPriority(request.getPriority());
			}
			// entityType and entityId are immutable — ignored even if supplied
			returnPolicyMappingRepository.save(mapping);
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Return policy mapping updated successfully");
			logger.info("Updated ReturnPolicyMappingEO id={}", mappingId);
		}
		catch (Exception e) {
			logger.error("Error updating return policy mapping id={}", mappingId, e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to update return policy mapping");
		}
		return response;
	}

	@Override
	public ResponseDTO deleteReturnPolicyMapping(Long mappingId) {
		ResponseDTO response = new ResponseDTO();
		try {
			if (mappingId == null) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Mapping id is required");
				return response;
			}
			if (!returnPolicyMappingRepository.existsById(mappingId)) {
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Return policy mapping not found for id: " + mappingId);
				return response;
			}
			returnPolicyMappingRepository.deleteById(mappingId);
			response.setResponseStatus(Constants.SUCCESS_STATUS);
			response.setResponseMessage("Return policy mapping deleted successfully");
			logger.info("Deleted ReturnPolicyMappingEO id={}", mappingId);
		}
		catch (Exception e) {
			logger.error("Error deleting return policy mapping id={}", mappingId, e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to delete return policy mapping");
		}
		return response;
	}

	@Override
	public List<ReturnPolicyMappingResponseDTO> getReturnPolicyMappings(String entityType, Long entityId) {
		try {
			List<ReturnPolicyMappingEO> mappings;
			if (entityType != null && entityId != null) {
				mappings = returnPolicyMappingRepository.findByEntityIdAndEntityType(entityId, entityType);
			}
			else if (entityType != null) {
				mappings = returnPolicyMappingRepository.findByEntityType(entityType);
			}
			else if (entityId != null) {
				mappings = returnPolicyMappingRepository.findByEntityId(entityId);
			}
			else {
				mappings = returnPolicyMappingRepository.findAll();
			}
			return mappings.stream()
				.map(m -> ReturnPolicyMappingResponseDTO.builder()
					.id(m.getId())
					.policyId(m.getPolicy() != null ? m.getPolicy().getId() : null)
					.policyName(m.getPolicy() != null ? m.getPolicy().getName() : null)
					.entityType(m.getEntityType())
					.entityId(m.getEntityId())
					.priority(m.getPriority())
					.build())
				.collect(Collectors.toList());
		}
		catch (Exception e) {
			logger.error("Error fetching return policy mappings: entityType={}, entityId={}", entityType, entityId, e);
			return new ArrayList<>();
		}
	}

	@Override
	public ReturnPolicyDetailDTO getReturnPolicyByProductVariantId(Long productVariantId) {
		logger.info("Fetching return policy for productVariantId={}", productVariantId);
		try {
			if (productVariantId == null) {
				logger.warn("productVariantId is null");
				return null;
			}
			// Step 1: fetch ProductVariantEO
			ProductVariantEO variant = productVariantRepository.findById(productVariantId).orElse(null);
			if (variant == null) {
				logger.warn("ProductVariant not found for id={}", productVariantId);
				return null;
			}
			// Step 2: fetch ProductEO from variant
			ProductEO product = variant.getProduct();
			if (product == null) {
				logger.warn("Product not found for variantId={}", productVariantId);
				return null;
			}
			Long productId = product.getId().longValue();
			// Step 3: fetch return_policy_mapping by product id as entity_id
			List<ReturnPolicyMappingEO> mappings = returnPolicyMappingRepository.findByEntityIdAndEntityType(productId,
					"PRODUCTS");
			if (mappings == null || mappings.isEmpty()) {
				logger.info("No return policy mapping found for productId={}", productId);
				return null;
			}
			// Use highest priority mapping (lowest priority int value first, or first
			// available)
			ReturnPolicyMappingEO mapping = mappings.stream().min((a, b) -> {
				int pa = a.getPriority() != null ? a.getPriority() : 0;
				int pb = b.getPriority() != null ? b.getPriority() : 0;
				return Integer.compare(pb, pa); // higher priority number = more specific
			}).orElse(mappings.get(0));
			// Step 4: fetch ReturnPolicyEO
			ReturnPolicyEO policy = mapping.getPolicy();
			if (policy == null) {
				logger.warn("Return policy not found in mapping id={}", mapping.getId());
				return null;
			}
			// Step 5: fetch ReturnPolicyConditions
			List<ReturnPolicyConditionEO> conditionEOs = returnPolicyConditionRepository.findByPolicyId(policy.getId());
			List<ReturnPolicyConditionResponseDTO> conditions = conditionEOs.stream()
				.map(c -> ReturnPolicyConditionResponseDTO.builder()
					.id(c.getId())
					.policyId(policy.getId())
					.conditionType(c.getConditionType())
					.conditionValue(c.getConditionValue())
					.build())
				.collect(Collectors.toList());
			ReturnPolicyDetailDTO result = ReturnPolicyDetailDTO.builder()
				.policyId(policy.getId())
				.name(policy.getName())
				.description(policy.getDescription())
				.returnWindowDays(policy.getReturnWindowDays())
				.isReturnable(policy.getIsReturnable())
				.refundType(policy.getRefundType())
				.returnMethod(policy.getReturnMethod())
				.conditions(conditions)
				.build();
			logger.info("Return policy fetched successfully for productVariantId={}, policyId={}", productVariantId,
					policy.getId());
			return result;
		}
		catch (Exception e) {
			logger.error("Error fetching return policy for productVariantId={}", productVariantId, e);
			return null;
		}
	}

	// ─────────────────────────────────────────────────────────────────────────
	// Get return policies by product category IDs
	// Lookup order per category:
	// 1. CATEGORY mapping (entityType = "CATEGORY", entityId = categoryId)
	// 2. PRODUCTS mapping (entityType = "PRODUCTS", entityId = any productId in that
	// category)
	// 3. GLOBAL mapping (entityType = "GLOBAL", entityId = 0)
	// 4. null (no mapping found at any level)
	// ─────────────────────────────────────────────────────────────────────────
	@Override
	public List<CategoryReturnPolicyResponseDTO> getReturnPoliciesByCategories(List<Long> categoryIds) {

		List<CategoryReturnPolicyResponseDTO> result = new ArrayList<>();

		if (categoryIds == null || categoryIds.isEmpty()) {
			return result;
		}

		try {
			// ── 1. Batch-fetch all CATEGORY-level mappings for the supplied IDs ──
			List<ReturnPolicyMappingEO> categoryMappings = returnPolicyMappingRepository
				.findByEntityIdInAndEntityType(categoryIds, "CATEGORY");

			// Build a map: categoryId → best CATEGORY mapping (highest priority int wins)
			Map<Long, ReturnPolicyMappingEO> bestByCategory = new HashMap<>();
			for (ReturnPolicyMappingEO m : categoryMappings) {
				Long catId = m.getEntityId();
				ReturnPolicyMappingEO existing = bestByCategory.get(catId);
				if (existing == null) {
					bestByCategory.put(catId, m);
				}
				else {
					int pNew = m.getPriority() != null ? m.getPriority() : 0;
					int pOld = existing.getPriority() != null ? existing.getPriority() : 0;
					if (pNew > pOld) {
						bestByCategory.put(catId, m);
					}
				}
			}

			// ── 2. Pre-collect category IDs that need PRODUCTS-level lookup ──────
			// (those without a direct CATEGORY mapping)
			List<Long> categoriesNeedingProductLookup = new ArrayList<>();
			for (Long catId : categoryIds) {
				if (!bestByCategory.containsKey(catId)) {
					categoriesNeedingProductLookup.add(catId);
				}
			}

			// Build map: categoryId → best PRODUCTS mapping
			// AND categoryId → ALL product mappings (for the detail list)
			Map<Long, ReturnPolicyMappingEO> bestByProductsForCategory = new HashMap<>();
			// categoryId → list of (ProductEO, ReturnPolicyMappingEO) pairs for detail
			Map<Long, List<Object[]>> allProductMappingsForCategory = new HashMap<>();

			if (!categoriesNeedingProductLookup.isEmpty()) {
				for (Long catId : categoriesNeedingProductLookup) {
					try {
						// Fetch all products in this category
						ProductCategoriesEO catEO = productCatRepository.findById(catId).orElse(null);
						if (catEO == null)
							continue;

						List<ProductEO> products = productRepository.findByCategoryAndStatus(catEO, "A");
						if (products == null || products.isEmpty())
							continue;

						// Build productId → ProductEO lookup
						Map<Long, ProductEO> productById = new HashMap<>();
						for (ProductEO p : products) {
							productById.put(p.getId().longValue(), p);
						}

						List<Long> productIds = new ArrayList<>(productById.keySet());

						// Batch-fetch PRODUCTS mappings for these product IDs
						List<ReturnPolicyMappingEO> productMappings = returnPolicyMappingRepository
							.findByEntityIdInAndEntityType(productIds, "PRODUCTS");

						if (productMappings == null || productMappings.isEmpty())
							continue;

						// Pick highest-priority mapping across all products in this
						// category
						ReturnPolicyMappingEO best = productMappings.stream()
							.max(java.util.Comparator.comparingInt(m -> m.getPriority() != null ? m.getPriority() : 0))
							.orElse(null);
						if (best != null) {
							bestByProductsForCategory.put(catId, best);
						}

						// Store all (ProductEO, mapping) pairs for the detail list
						List<Object[]> pairs = new ArrayList<>();
						for (ReturnPolicyMappingEO pm : productMappings) {
							ProductEO prod = productById.get(pm.getEntityId());
							if (prod != null) {
								pairs.add(new Object[] { prod, pm });
							}
						}
						if (!pairs.isEmpty()) {
							allProductMappingsForCategory.put(catId, pairs);
						}

					}
					catch (Exception ex) {
						logger.warn("Error fetching PRODUCTS mapping for categoryId={}: {}", catId, ex.getMessage());
					}
				}
			}

			// ── 3. Fetch GLOBAL fallback once (lazy) ─────────────────────────────
			ReturnPolicyDetailDTO globalPolicy = null;
			boolean globalFetched = false;

			// ── 4. Build response for each requested category ─────────────────────
			for (Long catId : categoryIds) {

				// Resolve category name
				String categoryName = null;
				try {
					ProductCategoriesEO cat = productCatRepository.findById(catId).orElse(null);
					if (cat != null) {
						categoryName = cat.getName();
					}
				}
				catch (Exception ex) {
					logger.warn("Could not fetch category name for id={}: {}", catId, ex.getMessage());
				}

				ReturnPolicyDetailDTO policyDetail = null;
				String resolvedVia = null;
				List<MappedProductDTO> mappedProducts = new ArrayList<>();

				ReturnPolicyMappingEO mapping = bestByCategory.get(catId);

				if (mapping != null) {
					// Level 1 — CATEGORY mapping
					policyDetail = buildReturnPolicyDetail(mapping);
					resolvedVia = "CATEGORY";
				}
				else {
					// Level 2 — PRODUCTS mapping (products belonging to this category)
					ReturnPolicyMappingEO productMapping = bestByProductsForCategory.get(catId);
					if (productMapping != null) {
						policyDetail = buildReturnPolicyDetail(productMapping);
						resolvedVia = "PRODUCTS";

						// Build per-product detail list for all products with a mapping
						List<Object[]> pairs = allProductMappingsForCategory.get(catId);
						if (pairs != null) {
							for (Object[] pair : pairs) {
								ProductEO prod = (ProductEO) pair[0];
								ReturnPolicyMappingEO pm = (ReturnPolicyMappingEO) pair[1];
								ReturnPolicyDetailDTO productPolicy = buildReturnPolicyDetail(pm);
								mappedProducts.add(MappedProductDTO.builder()
									.productId(prod.getId().longValue())
									.productName(prod.getName())
									.productSlug(prod.getSlug())
									.mappingPriority(pm.getPriority())
									.returnPolicy(productPolicy)
									.build());
							}
							// Sort by mappingPriority descending (highest first)
							mappedProducts.sort((a, b) -> {
								int pa = a.getMappingPriority() != null ? a.getMappingPriority() : 0;
								int pb = b.getMappingPriority() != null ? b.getMappingPriority() : 0;
								return Integer.compare(pb, pa);
							});
						}
					}
					else {
						// Level 3 — GLOBAL fallback
						if (!globalFetched) {
							globalPolicy = fetchGlobalReturnPolicy();
							globalFetched = true;
						}
						policyDetail = globalPolicy;
						if (policyDetail != null)
							resolvedVia = "GLOBAL";
					}
				}

				logger.debug("getReturnPoliciesByCategories: catId={}, resolvedVia={}", catId, resolvedVia);

				result.add(CategoryReturnPolicyResponseDTO.builder()
					.categoryId(catId)
					.categoryName(categoryName)
					.resolvedVia(resolvedVia)
					.returnPolicy(policyDetail)
					.mappedProducts(mappedProducts.isEmpty() ? null : mappedProducts)
					.build());
			}

		}
		catch (Exception e) {
			logger.error("Error fetching return policies by categories: {}", e.getMessage(), e);
		}

		return result;
	}

	/**
	 * Builds a {@link ReturnPolicyDetailDTO} from a mapping record (fetches conditions).
	 */
	private ReturnPolicyDetailDTO buildReturnPolicyDetail(ReturnPolicyMappingEO mapping) {
		try {
			ReturnPolicyEO policy = mapping.getPolicy();
			if (policy == null)
				return null;

			List<ReturnPolicyConditionEO> conditionEOs = returnPolicyConditionRepository.findByPolicyId(policy.getId());
			List<ReturnPolicyConditionResponseDTO> conditions = conditionEOs.stream()
				.map(c -> ReturnPolicyConditionResponseDTO.builder()
					.id(c.getId())
					.policyId(policy.getId())
					.conditionType(c.getConditionType())
					.conditionValue(c.getConditionValue())
					.build())
				.collect(Collectors.toList());

			return ReturnPolicyDetailDTO.builder()
				.policyId(policy.getId())
				.name(policy.getName())
				.description(policy.getDescription())
				.returnWindowDays(policy.getReturnWindowDays())
				.isReturnable(policy.getIsReturnable())
				.refundType(policy.getRefundType())
				.returnMethod(policy.getReturnMethod())
				.conditions(conditions)
				.build();
		}
		catch (Exception e) {
			logger.warn("Error building return policy detail for mapping id={}: {}", mapping.getId(), e.getMessage());
			return null;
		}
	}

	/**
	 * Fetches the GLOBAL return policy (highest-priority GLOBAL mapping), or null if
	 * none.
	 */
	private ReturnPolicyDetailDTO fetchGlobalReturnPolicy() {
		try {
			// entityId=0 is used as a convention for GLOBAL mappings
			List<ReturnPolicyMappingEO> globalMappings = returnPolicyMappingRepository.findByEntityIdAndEntityType(0L,
					"GLOBAL");
			if (globalMappings == null || globalMappings.isEmpty())
				return null;

			ReturnPolicyMappingEO best = globalMappings.stream()
				.max(java.util.Comparator.comparingInt(m -> m.getPriority() != null ? m.getPriority() : 0))
				.orElse(globalMappings.get(0));

			return buildReturnPolicyDetail(best);
		}
		catch (Exception e) {
			logger.warn("Error fetching global return policy: {}", e.getMessage());
			return null;
		}
	}

	@Override
	public ResponseDTO approveReturnRequest(ReturnApproveRequestDTO request) {
		ResponseDTO response = new ResponseDTO();
		try {
			logger.info("Processing approveReturnRequest for returnId={}, status={}, userId={}", request.getReturnId(),
					request.getStatus(), request.getUserId());

			// Step 1: Validate required fields
			if (request.getReturnId() == null || request.getReturnId().trim().isEmpty()) {
				logger.warn("approveReturnRequest called without returnId");
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Return ID is required");
				return response;
			}
			if (request.getStatus() == null || request.getStatus().trim().isEmpty()) {
				logger.warn("approveReturnRequest called without status");
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Status is required");
				return response;
			}
			if (request.getUserId() == null) {
				logger.warn("approveReturnRequest called without userId");
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("User ID is required");
				return response;
			}
			String newStatus = request.getStatus().trim().toUpperCase();
			if (!Constants.RETURN_STATUS_APPROVED.equals(newStatus)
					&& !Constants.RETURN_STATUS_REJECTED.equals(newStatus)) {
				logger.warn("approveReturnRequest called with invalid status={}", newStatus);
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Invalid status. Allowed values: APPROVED, REJECTED");
				return response;
			}

			// Step 2: Fetch return request by returnId
			ReturnRequestEO returnRequest = returnRequestRepository.findByReturnId(request.getReturnId().trim())
				.orElse(null);
			if (returnRequest == null) {
				logger.warn("Return request not found for returnId={}", request.getReturnId());
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Return request not found: " + request.getReturnId());
				return response;
			}

			// Step 3: Guard — only REQUESTED returns can be approved/rejected
			if (!Constants.RETURN_STATUS_REQUESTED.equals(returnRequest.getStatus())) {
				logger.warn("Return request returnId={} is already in status={}, cannot update", request.getReturnId(),
						returnRequest.getStatus());
				response.setResponseStatus(Constants.FAILURE_STATUS);
				response.setResponseMessage("Return request is already in status: " + returnRequest.getStatus()
						+ ". Only REQUESTED returns can be approved/rejected.");
				return response;
			}

			// Step 4: Update ReturnRequestEO status
			String previousStatus = returnRequest.getStatus();
			returnRequest.setStatus(newStatus);
			returnRequestRepository.save(returnRequest);
			logger.info("Updated returnRequest returnId={} status from '{}' to '{}'", returnRequest.getReturnId(),
					previousStatus, newStatus);

			// Step 5: Save ReturnStatusHistoryEO entry
			ReturnStatusHistoryEO statusHistory = new ReturnStatusHistoryEO();
			statusHistory.setReturnRequest(returnRequest);
			statusHistory.setNewStatus(newStatus);
			statusHistory.setActivityType(newStatus);
			statusHistory.setRemarks(request.getComments() != null && !request.getComments().trim().isEmpty()
					? request.getComments().trim() : (Constants.RETURN_STATUS_APPROVED.equals(newStatus)
							? "Return request approved" : "Return request rejected"));
			statusHistory.setChangedBy(String.valueOf(request.getUserId()));
			statusHistory.setChangedAt(LocalDateTime.now());
			returnStatusHistoryRepository.save(statusHistory);
			logger.info("Saved ReturnStatusHistory for returnId={} with status='{}'", returnRequest.getReturnId(),
					newStatus);

			// Step 6: Update Order and OrderItem status
			OrderEO order = returnRequest.getOrder();
			if (order != null) {
				String orderStatus = Constants.RETURN_STATUS_APPROVED.equals(newStatus) ? "Return Approved"
						: "Return Rejected";
				order.setOrderStatus(orderStatus);
				orderRepository.save(order);

				List<OrderItemEO> orderItems = orderProductRepository.findByOrder(order);
				for (OrderItemEO item : orderItems) {
					item.setStatus(orderStatus);
					orderProductRepository.save(item);
				}
				logger.info("Updated order id={} and {} item(s) status to '{}'", order.getOrderId(), orderItems.size(),
						orderStatus);
			}

			// Step 10: Directly process return order event
			OrderEvent event = OrderEvent.builder()
				.orderId(order.getOrderId() != null ? order.getOrderId().longValue() : null)
				.eventType(Constants.ORDER_EVENT_TYPE_RETURN_REQUEST)
				.build();
			if (event.getOrderId() != null) {
				this.processReturnOrderEvent(event);
				logger.info("Triggered return order processing for orderId={}", event.getOrderId());
			}

			response.setResponseStatus(Constants.STATUS_SUCCESS);
			response.setResponseMessage("Return request " + newStatus.toLowerCase() + " successfully for returnId: "
					+ returnRequest.getReturnId());

		}
		catch (Exception e) {
			logger.error("Error in approveReturnRequest for returnId={}: {}",
					request != null ? request.getReturnId() : null, e.getMessage(), e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to process return request. Please contact support.");
		}
		return response;
	}

	@Override
	public List<ReturnRequestResponseDTO> getReturnRequests(String orderNumber, String status) {
		logger.info("Fetching return requests with orderNumber={}, status={}", orderNumber, status);
		try {
			// Fetch return requests based on optional filters
			List<ReturnRequestEO> returnRequests;
			if (orderNumber != null && !orderNumber.trim().isEmpty() && status != null && !status.trim().isEmpty()) {
				returnRequests = returnRequestRepository.findByOrderOrderNumberAndStatus(orderNumber.trim(),
						status.trim());
			}
			else if (orderNumber != null && !orderNumber.trim().isEmpty()) {
				returnRequests = returnRequestRepository.findByOrderOrderNumber(orderNumber.trim());
			}
			else if (status != null && !status.trim().isEmpty()) {
				returnRequests = returnRequestRepository.findByStatus(status.trim());
			}
			else {
				returnRequests = returnRequestRepository.findAll();
			}

			return returnRequests.stream().map(returnRequest -> {
				// Fetch reason description from reason_master by reasonCode
				String reasonDescription = null;
				if (returnRequest.getReasonCode() != null) {
					ReasonMasterEO reasonMaster = reasonMasterRepository.findByReasonCode(returnRequest.getReasonCode())
						.orElse(null);
					if (reasonMaster != null) {
						reasonDescription = reasonMaster.getReasonDescription();
					}
				}

				// Fetch return policy via product variant from the first order item
				ReturnPolicyDetailDTO returnPolicy = null;
				if (returnRequest.getOrder() != null) {
					List<OrderItemEO> orderItems = orderProductRepository.findByOrder(returnRequest.getOrder());
					if (orderItems != null && !orderItems.isEmpty()) {
						OrderItemEO firstItem = orderItems.get(0);
						if (firstItem.getProductVar() != null && firstItem.getProductVar().getId() != null) {
							returnPolicy = getReturnPolicyByProductVariantId(
									firstItem.getProductVar().getId().longValue());
						}
					}
				}

				// Fetch images
				List<ReturnImageDTO> imageDTOs = returnImageRepository.findByReturnRequest(returnRequest)
					.stream()
					.map(img -> ReturnImageDTO.builder()
						.id(img.getId())
						.imageUrl(img.getImageUrl())
						.imageType(img.getImageType())
						.build())
					.collect(Collectors.toList());

				// Fetch status history
				List<ReturnStatusHistoryDTO> historyDTOs = returnStatusHistoryRepository
					.findByReturnRequest(returnRequest)
					.stream()
					.map(h -> ReturnStatusHistoryDTO.builder()
						.id(h.getId())
						.newStatus(h.getNewStatus())
						.activityType(h.getActivityType())
						.remarks(h.getRemarks())
						.changedBy(h.getChangedBy())
						.changedAt(h.getChangedAt())
						.build())
					.collect(Collectors.toList());

				return ReturnRequestResponseDTO.builder()
					.id(returnRequest.getId())
					.returnId(returnRequest.getReturnId())
					.orderNumber(returnRequest.getOrder() != null ? returnRequest.getOrder().getOrderNumber() : null)
					.userId(returnRequest.getUser() != null ? returnRequest.getUser().getId().longValue() : null)
					.returnType(returnRequest.getReturnType())
					.reasonCode(returnRequest.getReasonCode())
					.reasonDescription(reasonDescription)
					.status(returnRequest.getStatus())
					.userComments(returnRequest.getUserComments())
					.carrier(returnRequest.getCarrier())
					.reverseTrackingNumber(returnRequest.getReverseTrackingNumber())
					.pickupScheduledDate(returnRequest.getPickupScheduledDate())
					.pickupCompletedDate(returnRequest.getPickupCompletedDate())
					.warehouseReceivedDate(returnRequest.getWarehouseReceivedDate())
					.qcStatus(returnRequest.getQcStatus())
					.qcRemarks(returnRequest.getQcRemarks())
					.inspectedAt(returnRequest.getInspectedAt())
					.refundAmount(returnRequest.getRefundAmount())
					.paymentId(returnRequest.getPaymentId())
					.refundId(returnRequest.getRefundId())
					.images(imageDTOs)
					.statusHistory(historyDTOs)
					.returnPolicy(returnPolicy)
					.build();
			}).collect(Collectors.toList());

		}
		catch (Exception e) {
			logger.error("Error fetching return requests with orderNumber={}, status={}: {}", orderNumber, status,
					e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	// ── Reason Master CRUD ──────────────────────────────────────────────────

	@Override
	public ReasonListResponseDTO getAllReasons(String status, String type) {
		logger.info("getAllReasons called with status={}, type={}", status, type);
		try {
			List<ReasonMasterEO> list;
			if (status != null && !status.isBlank() && type != null && !type.isBlank()) {
				list = reasonMasterRepository.findAllByStatusAndType(status.trim(), type.trim());
			}
			else if (status != null && !status.isBlank()) {
				list = reasonMasterRepository.findAllByStatus(status.trim());
			}
			else if (type != null && !type.isBlank()) {
				list = reasonMasterRepository.findByType(type.trim());
			}
			else {
				list = reasonMasterRepository.findAll();
			}
			List<ReasonResponseDTO> reasons = list.stream()
				.map(r -> ReasonResponseDTO.builder()
					.id(r.getId())
					.reasonCode(r.getReasonCode())
					.reasonDescription(r.getReasonDescription())
					.type(r.getType())
					.status(r.getStatus())
					.build())
				.collect(Collectors.toList());
			return ReasonListResponseDTO.builder()
				.reasons(reasons)
				.responseStatus(Constants.SUCCESS_STATUS)
				.responseMessage("Reasons fetched successfully")
				.build();
		}
		catch (Exception e) {
			logger.error("Error in getAllReasons: {}", e.getMessage(), e);
			return ReasonListResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("Failed to fetch reasons")
				.build();
		}
	}

	@Override
	public ReasonResponseDTO getReasonById(Long id) {
		logger.info("getReasonById called with id={}", id);
		try {
			ReasonMasterEO reason = reasonMasterRepository.findById(id).orElse(null);
			if (reason == null) {
				return ReasonResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage("Reason not found for id: " + id)
					.build();
			}
			return ReasonResponseDTO.builder()
				.id(reason.getId())
				.reasonCode(reason.getReasonCode())
				.reasonDescription(reason.getReasonDescription())
				.type(reason.getType())
				.status(reason.getStatus())
				.responseStatus(Constants.SUCCESS_STATUS)
				.responseMessage("Reason fetched successfully")
				.build();
		}
		catch (Exception e) {
			logger.error("Error in getReasonById id={}: {}", id, e.getMessage(), e);
			return ReasonResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("Failed to fetch reason")
				.build();
		}
	}

	@Override
	public ReasonResponseDTO updateReason(Long id, UpdateReasonRequestDTO request) {
		logger.info("updateReason called with id={}", id);
		try {
			ReasonMasterEO reason = reasonMasterRepository.findById(id).orElse(null);
			if (reason == null) {
				return ReasonResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage("Reason not found for id: " + id)
					.build();
			}
			if (request.getReasonDescription() != null && !request.getReasonDescription().isBlank()) {
				reason.setReasonDescription(request.getReasonDescription().trim());
			}
			if (request.getType() != null && !request.getType().isBlank()) {
				reason.setType(request.getType().trim());
			}
			reasonMasterRepository.save(reason);
			logger.info("Reason updated successfully for id={}", id);
			return ReasonResponseDTO.builder()
				.id(reason.getId())
				.reasonCode(reason.getReasonCode())
				.reasonDescription(reason.getReasonDescription())
				.type(reason.getType())
				.status(reason.getStatus())
				.responseStatus(Constants.SUCCESS_STATUS)
				.responseMessage("Reason updated successfully")
				.build();
		}
		catch (Exception e) {
			logger.error("Error in updateReason id={}: {}", id, e.getMessage(), e);
			return ReasonResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("Failed to update reason")
				.build();
		}
	}

	@Override
	public ResponseDTO deleteReason(Long id, ReasonStatusChangeRequestDTO request) {
		logger.info("deleteReason (status change) called with id={}, newStatus={}", id,
				request != null ? request.getStatus() : null);
		try {
			ReasonMasterEO reason = reasonMasterRepository.findById(id).orElse(null);
			if (reason == null) {
				return ResponseDTO.builder()
					.responseStatus(Constants.FAILURE_STATUS)
					.responseMessage("Reason not found for id: " + id)
					.build();
			}
			reason.setStatus(request.getStatus().trim());
			reasonMasterRepository.save(reason);
			logger.info("Reason status changed to '{}' for id={}", request.getStatus(), id);
			return ResponseDTO.builder()
				.responseStatus(Constants.SUCCESS_STATUS)
				.responseMessage("Reason status changed to '" + request.getStatus().trim() + "' successfully")
				.build();
		}
		catch (Exception e) {
			logger.error("Error in deleteReason id={}: {}", id, e.getMessage(), e);
			return ResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("Failed to update reason status")
				.build();
		}
	}

	// ── Order + Shipment combined view ──────────────────────────────────────

	@Override
	public OrderShipmentListResponseDTO getOrdersWithShipments(OrderShipmentSearchRequestDTO request) {
		logger.info(
				"getOrdersWithShipments called with filters: orderStatus={}, orderNumber={}, createdFrom={}, createdTo={}, shipmentNumber={}",
				request.getOrderStatus(), request.getOrderNumber(), request.getOrderCreatedFrom(),
				request.getOrderCreatedTo(), request.getShipmentNumber());
		try {
			// Normalise empty strings to null
			final String orderStatus = blankToNull(request.getOrderStatus());
			final String orderNumber = blankToNull(request.getOrderNumber());
			final String shipmentNumber = blankToNull(request.getShipmentNumber());
			final LocalDateTime createdFrom = request.getOrderCreatedFrom();
			final LocalDateTime createdTo = request.getOrderCreatedTo();

			// Build a JPA Specification dynamically – only apply predicates for non-null
			// values
			Specification<OrderEO> spec = (root, query, cb) -> {
				List<Predicate> predicates = new ArrayList<>();

				if (orderStatus != null) {
					predicates.add(cb.equal(root.get("orderStatus"), orderStatus));
				}
				if (orderNumber != null) {
					predicates.add(cb.equal(root.get("orderNumber"), orderNumber));
				}
				if (createdFrom != null) {
					predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
				}
				if (createdTo != null) {
					predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
				}
				if (shipmentNumber != null) {
					// Subquery: EXISTS (SELECT s FROM shipping WHERE s.order_id =
					// o.order_id AND s.tracking_number = ?)
					Subquery<Long> sub = query.subquery(Long.class);
					Root<ShippingEO> shipRoot = sub.from(ShippingEO.class);
					sub.select(cb.literal(1L))
						.where(cb.equal(shipRoot.get("order"), root),
								cb.equal(shipRoot.get("trackingNumber"), shipmentNumber));
					predicates.add(cb.exists(sub));
				}

				query.distinct(true);
				query.orderBy(cb.desc(root.get("createdAt")));
				return cb.and(predicates.toArray(new Predicate[0]));
			};

			List<OrderEO> orders = orderRepository.findAll(spec);

			List<OrderShipmentDetailDTO> result = orders.stream()
				.map(this::buildOrderShipmentDetail)
				.collect(Collectors.toList());

			return OrderShipmentListResponseDTO.builder().totalCount(result.size()).orders(result).build();

		}
		catch (Exception e) {
			logger.error("Error in getOrdersWithShipments: {}", e.getMessage(), e);
			return OrderShipmentListResponseDTO.builder().totalCount(0).orders(Collections.emptyList()).build();
		}
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

	private OrderShipmentDetailDTO buildOrderShipmentDetail(OrderEO order) {
		// Addresses
		List<OrderAddressEO> addresses = orderAddressRepository.findAllByOrder(order);
		OrderAddressDTO shippingAddr = addresses.stream()
			.filter(a -> "SHIPPING".equalsIgnoreCase(a.getAddressType()))
			.findFirst()
			.map(this::mapAddress)
			.orElse(null);
		OrderAddressDTO billingAddr = addresses.stream()
			.filter(a -> "BILLING".equalsIgnoreCase(a.getAddressType()))
			.findFirst()
			.map(this::mapAddress)
			.orElse(null);

		// Customer info
		String customerName = null;
		String customerEmail = null;
		String customerPhone = null;
		if (order.getCustomer() != null) {
			CustomerEO c = order.getCustomer();
			customerName = (c.getFirstName() != null ? c.getFirstName() : "")
					+ (c.getLastName() != null ? " " + c.getLastName() : "");
			customerName = customerName.trim();
			customerEmail = c.getEmail();
			customerPhone = c.getMobileNumber();
		}

		// Shipments
		List<ShippingEO> shipments = shippingRepository.findByOrder(order);
		List<ShipmentInfoDTO> shipmentInfoList = shipments.stream()
			.map(this::buildShipmentInfo)
			.collect(Collectors.toList());

		return OrderShipmentDetailDTO.builder()
			.orderId(order.getOrderId())
			.orderNumber(order.getOrderNumber())
			.orderStatus(order.getOrderStatus())
			.paymentStatus(order.getPaymentStatus())
			.currency(order.getCurrency())
			.subtotalAmount(order.getSubtotalAmount())
			.taxAmount(order.getTaxAmount())
			.shippingFee(order.getShippingFee())
			.discountAmount(order.getDiscountAmount())
			.totalAmount(order.getTotalAmount())
			.orderCreatedAt(order.getCreatedAt())
			.customerName(customerName)
			.customerEmail(customerEmail)
			.customerPhone(customerPhone)
			.shippingAddress(shippingAddr)
			.billingAddress(billingAddr)
			.shipments(shipmentInfoList)
			.build();
	}

	private OrderAddressDTO mapAddress(OrderAddressEO a) {
		return OrderAddressDTO.builder()
			.name(a.getRecipientName())
			.address1(a.getAddressLine1())
			.address2(a.getAddressLine2())
			.landmark(a.getLandMark())
			.city(a.getCity())
			.state(a.getState())
			.country(a.getCountry())
			.postalCode(a.getPostalCode())
			.build();
	}

	private ShipmentInfoDTO buildShipmentInfo(ShippingEO s) {
		// Tracking history ordered by time ascending
		List<ShipmentTrackingHistoryEO> historyEntries = shipmentTrackingHistoryRepository
			.findByShipmentOrderByUpdatedAtAsc(s);

		List<ShipTrackHistoryDTO> history = historyEntries.stream()
			.map(h -> ShipTrackHistoryDTO.builder()
				.status(h.getStatus())
				.location(h.getLocation())
				.remarks(h.getRemarks())
				.date(h.getUpdatedAt())
				.build())
			.collect(Collectors.toList());

		// Courier candidates from courier_selection_log
		List<CourierSelectionLogDTO> courierCandidates = java.util.Collections.emptyList();
		try {
			if (s.getShipmentId() != null) {
				List<CourierSelectionLogEO> rows = courierSelectionLogRepository
					.findByShipmentIdOrderByRankAsc(s.getShipmentId());
				courierCandidates = rows.stream()
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
			}
		}
		catch (Exception e) {
			logger.warn("Failed to load courier candidates for shipmentId={}: {}", s.getShipmentId(), e.getMessage());
		}

		return ShipmentInfoDTO.builder()
			.shipmentId(s.getShipmentId())
			.trackingNumber(s.getTrackingNumber())
			.courierName(s.getCourierName())
			.courierCompanyId(s.getCourierCompanyId())
			.shipmentType(s.getType())
			.shipmentStatus(s.getShipmentStatus())
			.awb(s.getAwb())
			.labelUrl(s.getLabelUrl())
			.shipOrderId(s.getShipOrderId())
			.shipShipmentId(s.getShipShipmentId())
			.shippedDate(s.getShippedDate())
			.deliveredDate(s.getDeliveredDate())
			.estimatedDeliveryDate(s.getEstimatedDeliveryDate())
			.expectedDeliveryDate(s.getExpectedDeliveryDate())
			.pickupScheduledDate(s.getPickupScheduledDate())
			.trackUrl(s.getTrackUrl())
			.length(s.getLength())
			.breadth(s.getBreadth())
			.height(s.getHeight())
			.weight(s.getWeight())
			.shippingPrice(s.getShippingPrice())
			.courierCandidates(courierCandidates)
			.createdAt(s.getCreatedAt())
			.updatedAt(s.getUpdatedAt())
			.shipmentHistory(history)
			.build();
	}

	// ── Retry Payment ────────────────────────────────────────────────────────

	@Override
	public OrderResponseDTO retryPayment(RetryPaymentRequestDTO request) {
		OrderResponseDTO responseDTO = new OrderResponseDTO();
		try {
			logger.info("retryPayment: orderId={}, orderNumber={}", request.getOrderId(), request.getOrderNumber());

			// 1. Resolve the order
			OrderEO order = null;
			if (request.getOrderNumber() != null && !request.getOrderNumber().isBlank()) {
				order = orderRepository.findByOrderNumber(request.getOrderNumber().trim()).orElse(null);
			}
			if (order == null && request.getOrderId() != null && !request.getOrderId().isBlank()) {
				order = orderRepository.findById(Long.valueOf(request.getOrderId().trim())).orElse(null);
			}
			if (order == null) {
				responseDTO.setStatus(Constants.FAILURE_STATUS);
				responseDTO.setMessage("Order not found. Please check the orderId or orderNumber.");
				return responseDTO;
			}

			// 2. Guard — block retry if already paid / confirmed / cancelled / shipped /
			// delivered
			String currentOrderStatus = order.getOrderStatus();
			if (Constants.ORDER_STATUS_CONFIRMED.equalsIgnoreCase(currentOrderStatus)
					|| Constants.ORDER_STATUS_DELIVERED.equalsIgnoreCase(currentOrderStatus)
					|| Constants.ORDER_STATUS_CANCELLED.equalsIgnoreCase(currentOrderStatus)
					|| Constants.ORDER_STATUS_SHIPPED.equalsIgnoreCase(currentOrderStatus)) {
				responseDTO.setStatus(Constants.FAILURE_STATUS);
				responseDTO.setMessage("Payment retry is not allowed for order in status: " + currentOrderStatus);
				return responseDTO;
			}
			// Allow retry when order is in PAYMENT FAILED or initial CREATED/Submitted
			// state

			// 3. Find existing PaymentEO — block retry if already captured/paid
			PaymentEO payment = paymentRepository.findByOrder(order).orElse(null);
			if (payment != null) {
				String currentPayStatus = payment.getPaymentStatus();
				if (Constants.ORDER_PAYMENT_STATUS_PAID.equalsIgnoreCase(currentPayStatus)
						|| "CAPTURED".equalsIgnoreCase(currentPayStatus)) {
					responseDTO.setStatus(Constants.FAILURE_STATUS);
					responseDTO.setMessage("Payment is already completed for this order.");
					return responseDTO;
				}
			}

			// 4. Determine the amount to charge
			BigDecimal totalAmount = order.getTotalAmount();
			if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
				responseDTO.setStatus(Constants.FAILURE_STATUS);
				responseDTO.setMessage("Cannot determine order amount. Please contact support.");
				return responseDTO;
			}

			// 5. Create a fresh Razorpay order
			JSONObject orderRequest = new JSONObject();
			orderRequest.put("amount", totalAmount.multiply(BigDecimal.valueOf(100)).longValue());
			orderRequest.put("currency", "INR");
			orderRequest.put("receipt", "retry_rcptid_" + System.currentTimeMillis());
			orderRequest.put("payment_capture", 1);
			JSONObject notes = new JSONObject();
			notes.put("order_number", order.getOrderNumber());
			notes.put("retry", "true");
			orderRequest.put("notes", notes);

			Order payOrder = razorpayClient.orders.create(orderRequest);
			String newRazorpayOrderId = (String) payOrder.get("id");
			BigDecimal razorpayAmount = new BigDecimal(((Number) payOrder.get("amount")).longValue())
				.divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

			// 6. Update or create PaymentEO
			if (payment == null) {
				payment = new PaymentEO();
				payment.setOrder(order);
			}
			payment.setPaymentProviderOrderId(newRazorpayOrderId);
			payment.setPaymentStatus("CREATED");
			payment.setAmount(razorpayAmount);
			paymentRepository.save(payment);

			logger.info("retryPayment: new Razorpay orderId={} created for orderNumber={}", newRazorpayOrderId,
					order.getOrderNumber());

			// 7. Build response (same shape as createOrder)
			responseDTO.setOrderNumber(order.getOrderNumber());
			responseDTO.setAmount(razorpayAmount);
			responseDTO.setSubtotalAmount(order.getSubtotalAmount());
			responseDTO.setShippingFee(order.getShippingFee());
			responseDTO.setIsFreeDelivery(
					order.getShippingFee() != null && order.getShippingFee().compareTo(BigDecimal.ZERO) == 0);
			responseDTO.setAmount(razorpayAmount);
			responseDTO.setCurrency(Constants.PAYMENT_CURRENCY);
			responseDTO.setStoreName(Constants.STORE_NAME);
			responseDTO.setDescription(Constants.ORDER_DESCRIPTION);
			responseDTO.setPaymentOrderId(newRazorpayOrderId);
			responseDTO.setPaymentGatewayKey(keyId);
			responseDTO.setStatus(Constants.STATUS_SUCCESS);
			responseDTO.setMessage("Retry payment initiated successfully.");

		}
		catch (Exception e) {
			logger.error("retryPayment error for orderId={}, orderNumber={}: {}", request.getOrderId(),
					request.getOrderNumber(), e.getMessage(), e);
			responseDTO.setStatus(Constants.FAILURE_STATUS);
			responseDTO.setMessage("Failed to initiate retry payment. Please try again later.");
		}
		return responseDTO;
	}

}
