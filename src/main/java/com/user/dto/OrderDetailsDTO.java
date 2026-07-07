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
public class OrderDetailsDTO {

	private String orderNumber;

	private String status;

	private BigDecimal totalAmount;

	private String currency;

	private String orderId;

	private LocalDateTime orderDate;

	// Customer / Shipping Details
	private OrderAddressDTO shippingAddress;

	private List<OrderStatusProd> products;

	private List<OrderShippingDTO> shippingProducts;

}
