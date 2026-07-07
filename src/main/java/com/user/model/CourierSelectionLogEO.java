package com.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stores all candidate couriers evaluated for a shipment via the Shiprocket
 * Serviceability API, along with which courier was ultimately selected.
 *
 * Table: courier_selection_log
 */
@Entity
@Table(name = "courier_selection_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierSelectionLogEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Our internal order ID (orders.order_id) */
	@Column(name = "order_id")
	private Long orderId;

	/** Human-readable order number (orders.order_number) */
	@Column(name = "order_number", length = 100)
	private String orderNumber;

	/** Our internal shipment ID (shipping.shipment_id) */
	@Column(name = "shipment_id")
	private Long shipmentId;

	/** Shiprocket's shipment_id returned after createOrder */
	@Column(name = "ship_shipment_id")
	private Integer shipShipmentId;

	/** Shiprocket courier_company_id */
	@Column(name = "courier_company_id")
	private Integer courierCompanyId;

	/** Human-readable courier name (e.g. "Delhivery Surface") */
	@Column(name = "courier_name", length = 200)
	private String courierName;

	/** Quoted shipping rate from serviceability API */
	@Column(name = "rate", precision = 10, scale = 2)
	private BigDecimal rate;

	/** Estimated delivery days from serviceability API */
	@Column(name = "estimated_delivery_days")
	private Double estimatedDeliveryDays;

	/**
	 * Rank among candidates (1 = best/cheapest-fastest, 2 = second-best, …). Lower rank
	 * was attempted first for AWB generation.
	 */
	@Column(name = "rank")
	private Integer rank;

	/**
	 * TRUE when this courier was the one finally used to generate the AWB. Only one row
	 * per (shipment_id) should have is_selected = true.
	 */
	@Column(name = "is_selected")
	private Boolean isSelected;

	/** AWB code set once this courier was used successfully. */
	@Column(name = "awb_code", length = 100)
	private String awbCode;

	/** Actual shipping price charged (from AWB response freight_charge/rate). */
	@Column(name = "shipping_price", precision = 10, scale = 2)
	private BigDecimal shippingPrice;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

}
