package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceabilityRequestDTO {

	/** Postcode from where the order will be picked. (REQUIRED) */
	private Integer pickupPostcode;

	/** Postcode where the order will be delivered. (REQUIRED) */
	private Integer deliveryPostcode;

	/**
	 * 1 for Cash on Delivery, 0 for Prepaid. (CONDITIONAL — required if order_id not
	 * provided)
	 */
	private Integer cod;

	/** Weight of shipment in kgs. (CONDITIONAL — required if order_id not provided) */
	private String weight;

	/** Shiprocket order id (if already created). (OPTIONAL) */
	private Integer orderId;

	/** Length of the shipment in cms. (OPTIONAL) */
	private Integer length;

	/** Breadth of the shipment in cms. (OPTIONAL) */
	private Integer breadth;

	/** Height of the shipment in cms. (OPTIONAL) */
	private Integer height;

	/** Price of the order shipment in rupees. (OPTIONAL) */
	private Integer declaredValue;

	/** Mode of travel: "Surface" or "Air". (OPTIONAL) */
	private String mode;

	/** 1 if return order, 0 if not. declared_value required if set. (OPTIONAL) */
	private Integer isReturn;

	/** Filter to show only "documents" couriers (accepted value: 1). (OPTIONAL) */
	private Integer couriersType;

	/** Filter to show only Hyperlocal couriers (accepted value: 1). (OPTIONAL) */
	private Integer onlyLocal;

	/** Filter to show only QC-enabled couriers. is_return must be 1. (CONDITIONAL) */
	private Integer qcCheck;

	/** Warehouse name to resolve pickup postcode dynamically. (INTERNAL USE) */
	private String warehouseName;

}
