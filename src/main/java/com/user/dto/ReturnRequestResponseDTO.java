package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestResponseDTO {

	private Long id;

	private String returnId;

	private String orderNumber;

	private Long userId;

	private String returnType;

	private String reasonCode;

	private String reasonDescription;

	private String status;

	private String userComments;

	private String carrier;

	private String reverseTrackingNumber;

	private LocalDate pickupScheduledDate;

	private LocalDate pickupCompletedDate;

	private LocalDate warehouseReceivedDate;

	private String qcStatus;

	private String qcRemarks;

	private LocalDateTime inspectedAt;

	private BigDecimal refundAmount;

	private String paymentId;

	private String refundId;

	private List<ReturnImageDTO> images;

	private List<ReturnStatusHistoryDTO> statusHistory;

	private ReturnPolicyDetailDTO returnPolicy;

}
