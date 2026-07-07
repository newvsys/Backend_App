package com.user.service;

import com.user.dto.FlashMessageCreateDTO;
import com.user.dto.FlashMessageListResponseDTO;
import com.user.dto.FlashMessageResponseDTO;
import com.user.dto.ResponseDTO;

public interface FlashMessageService {

	/**
	 * Create a new flash / marquee message.
	 */
	FlashMessageResponseDTO createFlashMessage(FlashMessageCreateDTO dto);

	/**
	 * Update an existing flash / marquee message (only non-null fields are updated).
	 */
	FlashMessageResponseDTO updateFlashMessage(Long id, FlashMessageCreateDTO dto);

	/**
	 * Activate a flash message (set status = 'A').
	 */
	ResponseDTO activateFlashMessage(Long id);

	/**
	 * Deactivate a flash message (set status = 'I').
	 */
	ResponseDTO deactivateFlashMessage(Long id);

	/**
	 * Get a single flash message by its id.
	 */
	FlashMessageResponseDTO getFlashMessageById(Long id);

	/**
	 * Get all flash messages, optionally filtered by status ('A' / 'I'). Returns all
	 * messages when status is null or blank, ordered by priority asc.
	 */
	FlashMessageListResponseDTO getFlashMessages(String status);

}

