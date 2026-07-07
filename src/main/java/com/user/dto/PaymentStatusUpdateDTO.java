package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusUpdateDTO {

	private String razorpayOrderId;

	private String razorpayPaymentId;

	private String paymentStatus;

}
