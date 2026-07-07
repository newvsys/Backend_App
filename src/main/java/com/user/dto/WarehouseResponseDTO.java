package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseResponseDTO {

	private Long warehouseId;

	private String warehouseName;

	private String warehouseCode;

	private String channelId;

	private String contactPerson;

	private String contactNumber;

	private String email;

	private String addressLine1;

	private String addressLine2;

	private String city;

	private String state;

	private String postalCode;

	private String country;

	private Double latitude;

	private Double longitude;

	private String status;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
