package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddressDTO {

	private Integer addressId; // null for new addresses

	private String addressType;

	private String recipientName;

	private String addressLine1;

	private String addressLine2;

	private String landMark;

	private String city;

	private String state;

	private String country;

	private String postalCode;

	private String contactNumber;

}
