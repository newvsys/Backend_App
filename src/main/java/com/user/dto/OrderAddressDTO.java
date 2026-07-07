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
public class OrderAddressDTO {

	private String name;

	private String address1;

	private String address2;

	private String landmark;

	private String city;

	private String state;

	private String country;

	private String postalCode;

}
