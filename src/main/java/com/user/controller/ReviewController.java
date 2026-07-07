package com.user.controller;

import com.user.dto.*;
import com.user.service.ReviewService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Product Reviews and Q&A (Questions & Answers).
 *
 * Base path: /api/reviews
 *
 * ── Review APIs ───────────────────────────────────────────────────────────── POST
 * /reviews – Submit a new review GET /reviews/product/{productId} – Get approved reviews
 * for a product (public) GET /reviews/product/{productId}/all – Get all reviews
 * regardless of status (admin) GET /reviews/status/{status} – Get reviews by status
 * (admin) PATCH /reviews/{reviewId}/status – Approve / Reject a review (admin) DELETE
 * /reviews/{reviewId} – Delete a review (admin)
 *
 * ── Q&A APIs ──────────────────────────────────────────────────────────────── POST
 * /reviews/questions – Post a product question POST
 * /reviews/questions/{questionId}/answers – Post an answer to a question GET
 * /reviews/questions/product/{productId} – Get answered questions for product (public)
 * GET /reviews/questions/product/{productId}/all – Get all questions regardless of status
 * (admin) GET /reviews/questions/status/{status} – Get questions by status (admin) PATCH
 * /reviews/questions/{questionId}/status – Update question status (admin) DELETE
 * /reviews/questions/{questionId} – Delete a question (admin) DELETE
 * /reviews/answers/{answerId} – Delete an answer (admin)
 */
@RestController
@RequestMapping("/reviews")
public class ReviewController {

	private static final Logger logger = LogManager.getLogger(ReviewController.class);

	@Autowired
	private ReviewService reviewService;

	// ── Review endpoints ─────────────────────────────────────────────────────

