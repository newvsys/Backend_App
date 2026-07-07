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
public class RefundOrderDetailsDTO {

	private String orderNumber;

	private String orderStatus;

	private BigDecimal totalAmount;

	private String currency;

	private LocalDateTime orderDate;

	private List<RefundOrderItemDTO> items;

}
