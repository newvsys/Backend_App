package com.user.service;

import com.user.communication.event.OrderEvent;
import com.user.communication.event.ShiprocketOrderEvent;
import com.user.dto.*;

public interface ShippingService {

	void processCreateShipmentEvent(OrderEvent shippingDTO);

	void processShiprocketOrderEvent(ShiprocketOrderEvent event);

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

}
