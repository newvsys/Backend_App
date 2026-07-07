package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRecordDTO {

	private Long refundTransactionId;

	private String refundReference;

	private String gatewayRefundId;

	private String status;

	private String refundType;

	private String refundReason;

	private String failureReason;

	private BigDecimal requestedAmount;

	private BigDecimal approvedAmount;

	private BigDecimal refundedAmount;

	private String currency;

	private LocalDateTime createdAt;

	private String customerName;

	private String customerMobile;

	private RefundOrderDetailsDTO order;

}
