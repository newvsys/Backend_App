package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {

	private Integer customerId;

	private String userId;

	private String firstName;

	private String email;

	private String mobileNumber;

	private String customerType;

	private String status;

	private List<CustomerAddressDTO> addresses;

}
