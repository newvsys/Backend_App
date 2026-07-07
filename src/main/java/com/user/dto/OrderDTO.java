package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

	// match OrderEO primary key type/name
	private Integer orderId;

	// or String, UUID, etc. per OrderEO
	private Integer userId;

	private String name;

	private String phone;

	private String email;

	private String address1;

	private String address2;

	private String landmark;

	private String city;

	private String postalCode;

	private String country;

	private String status;

	// adjust to OrderEO type
	private BigDecimal total;

}
