package com.user.service;

import com.user.dto.*;

import java.util.List;

public interface WarehouseService {

	ResponseDTO createWarehouse(WarehouseCreateDTO warehouseDTO);

	ResponseDTO updateWarehouse(Long warehouseId, WarehouseUpdateDTO warehouseDTO);

	ResponseDTO deleteWarehouse(Long warehouseId);

	WarehouseResponseDTO getWarehouseById(Long warehouseId);

	List<WarehouseResponseDTO> getAllWarehouses();

}
