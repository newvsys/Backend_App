package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO used to create an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateDTO {

	private String status;

	private Integer total;

	// Optional, can be null if not logged in
	private String userId;

	private Integer customerId;

	private Integer orderAddressId;

	private List<OrderProductRequestDTO> products;

	private String address1;

	private String address2;

	private String city;

	private String country;

	private String email;

	private String landmark;

	private String name;

	private String phone;

	private String postalCode;

	private String state;

}
