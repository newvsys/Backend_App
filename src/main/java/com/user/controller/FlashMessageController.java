package com.user.controller;

import com.user.dto.FlashMessageCreateDTO;
import com.user.dto.FlashMessageListResponseDTO;
import com.user.dto.FlashMessageResponseDTO;
import com.user.dto.ResponseDTO;
import com.user.service.FlashMessageService;
import com.user.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for managing flash / marquee messages displayed on the website.
 *
 * Base path: /api/flash-messages
 *
 * ┌────────────────────────────────────────────────────────────────────────────┐
 * │ POST   /api/flash-messages              – create a new flash message        │
 * │ PUT    /api/flash-messages/{id}         – update a flash message            │
 * │ PATCH  /api/flash-messages/{id}/activate   – activate  (status → 'A')      │
 * │ PATCH  /api/flash-messages/{id}/deactivate – deactivate (status → 'I')     │
 * │ GET    /api/flash-messages/{id}         – get a single message by id        │
 * │ GET    /api/flash-messages?status=A     – get all messages (opt. by status) │
 * └────────────────────────────────────────────────────────────────────────────┘
 *
 * Status values: A = Active (shown on website), I = Inactive (hidden)
 */
@RestController
@RequestMapping("/api/flash-messages")
public class FlashMessageController {

	private static final Logger logger = LoggerFactory.getLogger(FlashMessageController.class);

	@Autowired
	private FlashMessageService flashMessageService;

	// ─── Create ───────────────────────────────────────────────────────────────

	/**
	 * POST /api/flash-messages
	 *
	 * Creates a new flash message to be shown in the website marquee.
	 *
	 * Request body example: { "title": "Summer Sale", "message": "Get 50% off on all
	 * items this weekend!", "type": "OFFER", "bgColor": "#FF5733", "textColor": "#FFFFFF",
	 * "speed": "normal", "priority": 1, "linkUrl": "https://example.com/sale",
	 * "startDate": "2026-07-01T00:00:00", "endDate": "2026-07-07T23:59:59", "status":
	 * "A", "updatedBy": "admin" }
	 */
	@PostMapping
	public ResponseEntity<?> createFlashMessage(@RequestBody FlashMessageCreateDTO dto) {
		logger.info("POST /api/flash-messages - createFlashMessage: title={}", dto != null ? dto.getTitle() : null);

		if (dto == null) {
			return ResponseEntity.badRequest().body(buildError("Request body must not be null"));
		}
		if (dto.getMessage() == null || dto.getMessage().isBlank()) {
			return ResponseEntity.badRequest().body(buildError("message is required"));
		}

		try {
			FlashMessageResponseDTO response = flashMessageService.createFlashMessage(dto);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error creating flash message: {}", e.getMessage(), e);
			return ResponseEntity.status(500).body(buildError("An error occurred while creating the flash message"));
		}
	}

	// ─── Update ───────────────────────────────────────────────────────────────

	/**
	 * PUT /api/flash-messages/{id}
	 *
	 * Updates an existing flash message. Only non-null fields in the request body are
	 * applied; omit a field to leave it unchanged.
	 */
	@PutMapping("/{id}")
	public ResponseEntity<?> updateFlashMessage(@PathVariable("id") Long id,
			@RequestBody FlashMessageCreateDTO dto) {
		logger.info("PUT /api/flash-messages/{} - updateFlashMessage", id);

		if (id == null) {
			return ResponseEntity.badRequest().body(buildError("id is required"));
		}
		if (dto == null) {
			return ResponseEntity.badRequest().body(buildError("Request body must not be null"));
		}

		try {
			FlashMessageResponseDTO response = flashMessageService.updateFlashMessage(id, dto);
			return ResponseEntity.ok(response);
		}
		catch (RuntimeException e) {
			logger.error("Flash message not found id={}: {}", id, e.getMessage());
			return ResponseEntity.status(404).body(buildError(e.getMessage()));
		}
		catch (Exception e) {
			logger.error("Error updating flash message id={}: {}", id, e.getMessage(), e);
			return ResponseEntity.status(500).body(buildError("An error occurred while updating the flash message"));
		}
	}

	// ─── Activate ─────────────────────────────────────────────────────────────

