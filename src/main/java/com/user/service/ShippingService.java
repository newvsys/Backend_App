package com.user.service;

import com.user.communication.event.ShiprocketOrderEvent;
import com.user.dto.*;
import com.user.model.OrderEO;

public interface ShippingService {

	ShippingResponseDTO processCreateShipmentEvent(ShippingRequestDTO shippingDTO);

	ShiprocketOrderEventResponseDTO processShiprocketOrderEvent(ShiprocketOrderEvent event);

	ShipTrackHistoryResponseDTO getShippingHistory(ShipTrackHistoryRequestDTO requestDTO);

	ShipStatusUpdateResponseDTO shipmentStatusUpdate(ShipStatusUpdateRequestDTO requestDTO);

	AllShipmentsResponseDTO getAllShipments(String status, String orderNumber);

	ResponseCreateCartonDTO addCarton(RequestCreateCartonDTO requestCreateCartonDTO);

	ResponseCreateCartonDTO getCartonById(Long id);

	CartonListResponseDTO getAllCartons(String status);

	ResponseCreateCartonDTO updateCarton(Long id, CartonUpdateRequestDTO request);

	ResponseDTO deleteCarton(Long id, CartonStatusChangeRequestDTO request);

	// Manual Shiprocket step APIs
	AwbResponse generateAwb(AwbRequest request);

	PickupResponse requestPickup(PickupRequest request);

	LabelResponse generateLabel(LabelRequest request);

	// Track shipment by AWB code (DB + live Shiprocket data)
	TrackShipmentResponseDTO trackShipment(String awbCode);

	/**
	 * Manually create or update a shipment order and its tracking history. Used when the
	 * automated Shiprocket flow (CREATE_ORDER / GENERATE_AWB / REQUEST_PICKUP /
	 * GENERATE_LABEL) has failed and an admin needs to supply the missing data manually.
	 */
	ManualShiprocketUpdateResponseDTO manualShiprocketUpdate(ManualShiprocketUpdateRequestDTO request);

	// ── Order-number-based shipment management APIs ───────────────────────────

	/**
	 * GET — Fetch full shipping details (including tracking history) for the given order
	 * number. Returns FAILURE if no shipping record is found.
	 */
	ShippingDetailResponseDTO getShippingDetailsByOrderNumber(String orderNumber);

	/**
	 * PUT — Update an existing shipping record identified by order number. Only non-null
	 * fields in the request are applied. Returns FAILURE if no shipping record exists for
	 * the order.
	 */
	ManualShiprocketUpdateResponseDTO updateShippingByOrderNumber(String orderNumber, ShippingOrderRequestDTO request);

	/**
	 * POST — Create a brand-new shipping record for the given order number. Returns
	 * FAILURE if a shipping record already exists for the order (use PUT to update it
	 * instead).
	 */
	ManualShiprocketUpdateResponseDTO createShippingByOrderNumber(String orderNumber, ShippingOrderRequestDTO request);

	/**
	 * GET — Fetch the live Shiprocket shipment data for the given internal order number,
	 * returned in the exact shape expected by the PUT
	 * /api/shipment/order/{orderNumber} request body. Resolves the linked
	 * Shiprocket order/shipment via the local DB, then refreshes AWB / courier /
	 * status fields from the live Shiprocket API (falls back to the last known
	 * DB values if the live call fails).
	 */
	ShipmentPutPayloadResponseDTO getShiprocketPutPayloadByOrderNumber(String orderNumber);

	/**
	 * POST — Manually retrigger the Shiprocket shipping process for all active FORWARD
	 * shipments belonging to the given internal order id. Intended for the admin UI to
	 * re-attempt a failed shipment (e.g. status MANUAL_PROCESSING_REQUIRED, or stuck
	 * without an AWB/label) after some time has passed, without creating a duplicate
	 * Shiprocket order if one already exists for the shipment.
	 * without an AWB/label) after some time has passed, without creating a duplicate
	 * Shiprocket order if one already exists for the shipment.
	 * <p>
	 * This operation is safe to call multiple times for the same order number: any
	 * shipment that has already been fully processed (AWB assigned, pickup scheduled,
	 * label generated and tracking URL captured) is left untouched and simply reported
	 * back as SKIPPED — it will not be reprocessed/duplicated.
	 * @param orderNumber the customer-facing order number (OrderEO.orderNumber)
	 */
	RetriggerShippingResponseDTO retriggerShippingProcess(String orderNumber);

	/**
	 * GET — Fetch all orders whose status is "Confirmed" or "Ready to Ship" (Order With
	 * Shipping) AND whose linked shipment has at least one Shiprocket step status equal
	 * to FAILURE, across: shiprocket_order_status, generate_awb_status,
	 * request_pickup_status, generate_label_status, track_shipment_status,
	 * estimate_status. Each result combines the parent Order details with the
	 * corresponding ShippingEO (shipment) details, plus the list of steps that failed.
	 */
	FailedShiprocketOrdersResponseDTO getConfirmedOrReadyToShipOrdersWithFailedShiprocketStep();

	/**
	 * GET — Fetch the list of available courier services (excluding blocklisted
	 * couriers) for the given internal order id. Pickup postcode, delivery
	 * postcode, and shipment weight/dimensions are resolved from the order's
	 * existing shipment (if any) and shipping address, mirroring the internal
	 * serviceability lookup used by {@code executeFindBestCourierStep} during
	 * automated Shiprocket processing.
	 * @param orderId internal order id (OrderEO.orderId)
	 */
	AvailableCourierServicesResponseDTO getAvailableCourierServicesByOrderId(Long orderId);

	/**
	 * GET — Fetch shipping details by shipment ID. Returns the complete shipping
	 * record with all fields and tracking history.
	 */
	ShippingDetailResponseDTO getShippingByShipmentId(Long shipmentId);

	/**
	 * POST — Create a new shipping record directly with all fields. Used to manually
	 * save shipping details when creating from shipment ID.
	 */
	ManualShiprocketUpdateResponseDTO saveShipping(ShippingOrderRequestDTO request);

	// ── Order ID-based shipment management APIs ──────────────────────────────

	/**
	 * GET — Fetch all shipping records for a given order ID.
	 * Called by: GET /api/order/{orderId}/shipping
	 */
	ShippingEntityResponseDTO getShippingsByOrderId(Long orderId);

	/**
	 * POST — Create a new shipping record for the given order ID with all fields.
	 * Called by: POST /api/order/{orderId}/shipping
	 */
	ShippingEntityResponseDTO createShippingForOrder(Long orderId, CreateShippingRequestDTO request);

	/**
	 * Create an initial shipment record after successful payment confirmation.
	 * Creates a ShippingEO with PENDING status and a corresponding ShipmentTrackingHistoryEO
	 * with status "Order Confirmed" and warehouse location.
	 * This is a best-effort operation that won't fail the payment process if it encounters errors.
	 */
	void createInitialShipmentAfterPayment(OrderEO order);

}
