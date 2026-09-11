package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * All the fields required to create a new order on Shiprocket via its "Create Order"
 * API, pre-built from the order's current data so the admin UI can preview / trigger
 * shipment creation without having to re-derive this payload client-side.
 * <p>
 * Mirrors the request built in {@code ShippingServiceImpl.processCreateShipmentEvent}.
 * Field names use the same semantics as Shiprocket's API (snake_case concepts
 * translated to camelCase Java fields).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiprocketOrderPayloadDTO {

	/** Our order_number, sent as Shiprocket's order_id. */
	private String orderId;

	/** Order date formatted d-M-yyyy, as required by Shiprocket. */
	private String orderDate;

	/** Warehouse pickup location nickname registered on Shiprocket. */
	private String pickupLocation;

	private String channelId;

	private String billingCustomerName;

	private String billingLastName;

	private String billingAddress;

	private String billingCity;

	private String billingPincode;

	private String billingState;

	private String billingCountry;

	private String billingEmail;

	private String billingPhone;

	private Boolean shippingIsBilling;

	/** "Prepaid" or "COD" derived from order.paymentStatus. */
	private String paymentMethod;

	private BigDecimal subTotal;

	/** Selected carton's outer dimensions (cm) / total shipment weight (kg). */
	private Double length;

	private Double breadth;

	private Double height;

	private Double weight;

	/** Name of the carton auto-selected to pack this order, if one was found. */
	private String selectedCartonName;

	/**
	 * Populated only if carton selection failed (e.g. no carton fits) — dimensions
	 * above will be null in that case.
	 */
	private String cartonSelectionError;

	private List<ShiprocketOrderItemPayloadDTO> orderItems;

	/** True if a Shiprocket order has already been created for this order's shipment(s). */
	private Boolean alreadyCreatedOnShiprocket;

}