	/**
	 * PATCH /api/flash-messages/{id}/activate
	 *
	 * Sets the flash message status to 'A' (Active), making it visible on the website.
	 */
	@PatchMapping("/{id}/activate")
	public ResponseEntity<?> activateFlashMessage(@PathVariable("id") Long id) {
		logger.info("PATCH /api/flash-messages/{}/activate", id);

		if (id == null) {
			return ResponseEntity.badRequest().body(buildError("id is required"));
		}

		try {
			ResponseDTO response = flashMessageService.activateFlashMessage(id);
			return ResponseEntity.ok(response);
		}
		catch (RuntimeException e) {
			logger.error("Flash message not found id={}: {}", id, e.getMessage());
			return ResponseEntity.status(404).body(buildError(e.getMessage()));
		}
		catch (Exception e) {
			logger.error("Error activating flash message id={}: {}", id, e.getMessage(), e);
			return ResponseEntity.status(500).body(buildError("An error occurred while activating the flash message"));
		}
	}

	// ─── Deactivate ───────────────────────────────────────────────────────────

	/**
	 * PATCH /api/flash-messages/{id}/deactivate
	 *
	 * Sets the flash message status to 'I' (Inactive), hiding it from the website.
	 */
	@PatchMapping("/{id}/deactivate")
	public ResponseEntity<?> deactivateFlashMessage(@PathVariable("id") Long id) {
		logger.info("PATCH /api/flash-messages/{}/deactivate", id);

		if (id == null) {
			return ResponseEntity.badRequest().body(buildError("id is required"));
		}

		try {
			ResponseDTO response = flashMessageService.deactivateFlashMessage(id);
			return ResponseEntity.ok(response);
		}
		catch (RuntimeException e) {
			logger.error("Flash message not found id={}: {}", id, e.getMessage());
			return ResponseEntity.status(404).body(buildError(e.getMessage()));
		}
		catch (Exception e) {
			logger.error("Error deactivating flash message id={}: {}", id, e.getMessage(), e);
			return ResponseEntity.status(500)
				.body(buildError("An error occurred while deactivating the flash message"));
		}
	}

	// ─── Get by id ────────────────────────────────────────────────────────────

	/**
	 * GET /api/flash-messages/{id}
	 *
	 * Retrieves a single flash message by its id.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<?> getFlashMessageById(@PathVariable("id") Long id) {
		logger.info("GET /api/flash-messages/{} - getFlashMessageById", id);

		if (id == null) {
			return ResponseEntity.badRequest().body(buildError("id is required"));
		}

		try {
			FlashMessageResponseDTO response = flashMessageService.getFlashMessageById(id);
			return ResponseEntity.ok(response);
		}
		catch (RuntimeException e) {
			logger.error("Flash message not found id={}: {}", id, e.getMessage());
			return ResponseEntity.status(404).body(buildError(e.getMessage()));
		}
		catch (Exception e) {
			logger.error("Error fetching flash message id={}: {}", id, e.getMessage(), e);
			return ResponseEntity.status(500).body(buildError("An error occurred while fetching the flash message"));
		}
	}

	// ─── Get list by status ───────────────────────────────────────────────────

	/**
	 * GET /api/flash-messages
	 *
	 * Returns all flash messages, ordered by priority (ascending). Pass the optional
	 * {@code status} query parameter to filter:
	 * <ul>
	 * <li>{@code ?status=A} – only active messages (recommended for the public
	 * website)</li>
	 * <li>{@code ?status=I} – only inactive messages</li>
	 * <li>(omit) – all messages regardless of status (admin dashboard)</li>
	 * </ul>
	 */
	@GetMapping
	public ResponseEntity<FlashMessageListResponseDTO> getFlashMessages(
			@RequestParam(value = "status", required = false) String status) {
		logger.info("GET /api/flash-messages - getFlashMessages, status={}", status);

		try {
			FlashMessageListResponseDTO response = flashMessageService.getFlashMessages(status);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error fetching flash messages: {}", e.getMessage(), e);
			FlashMessageListResponseDTO errorResp = FlashMessageListResponseDTO.builder()
				.responseStatus(Constants.FAILURE_STATUS)
				.responseMessage("An error occurred while fetching flash messages")
				.totalCount(0)
				.build();
			return ResponseEntity.status(500).body(errorResp);
		}
	}

	// ─── helper ───────────────────────────────────────────────────────────────

	private ResponseDTO buildError(String message) {
		return ResponseDTO.builder().responseStatus(Constants.FAILURE_STATUS).responseMessage(message).build();
	}

}

