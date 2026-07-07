package com.user.service;

import com.user.dto.*;
import com.user.model.WarehouseEO;
import com.user.repository.WarehouseRepository;
import com.user.utility.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WarehouseServiceImpl implements WarehouseService {

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Override
	public ResponseDTO createWarehouse(WarehouseCreateDTO warehouseDTO) {
		ResponseDTO resultDTO = new ResponseDTO();
		try {
			WarehouseEO warehouseEO = new WarehouseEO();
			if (warehouseDTO.getWarehouseName() != null)
				warehouseEO.setWarehouseName(warehouseDTO.getWarehouseName());
			if (warehouseDTO.getWarehouseCode() != null)
				warehouseEO.setWarehouseCode(warehouseDTO.getWarehouseCode());
			if (warehouseDTO.getChannelId() != null)
				warehouseEO.setChannelId(warehouseDTO.getChannelId());
			if (warehouseDTO.getContactPerson() != null)
				warehouseEO.setContactPerson(warehouseDTO.getContactPerson());
			if (warehouseDTO.getContactNumber() != null)
				warehouseEO.setContactNumber(warehouseDTO.getContactNumber());
			if (warehouseDTO.getEmail() != null)
				warehouseEO.setEmail(warehouseDTO.getEmail());
			if (warehouseDTO.getAddressLine1() != null)
				warehouseEO.setAddressLine1(warehouseDTO.getAddressLine1());
			if (warehouseDTO.getAddressLine2() != null)
				warehouseEO.setAddressLine2(warehouseDTO.getAddressLine2());
			if (warehouseDTO.getCity() != null)
				warehouseEO.setCity(warehouseDTO.getCity());
			if (warehouseDTO.getState() != null)
				warehouseEO.setState(warehouseDTO.getState());
			if (warehouseDTO.getPostalCode() != null)
				warehouseEO.setPostalCode(warehouseDTO.getPostalCode());
			if (warehouseDTO.getCountry() != null)
				warehouseEO.setCountry(warehouseDTO.getCountry());
			if (warehouseDTO.getLatitude() != null)
				warehouseEO.setLatitude(warehouseDTO.getLatitude());
			if (warehouseDTO.getLongitude() != null)
				warehouseEO.setLongitude(warehouseDTO.getLongitude());
			warehouseEO.setStatus(Constants.STATUS_ACTIVE);
			warehouseRepository.save(warehouseEO);
			resultDTO.setResponseStatus(Constants.SUCCESS_STATUS);
			resultDTO.setResponseMessage("Warehouse created successfully");
		}
		catch (Exception e) {
			resultDTO.setResponseStatus(Constants.FAILURE_STATUS);
			resultDTO.setResponseMessage("Failed to create warehouse: " + e.getMessage());
		}
		return resultDTO;
	}

	@Override
	public ResponseDTO updateWarehouse(Long warehouseId, WarehouseUpdateDTO warehouseDTO) {
		ResponseDTO resultDTO = new ResponseDTO();
		try {
			Optional<WarehouseEO> optionalWarehouse = warehouseRepository.findByWarehouseIdAndStatus(warehouseId,
					Constants.STATUS_ACTIVE);
			if (optionalWarehouse.isEmpty()) {
				resultDTO.setResponseStatus(Constants.FAILURE_STATUS);
				resultDTO.setResponseMessage("Warehouse not found or inactive");
				return resultDTO;
			}
			WarehouseEO warehouseEO = optionalWarehouse.get();
			if (warehouseDTO.getWarehouseName() != null)
				warehouseEO.setWarehouseName(warehouseDTO.getWarehouseName());
			if (warehouseDTO.getChannelId() != null)
				warehouseEO.setChannelId(warehouseDTO.getChannelId());
			if (warehouseDTO.getContactPerson() != null)
				warehouseEO.setContactPerson(warehouseDTO.getContactPerson());
			if (warehouseDTO.getContactNumber() != null)
				warehouseEO.setContactNumber(warehouseDTO.getContactNumber());
			if (warehouseDTO.getEmail() != null)
				warehouseEO.setEmail(warehouseDTO.getEmail());
			if (warehouseDTO.getAddressLine1() != null)
				warehouseEO.setAddressLine1(warehouseDTO.getAddressLine1());
			if (warehouseDTO.getAddressLine2() != null)
				warehouseEO.setAddressLine2(warehouseDTO.getAddressLine2());
			if (warehouseDTO.getCity() != null)
				warehouseEO.setCity(warehouseDTO.getCity());
			if (warehouseDTO.getState() != null)
				warehouseEO.setState(warehouseDTO.getState());
			if (warehouseDTO.getPostalCode() != null)
				warehouseEO.setPostalCode(warehouseDTO.getPostalCode());
			if (warehouseDTO.getCountry() != null)
				warehouseEO.setCountry(warehouseDTO.getCountry());
			if (warehouseDTO.getLatitude() != null)
				warehouseEO.setLatitude(warehouseDTO.getLatitude());
			if (warehouseDTO.getLongitude() != null)
				warehouseEO.setLongitude(warehouseDTO.getLongitude());
			if (warehouseDTO.getStatus() != null)
				warehouseEO.setStatus(warehouseDTO.getStatus());
			warehouseRepository.save(warehouseEO);
			resultDTO.setResponseStatus(Constants.SUCCESS_STATUS);
			resultDTO.setResponseMessage("Warehouse updated successfully");
		}
		catch (Exception e) {
			resultDTO.setResponseStatus(Constants.FAILURE_STATUS);
			resultDTO.setResponseMessage("Failed to update warehouse: " + e.getMessage());
		}
		return resultDTO;
	}

	@Override
	public ResponseDTO deleteWarehouse(Long warehouseId) {
		ResponseDTO resultDTO = new ResponseDTO();
		try {
			Optional<WarehouseEO> optionalWarehouse = warehouseRepository.findByWarehouseIdAndStatus(warehouseId,
					Constants.STATUS_ACTIVE);
			if (optionalWarehouse.isEmpty()) {
				resultDTO.setResponseStatus(Constants.FAILURE_STATUS);
				resultDTO.setResponseMessage("Warehouse not found or already inactive");
				return resultDTO;
			}
			WarehouseEO warehouseEO = optionalWarehouse.get();
			warehouseEO.setStatus(Constants.STATUS_INACTIVE);
			warehouseRepository.save(warehouseEO);
			resultDTO.setResponseStatus(Constants.SUCCESS_STATUS);
			resultDTO.setResponseMessage("Warehouse deleted successfully");
		}
		catch (Exception e) {
			resultDTO.setResponseStatus(Constants.FAILURE_STATUS);
			resultDTO.setResponseMessage("Failed to delete warehouse: " + e.getMessage());
		}
		return resultDTO;
	}

	@Override
	public WarehouseResponseDTO getWarehouseById(Long warehouseId) {
		Optional<WarehouseEO> optionalWarehouse = warehouseRepository.findByWarehouseIdAndStatus(warehouseId,
				Constants.STATUS_ACTIVE);
		return optionalWarehouse.map(this::mapToResponseDTO).orElse(null);
	}

	@Override
	public List<WarehouseResponseDTO> getAllWarehouses() {
		return warehouseRepository.findAllByStatus(Constants.STATUS_ACTIVE)
			.stream()
			.map(this::mapToResponseDTO)
			.collect(Collectors.toList());
	}

	private WarehouseResponseDTO mapToResponseDTO(WarehouseEO eo) {
		return WarehouseResponseDTO.builder()
			.warehouseId(eo.getWarehouseId())
			.warehouseName(eo.getWarehouseName())
			.warehouseCode(eo.getWarehouseCode())
			.channelId(eo.getChannelId())
			.contactPerson(eo.getContactPerson())
			.contactNumber(eo.getContactNumber())
			.email(eo.getEmail())
			.addressLine1(eo.getAddressLine1())
			.addressLine2(eo.getAddressLine2())
			.city(eo.getCity())
			.state(eo.getState())
			.postalCode(eo.getPostalCode())
			.country(eo.getCountry())
			.latitude(eo.getLatitude())
			.longitude(eo.getLongitude())
			.status(eo.getStatus())
			.createdAt(eo.getCreatedAt())
			.updatedAt(eo.getUpdatedAt())
			.build();
	}

}
