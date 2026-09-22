package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailWithItemsDTO {

	private Integer orderId;
	private String orderNumber;
	private Integer userId;
	private String orderStatus;
	private String paymentStatus;
	private BigDecimal totalAmount;
	private BigDecimal discountAmount;
	private BigDecimal taxAmount;
	private BigDecimal shippingCost;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	// Delivery Address
	private String deliveryName;
	private String deliveryPhone;
	private String deliveryEmail;
	private String deliveryAddressLine1;
	private String deliveryAddressLine2;
	private String deliveryLandmark;
	private String deliveryCity;
	private String deliveryState;
	private String deliveryPostalCode;
	private String deliveryCountry;

	// Billing Address (if different)
	private String billingName;
	private String billingPhone;
	private String billingAddressLine1;
	private String billingAddressLine2;
	private String billingCity;
	private String billingState;
	private String billingPostalCode;
	private String billingCountry;

	// List of order items with product and variant details
	private List<OrderItemDetailDTO> items;

	// Summary
	private Long itemCount;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OrderItemDetailDTO {
		private Integer orderItemId;
		private Integer quantity;
		private BigDecimal unitPrice;
		private BigDecimal totalPrice;
		private String itemStatus;

		// Product Details
		private Integer productId;
		private String productName;
		private String productDescription;
		private String productSlug;

		// Product Variant Details
		private Integer variantId;
		private String skuCode;
		private String packSize;
		private String uom;
		private String containerType;
		private BigDecimal mrp;
		private BigDecimal sellingPrice;
		private String currency;
		private String variantStatus;

		// Calculated fields
		private BigDecimal discount;
		private BigDecimal discountPercentage;
	}

}

