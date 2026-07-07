package com.user.service;

import com.user.dto.*;

public interface ReviewService {

	// ── Reviews ──────────────────────────────────────────────────────────────

	/** Submit a new review for a product */
	ProductReviewDTO createReview(ProductReviewCreateDTO request);

	/** Get all approved reviews for a product (with rating summary) */
	ProductReviewListResponseDTO getReviewsByProduct(Integer productId);

	/** Get all reviews for a product (any status) – admin use */
	ProductReviewListResponseDTO getAllReviewsByProduct(Integer productId);

	/** Get all reviews with a given status – admin use */
	ProductReviewListResponseDTO getReviewsByStatus(String status);

	/** Approve / Reject a review */
	ProductReviewDTO updateReviewStatus(Long reviewId, String status);

	/** Delete a review */
	void deleteReview(Long reviewId);

	// ── Questions & Answers ───────────────────────────────────────────────────

	/** Post a question about a product */
	ProductQuestionDTO createQuestion(ProductQuestionCreateDTO request);

	/** Post an answer to a question */
	ProductAnswerDTO createAnswer(ProductAnswerCreateDTO request);

	/** Get all answered questions for a product (public view) */
	ProductQAListResponseDTO getQAByProduct(Integer productId);

	/** Get all questions for a product (any status) – admin use */
	ProductQAListResponseDTO getAllQAByProduct(Integer productId);

	/** Get all questions with a given status – admin use */
	ProductQAListResponseDTO getQuestionsByStatus(String status);

	/** Update question status (PENDING / ANSWERED / CLOSED) */
	ProductQuestionDTO updateQuestionStatus(Long questionId, String status);

	/** Delete a question (and its answers) */
	void deleteQuestion(Long questionId);

	/** Delete a single answer */
	void deleteAnswer(Long answerId);

}
