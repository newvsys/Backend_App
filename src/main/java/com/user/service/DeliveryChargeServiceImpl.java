package com.user.service;

import com.user.dto.*;
import com.user.model.DeliveryChargeEO;
import com.user.repository.DeliveryChargeRepository;
import com.user.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryChargeServiceImpl implements DeliveryChargeService {

	private static final Logger logger = LoggerFactory.getLogger(DeliveryChargeServiceImpl.class);

	@Autowired
	private DeliveryChargeRepository deliveryChargeRepository;

	// ─── helpers ───────────────────────────────────────────────────────────────

	private DeliveryChargeResponseDTO toDTO(DeliveryChargeEO eo) {
		return DeliveryChargeResponseDTO.builder()
			.id(eo.getId())
			.ruleName(eo.getRuleName())
			.minOrderAmount(eo.getMinOrderAmount())
			.maxOrderAmount(eo.getMaxOrderAmount())
			.deliveryCharge(eo.getDeliveryCharge())
			.isFreeDelivery(eo.getIsFreeDelivery())
			.priority(eo.getPriority())
			.status(eo.getStatus())
			.description(eo.getDescription())
			.createdBy(eo.getCreatedBy())
			.updatedBy(eo.getUpdatedBy())
			.createdAt(eo.getCreatedAt())
			.updatedAt(eo.getUpdatedAt())
			.build();
	}

	// ─── create ────────────────────────────────────────────────────────────────

	@Override
	public DeliveryChargeResponseDTO createDeliveryCharge(DeliveryChargeCreateDTO dto) {
		logger.info("Creating delivery charge rule: {}", dto.getRuleName());

		DeliveryChargeEO eo = DeliveryChargeEO.builder()
			.ruleName(dto.getRuleName())
			.minOrderAmount(dto.getMinOrderAmount() != null ? dto.getMinOrderAmount() : BigDecimal.ZERO)
			.maxOrderAmount(dto.getMaxOrderAmount())
			.deliveryCharge(dto.getDeliveryCharge() != null ? dto.getDeliveryCharge() : BigDecimal.ZERO)
			.isFreeDelivery(dto.getDeliveryCharge() != null && dto.getDeliveryCharge().compareTo(BigDecimal.ZERO) == 0)
			.priority(dto.getPriority() != null ? dto.getPriority() : 100)
			.status(dto.getStatus() != null && !dto.getStatus().isBlank() ? dto.getStatus() : "A")
			.description(dto.getDescription())
			.createdBy(dto.getCreatedBy())
			.build();

		eo = deliveryChargeRepository.save(eo);
		logger.info("Delivery charge rule created with id={}", eo.getId());
		return toDTO(eo);
	}

	// ─── update ────────────────────────────────────────────────────────────────

	@Override
	public DeliveryChargeResponseDTO updateDeliveryCharge(Long id, DeliveryChargeUpdateDTO dto) {
		logger.info("Updating delivery charge rule id={}", id);

		DeliveryChargeEO eo = deliveryChargeRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Delivery charge rule not found with id=" + id));

		if (dto.getRuleName() != null && !dto.getRuleName().isBlank()) {
			eo.setRuleName(dto.getRuleName());
		}
		if (dto.getMinOrderAmount() != null) {
			eo.setMinOrderAmount(dto.getMinOrderAmount());
		}
		if (dto.getMaxOrderAmount() != null) {
			eo.setMaxOrderAmount(dto.getMaxOrderAmount());
		}
		if (dto.getDeliveryCharge() != null) {
			eo.setDeliveryCharge(dto.getDeliveryCharge());
			eo.setIsFreeDelivery(dto.getDeliveryCharge().compareTo(BigDecimal.ZERO) == 0);
		}
		if (dto.getPriority() != null) {
			eo.setPriority(dto.getPriority());
		}
		if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
			eo.setStatus(dto.getStatus());
		}
		if (dto.getDescription() != null) {
			eo.setDescription(dto.getDescription());
		}
		if (dto.getUpdatedBy() != null) {
			eo.setUpdatedBy(dto.getUpdatedBy());
		}

		eo = deliveryChargeRepository.save(eo);
		logger.info("Delivery charge rule id={} updated successfully", id);
		return toDTO(eo);
	}

	// ─── soft-delete ───────────────────────────────────────────────────────────

	@Override
	public ResponseDTO deleteDeliveryCharge(Long id) {
		logger.info("Soft-deleting delivery charge rule id={}", id);

		DeliveryChargeEO eo = deliveryChargeRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Delivery charge rule not found with id=" + id));

		eo.setStatus("I");
		deliveryChargeRepository.save(eo);
		logger.info("Delivery charge rule id={} deactivated", id);

		return ResponseDTO.builder()
			.responseStatus(Constants.SUCCESS_STATUS)
			.responseMessage("Delivery charge rule deactivated successfully")
			.build();
	}

	// ─── get by id ─────────────────────────────────────────────────────────────

	@Override
	public DeliveryChargeResponseDTO getDeliveryChargeById(Long id) {
		logger.info("Fetching delivery charge rule id={}", id);
		DeliveryChargeEO eo = deliveryChargeRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Delivery charge rule not found with id=" + id));
		return toDTO(eo);
	}

	// ─── get all ───────────────────────────────────────────────────────────────

	@Override
	public DeliveryChargeListResponseDTO getAllDeliveryCharges(String status) {
		logger.info("Fetching all delivery charge rules, status filter={}", status);

		List<DeliveryChargeEO> list;
		if (status != null && !status.isBlank()) {
			list = deliveryChargeRepository.findByStatusOrderByPriorityAsc(status.trim().toUpperCase());
		}
		else {
			list = deliveryChargeRepository.findAllByOrderByPriorityAsc();
		}

		List<DeliveryChargeResponseDTO> dtos = list.stream().map(this::toDTO).collect(Collectors.toList());

		return DeliveryChargeListResponseDTO.builder()
			.responseStatus(Constants.SUCCESS_STATUS)
			.responseMessage("Delivery charge rules fetched successfully")
			.deliveryCharges(dtos)
			.build();
	}

	// ─── calculate ─────────────────────────────────────────────────────────────

	@Override
	@Transactional(readOnly = true)
	public DeliveryChargeCalculateResponseDTO calculateDeliveryCharge(BigDecimal orderAmount) {
		logger.info("Calculating delivery charge for orderAmount={}", orderAmount);

		// Optimisation: pass PageRequest.of(0,1) so the DB returns at most 1 row
		// instead of fetching every matching rule and discarding all but the first.
		List<DeliveryChargeEO> matches = deliveryChargeRepository.findMatchingRules(orderAmount, PageRequest.of(0, 1));

		if (matches.isEmpty()) {
			logger.warn("No delivery charge rule found for orderAmount={}", orderAmount);
			return DeliveryChargeCalculateResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("No matching delivery charge rule found for the given order amount")
				.orderAmount(orderAmount)
				.build();
		}

		DeliveryChargeEO matched = matches.get(0);
		logger.info("Matched delivery charge rule id={}, charge={}", matched.getId(), matched.getDeliveryCharge());

		return DeliveryChargeCalculateResponseDTO.builder()
			.responseStatus(Constants.SUCCESS_STATUS)
			.responseMessage("Delivery charge calculated successfully")
			.orderAmount(orderAmount)
			.applicableDeliveryCharge(matched.getDeliveryCharge())
			.isFreeDelivery(matched.getIsFreeDelivery())
			.matchedRule(toDTO(matched))
			.build();
	}

}
