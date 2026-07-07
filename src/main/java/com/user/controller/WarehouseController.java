package com.user.controller;

import com.user.dto.*;
import com.user.service.WarehouseService;
import com.user.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class WarehouseController {

	private static final Logger logger = LoggerFactory.getLogger(WarehouseController.class);

	@Autowired
	private WarehouseService warehouseService;

	@PostMapping("/Create-Warehouse")
	public ResponseEntity<ResponseDTO> createWarehouse(@RequestBody WarehouseCreateDTO warehouseDTO) {
		logger.info("Received request to create warehouse: {}", warehouseDTO);
		ResponseDTO response = new ResponseDTO();
		try {
			response = warehouseService.createWarehouse(warehouseDTO);
			logger.info("Warehouse created successfully: {}", response);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error creating warehouse", e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to create warehouse");
			return ResponseEntity.status(500).body(response);
		}
	}

	@PutMapping("/Update-Warehouse/{warehouseId}")
	public ResponseEntity<ResponseDTO> updateWarehouse(@PathVariable Long warehouseId,
			@RequestBody WarehouseUpdateDTO warehouseDTO) {
		logger.info("Received request to update warehouse id: {}", warehouseId);
		ResponseDTO response = new ResponseDTO();
		try {
			response = warehouseService.updateWarehouse(warehouseId, warehouseDTO);
			logger.info("Warehouse update response: {}", response);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error updating warehouse", e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to update warehouse");
			return ResponseEntity.status(500).body(response);
		}
	}

	@DeleteMapping("/Delete-Warehouse/{warehouseId}")
	public ResponseEntity<ResponseDTO> deleteWarehouse(@PathVariable Long warehouseId) {
		logger.info("Received request to soft-delete warehouse id: {}", warehouseId);
		ResponseDTO response = new ResponseDTO();
		try {
			response = warehouseService.deleteWarehouse(warehouseId);
			logger.info("Warehouse delete response: {}", response);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error deleting warehouse", e);
			response.setResponseStatus(Constants.FAILURE_STATUS);
			response.setResponseMessage("Failed to delete warehouse");
			return ResponseEntity.status(500).body(response);
		}
	}

	@GetMapping("/Get-Warehouse/{warehouseId}")
	public ResponseEntity<?> getWarehouseById(@PathVariable Long warehouseId) {
		logger.info("Received request to fetch warehouse id: {}", warehouseId);
		try {
			WarehouseResponseDTO response = warehouseService.getWarehouseById(warehouseId);
			if (response == null) {
				ResponseDTO notFound = new ResponseDTO();
				notFound.setResponseStatus(Constants.FAILURE_STATUS);
				notFound.setResponseMessage("Warehouse not found or inactive");
				return ResponseEntity.status(404).body(notFound);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error fetching warehouse", e);
			ResponseDTO error = new ResponseDTO();
			error.setResponseStatus(Constants.FAILURE_STATUS);
			error.setResponseMessage("Failed to fetch warehouse");
			return ResponseEntity.status(500).body(error);
		}
	}

	@GetMapping("/Get-All-Warehouses")
	public ResponseEntity<?> getAllWarehouses() {
		logger.info("Received request to fetch all warehouses");
		try {
			List<WarehouseResponseDTO> response = warehouseService.getAllWarehouses();
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error fetching all warehouses", e);
			ResponseDTO error = new ResponseDTO();
			error.setResponseStatus(Constants.FAILURE_STATUS);
			error.setResponseMessage("Failed to fetch warehouses");
			return ResponseEntity.status(500).body(error);
		}
	}

}