	@PostMapping
	public ResponseEntity<ProductReviewDTO> createReview(@RequestBody ProductReviewCreateDTO request) {
		try {
			ProductReviewDTO review = reviewService.createReview(request);
			logger.info("Review submitted for productId={}", request.getProductId());
			return ResponseEntity.ok(review);
		}
		catch (IllegalArgumentException e) {
			logger.warn("Invalid review request: {}", e.getMessage());
			return ResponseEntity.badRequest().build();
		}
		catch (Exception e) {
			logger.error("Error creating review: {}", e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	@GetMapping("/product/{productId}")
	public ResponseEntity<ProductReviewListResponseDTO> getApprovedReviews(@PathVariable Integer productId) {
		try {
			ProductReviewListResponseDTO response = reviewService.getReviewsByProduct(productId);
			logger.info("Fetched approved reviews for productId={}", productId);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error fetching reviews for productId={}: {}", productId, e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	@GetMapping("/product/{productId}/all")
	public ResponseEntity<ProductReviewListResponseDTO> getAllReviews(@PathVariable Integer productId) {
		try {
			ProductReviewListResponseDTO response = reviewService.getAllReviewsByProduct(productId);
			logger.info("Fetched all reviews for productId={}", productId);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error fetching all reviews for productId={}: {}", productId, e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	@GetMapping("/status/{status}")
	public ResponseEntity<ProductReviewListResponseDTO> getReviewsByStatus(@PathVariable String status) {
		try {
			ProductReviewListResponseDTO response = reviewService.getReviewsByStatus(status);
			logger.info("Fetched reviews by status={}", status);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error fetching reviews by status={}: {}", status, e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	@PatchMapping("/{reviewId}/status")
	public ResponseEntity<ProductReviewDTO> updateReviewStatus(@PathVariable Long reviewId,
			@RequestParam String status) {
		try {
			ProductReviewDTO review = reviewService.updateReviewStatus(reviewId, status);
			logger.info("Review id={} status updated to {}", reviewId, status);
			return ResponseEntity.ok(review);
		}
		catch (Exception e) {
			logger.error("Error updating review status id={}: {}", reviewId, e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	@DeleteMapping("/{reviewId}")
	public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
		try {
			reviewService.deleteReview(reviewId);
			logger.info("Review id={} deleted", reviewId);
			return ResponseEntity.noContent().build();
		}
		catch (Exception e) {
			logger.error("Error deleting review id={}: {}", reviewId, e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	// ── Q&A endpoints ────────────────────────────────────────────────────────

	@PostMapping("/questions")
	public ResponseEntity<ProductQuestionDTO> createQuestion(@RequestBody ProductQuestionCreateDTO request) {
		try {
			ProductQuestionDTO question = reviewService.createQuestion(request);
			logger.info("Question submitted for productId={}", request.getProductId());
			return ResponseEntity.ok(question);
		}
		catch (IllegalArgumentException e) {
			logger.warn("Invalid question request: {}", e.getMessage());
			return ResponseEntity.badRequest().build();
		}
		catch (Exception e) {
			logger.error("Error creating question: {}", e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	@PostMapping("/questions/{questionId}/answers")
	public ResponseEntity<ProductAnswerDTO> createAnswer(@PathVariable Long questionId,
			@RequestBody ProductAnswerCreateDTO request) {
		try {
			request.setQuestionId(questionId);
			ProductAnswerDTO answer = reviewService.createAnswer(request);
			logger.info("Answer submitted for questionId={}", questionId);
			return ResponseEntity.ok(answer);
		}
		catch (IllegalArgumentException e) {
			logger.warn("Invalid answer request: {}", e.getMessage());
			return ResponseEntity.badRequest().build();
		}
		catch (Exception e) {
			logger.error("Error creating answer: {}", e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	@GetMapping("/questions/product/{productId}")
	public ResponseEntity<ProductQAListResponseDTO> getAnsweredQuestions(@PathVariable Integer productId) {
		try {
			ProductQAListResponseDTO response = reviewService.getQAByProduct(productId);
			logger.info("Fetched answered questions for productId={}", productId);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error fetching Q&A for productId={}: {}", productId, e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	@GetMapping("/questions/product/{productId}/all")
	public ResponseEntity<ProductQAListResponseDTO> getAllQuestions(@PathVariable Integer productId) {
		try {
			ProductQAListResponseDTO response = reviewService.getAllQAByProduct(productId);
			logger.info("Fetched all questions for productId={}", productId);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error fetching all Q&A for productId={}: {}", productId, e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	@GetMapping("/questions/status/{status}")
	public ResponseEntity<ProductQAListResponseDTO> getQuestionsByStatus(@PathVariable String status) {
		try {
			ProductQAListResponseDTO response = reviewService.getQuestionsByStatus(status);
			logger.info("Fetched questions by status={}", status);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			logger.error("Error fetching questions by status={}: {}", status, e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	@PatchMapping("/questions/{questionId}/status")
	public ResponseEntity<ProductQuestionDTO> updateQuestionStatus(@PathVariable Long questionId,
			@RequestParam String status) {
		try {
			ProductQuestionDTO question = reviewService.updateQuestionStatus(questionId, status);
			logger.info("Question id={} status updated to {}", questionId, status);
			return ResponseEntity.ok(question);
		}
		catch (Exception e) {
			logger.error("Error updating question status id={}: {}", questionId, e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	@DeleteMapping("/questions/{questionId}")
	public ResponseEntity<Void> deleteQuestion(@PathVariable Long questionId) {
		try {
			reviewService.deleteQuestion(questionId);
			logger.info("Question id={} deleted", questionId);
			return ResponseEntity.noContent().build();
		}
		catch (Exception e) {
			logger.error("Error deleting question id={}: {}", questionId, e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

	@DeleteMapping("/answers/{answerId}")
	public ResponseEntity<Void> deleteAnswer(@PathVariable Long answerId) {
		try {
			reviewService.deleteAnswer(answerId);
			logger.info("Answer id={} deleted", answerId);
			return ResponseEntity.noContent().build();
		}
		catch (Exception e) {
			logger.error("Error deleting answer id={}: {}", answerId, e.getMessage(), e);
			return ResponseEntity.status(500).build();
		}
	}

}
