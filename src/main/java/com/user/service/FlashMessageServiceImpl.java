package com.user.service;

import com.user.dto.FlashMessageCreateDTO;
import com.user.dto.FlashMessageListResponseDTO;
import com.user.dto.FlashMessageResponseDTO;
import com.user.dto.ResponseDTO;
import com.user.model.FlashMessageEO;
import com.user.repository.FlashMessageRepository;
import com.user.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlashMessageServiceImpl implements FlashMessageService {

	private static final Logger logger = LoggerFactory.getLogger(FlashMessageServiceImpl.class);

	@Autowired
	private FlashMessageRepository flashMessageRepository;

	// ─── helper: EO → DTO ────────────────────────────────────────────────────

	private FlashMessageResponseDTO toDTO(FlashMessageEO eo) {
		return FlashMessageResponseDTO.builder()
			.id(eo.getId())
			.title(eo.getTitle())
			.message(eo.getMessage())
			.type(eo.getType())
			.bgColor(eo.getBgColor())
			.textColor(eo.getTextColor())
			.speed(eo.getSpeed())
			.priority(eo.getPriority())
			.linkUrl(eo.getLinkUrl())
			.startDate(eo.getStartDate())
			.endDate(eo.getEndDate())
			.status(eo.getStatus())
			.createdBy(eo.getCreatedBy())
			.updatedBy(eo.getUpdatedBy())
			.createdAt(eo.getCreatedAt())
			.updatedAt(eo.getUpdatedAt())
			.build();
	}

	// ─── create ───────────────────────────────────────────────────────────────

	@Override
	@Transactional
	public FlashMessageResponseDTO createFlashMessage(FlashMessageCreateDTO dto) {
		logger.info("Creating flash message: title={}", dto.getTitle());

		FlashMessageEO eo = FlashMessageEO.builder()
			.title(dto.getTitle())
			.message(dto.getMessage())
			.type(dto.getType())
			.bgColor(dto.getBgColor())
			.textColor(dto.getTextColor())
			.speed(dto.getSpeed())
			.priority(dto.getPriority() != null ? dto.getPriority() : 100)
			.linkUrl(dto.getLinkUrl())
			.startDate(dto.getStartDate())
			.endDate(dto.getEndDate())
			.status(dto.getStatus() != null && !dto.getStatus().isBlank() ? dto.getStatus().toUpperCase() : "A")
			.createdBy(dto.getUpdatedBy())
			.updatedBy(dto.getUpdatedBy())
			.build();

		eo = flashMessageRepository.save(eo);
		logger.info("Flash message created with id={}", eo.getId());
		return toDTO(eo);
	}

	// ─── update ───────────────────────────────────────────────────────────────

	@Override
	@Transactional
	public FlashMessageResponseDTO updateFlashMessage(Long id, FlashMessageCreateDTO dto) {
		logger.info("Updating flash message id={}", id);

		FlashMessageEO eo = flashMessageRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Flash message not found with id=" + id));

		if (dto.getTitle() != null) {
			eo.setTitle(dto.getTitle());
		}
		if (dto.getMessage() != null && !dto.getMessage().isBlank()) {
			eo.setMessage(dto.getMessage());
		}
		if (dto.getType() != null) {
			eo.setType(dto.getType());
		}
		if (dto.getBgColor() != null) {
			eo.setBgColor(dto.getBgColor());
		}
		if (dto.getTextColor() != null) {
			eo.setTextColor(dto.getTextColor());
		}
		if (dto.getSpeed() != null) {
			eo.setSpeed(dto.getSpeed());
		}
		if (dto.getPriority() != null) {
			eo.setPriority(dto.getPriority());
		}
		if (dto.getLinkUrl() != null) {
			eo.setLinkUrl(dto.getLinkUrl());
		}
		if (dto.getStartDate() != null) {
			eo.setStartDate(dto.getStartDate());
		}
		if (dto.getEndDate() != null) {
			eo.setEndDate(dto.getEndDate());
		}
		if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
			eo.setStatus(dto.getStatus().toUpperCase());
		}
		if (dto.getUpdatedBy() != null) {
			eo.setUpdatedBy(dto.getUpdatedBy());
		}

		eo = flashMessageRepository.save(eo);
		logger.info("Flash message id={} updated successfully", id);
		return toDTO(eo);
	}

	// ─── activate ─────────────────────────────────────────────────────────────

	@Override
	@Transactional
	public ResponseDTO activateFlashMessage(Long id) {
		logger.info("Activating flash message id={}", id);

		FlashMessageEO eo = flashMessageRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Flash message not found with id=" + id));

		eo.setStatus("A");
		flashMessageRepository.save(eo);
		logger.info("Flash message id={} activated", id);

		return ResponseDTO.builder()
			.responseStatus(Constants.SUCCESS_STATUS)
			.responseMessage("Flash message activated successfully")
			.build();
	}

	// ─── deactivate ───────────────────────────────────────────────────────────

	@Override
	@Transactional
	public ResponseDTO deactivateFlashMessage(Long id) {
		logger.info("Deactivating flash message id={}", id);

		FlashMessageEO eo = flashMessageRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Flash message not found with id=" + id));

		eo.setStatus("I");
		flashMessageRepository.save(eo);
		logger.info("Flash message id={} deactivated", id);

		return ResponseDTO.builder()
			.responseStatus(Constants.SUCCESS_STATUS)
			.responseMessage("Flash message deactivated successfully")
			.build();
	}

	// ─── get by id ────────────────────────────────────────────────────────────

	@Override
	public FlashMessageResponseDTO getFlashMessageById(Long id) {
		logger.info("Fetching flash message id={}", id);
		FlashMessageEO eo = flashMessageRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Flash message not found with id=" + id));
		return toDTO(eo);
	}

	// ─── get list by status ───────────────────────────────────────────────────

	@Override
	public FlashMessageListResponseDTO getFlashMessages(String status) {
		logger.info("Fetching flash messages, status filter={}", status);

		List<FlashMessageEO> list;
		if (status != null && !status.isBlank()) {
			list = flashMessageRepository.findByStatusOrderByPriorityAscIdAsc(status.trim().toUpperCase());
		}
		else {
			list = flashMessageRepository.findAllByOrderByPriorityAscIdAsc();
		}

		List<FlashMessageResponseDTO> dtos = list.stream().map(this::toDTO).collect(Collectors.toList());

		return FlashMessageListResponseDTO.builder()
			.responseStatus(Constants.SUCCESS_STATUS)
			.responseMessage("Flash messages fetched successfully")
			.totalCount(dtos.size())
			.flashMessages(dtos)
			.build();
	}

}

