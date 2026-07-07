package com.user.communication.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsDetails {

	private Long shipmentId;

	private Long orderId;

	private Long warehouseId;

}
