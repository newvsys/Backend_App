package com.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "shipment_id")
	private Long shipmentId;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "order_id", nullable = false)
	private OrderEO order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id")
	private WarehouseEO warehouse;

	@Column(name = "tracking_number")
	private String trackingNumber;

	@Column(name = "courier_name")
	private String courierName;

	@Column(name = "shipment_type")
	private String type; // FORWARD | RETURN_PICKUP

	@Column(name = "shipment_status")
	private String shipmentStatus;

	@Column(name = "shipped_date")
	private LocalDateTime shippedDate;

	@Column(name = "delivered_date")
	private LocalDateTime deliveredDate;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "length")
	private Double length;

	@Column(name = "breadth")
	private Double breadth;

	@Column(name = "height")
	private Double height;

	@Column(name = "weight")
	private Double weight;

	@Column(name = "awb")
	private String awb;

	@Column(name = "labelUrl")
	private String labelUrl;

	@Column(name = "ship_order_id")
	private Integer shipOrderId;

	@Column(name = "ship_shipment_id")
	private Integer shipShipmentId;

	@Column(name = "pickup_id")
	private Long pickupId;

	@Column(name = "pickup_scheduled_date")
	private LocalDateTime pickupScheduledDate;

	@Column(name = "pickup_token", length = 255)
	private String pickupToken;

	@Column(name = "courier_company_id")
	private Integer courierCompanyId;

	@Column(name = "estimated_delivery_date")
	private LocalDateTime estimatedDeliveryDate;

	@Column(name = "expected_delivery_date")
	private LocalDateTime expectedDeliveryDate;

	@Column(name = "track_url", length = 500)
	private String trackUrl;

	@Column(name = "shipping_price", precision = 10, scale = 2)
	private BigDecimal shippingPrice;

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
