package com.user.service;

import com.user.dto.*;
import com.user.model.*;
import com.user.repository.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

	private static final Logger logger = LogManager.getLogger(ReviewServiceImpl.class);

	@Autowired
	private ProductReviewRepository reviewRepository;

	@Autowired
	private ReviewImageRepository reviewImageRepository;

	@Autowired
	private ProductQuestionRepository questionRepository;

	@Autowired
	private ProductAnswerRepository answerRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductVariantRepository variantRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CustomerRepository customerRepository;

	// ── Reviews ──────────────────────────────────────────────────────────────

	@Override
	@Transactional
	public ProductReviewDTO createReview(ProductReviewCreateDTO request) {
		try {
			// Step 1: fetch the product directly by productId
			ProductEO product = productRepository.findById(request.getProductId().longValue())
				.orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));

			// Step 2: fetch UserEO by userId, then resolve CustomerEO from UserEO
			CustomerEO customer = null;
			if (request.getCustomerId() != null) {
				UserEO user = userRepository.findById(request.getCustomerId().longValue()).orElse(null);
				if (user != null) {
					customer = customerRepository.findByUser(user).orElse(null);
				}
			}

			if (request.getRating() == null || request.getRating() > 5) {
				throw new IllegalArgumentException("Rating must be between 1 and 5");
			}

			ProductReviewEO review = ProductReviewEO.builder()
				.product(product)
				.customer(customer)
				.rating(request.getRating())
				.title(request.getTitle())
				.reviewText(request.getReviewText())
				.status("PENDING")
				.build();

			review = reviewRepository.save(review);

			// Save images if provided
			if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
				ProductReviewEO finalReview = review;
				List<ReviewImageEO> images = request.getImageUrls()
					.stream()
					.filter(url -> url != null && !url.isBlank())
					.map(url -> ReviewImageEO.builder().review(finalReview).imageUrl(url).build())
					.collect(Collectors.toList());
				reviewImageRepository.saveAll(images);
				review.setImages(images);
			}

			logger.info("Review created with id={} for productId={}", review.getId(), product.getId());
			return mapReviewToDTO(review);
		}
		catch (Exception e) {
			logger.error("Error creating review for productId={}: {}", request.getProductId(), e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ProductReviewListResponseDTO getReviewsByProduct(Integer productId) {
		try {
			ProductEO product = productRepository.findById(productId.longValue())
				.orElseThrow(() -> new RuntimeException("Product not found: " + productId));

			List<ProductReviewEO> reviews = reviewRepository.findByProductAndStatusOrderByCreatedAtDesc(product,
					"APPROVED");
			return buildReviewListResponse(product.getId(), product, reviews);
		}
		catch (Exception e) {
			logger.error("Error fetching reviews for productId={}: {}", productId, e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ProductReviewListResponseDTO getAllReviewsByProduct(Integer productId) {
		try {
			ProductVariantEO variant = variantRepository.findById(productId.longValue())
				.orElseThrow(() -> new RuntimeException("Product variant not found: " + productId));
			ProductEO product = variant.getProduct();

			List<ProductReviewEO> reviews = reviewRepository.findByProductOrderByCreatedAtDesc(product);
			return buildReviewListResponse(product.getId(), product, reviews);
		}
		catch (Exception e) {
			logger.error("Error fetching all reviews for variantId={}: {}", productId, e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ProductReviewListResponseDTO getReviewsByStatus(String status) {
		try {
			List<ProductReviewEO> reviews = reviewRepository.findByStatusOrderByCreatedAtDesc(status);
			List<ProductReviewDTO> dtos = reviews.stream().map(this::mapReviewToDTO).collect(Collectors.toList());
			return ProductReviewListResponseDTO.builder().totalReviews((long) reviews.size()).reviews(dtos).build();
		}
		catch (Exception e) {
			logger.error("Error fetching reviews by status={}: {}", status, e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional
	public ProductReviewDTO updateReviewStatus(Long reviewId, String status) {
		try {
			ProductReviewEO review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));
			review.setStatus(status);
			review = reviewRepository.save(review);
			logger.info("Review id={} status updated to {}", reviewId, status);
			return mapReviewToDTO(review);
		}
		catch (Exception e) {
			logger.error("Error updating review status id={}: {}", reviewId, e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional
	public void deleteReview(Long reviewId) {
		try {
			reviewRepository.deleteById(reviewId);
			logger.info("Review id={} deleted", reviewId);
		}
		catch (Exception e) {
			logger.error("Error deleting review id={}: {}", reviewId, e.getMessage(), e);
			throw e;
		}
	}

	// ── Questions & Answers ───────────────────────────────────────────────────

	@Override
	@Transactional
	public ProductQuestionDTO createQuestion(ProductQuestionCreateDTO request) {
		try {
			// Step 1: fetch product variant using productId as variant id
			ProductVariantEO variant = variantRepository.findById(request.getProductId().longValue())
				.orElseThrow(() -> new RuntimeException("Product variant not found: " + request.getProductId()));

			// Step 2: get the parent product from the variant
			ProductEO product = variant.getProduct();

			// Step 3: fetch UserEO by userId, then resolve CustomerEO from UserEO
			CustomerEO customer = null;
			if (request.getCustomerId() != null) {
				UserEO user = userRepository.findById(request.getCustomerId().longValue()).orElse(null);
				if (user != null) {
					customer = customerRepository.findByUser(user).orElse(null);
				}
			}

			if (request.getQuestionText() == null || request.getQuestionText().isBlank()) {
				throw new IllegalArgumentException("Question text is required");
			}

			ProductQuestionEO question = ProductQuestionEO.builder()
				.product(product)
				.customer(customer)
				.questionText(request.getQuestionText())
				.status("PENDING")
				.build();

			question = questionRepository.save(question);
			logger.info("Question created id={} for variantId={} productId={}", question.getId(),
					request.getProductId(), product.getId());
			return mapQuestionToDTO(question);
		}
		catch (Exception e) {
			logger.error("Error creating question for variantId={}: {}", request.getProductId(), e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional
	public ProductAnswerDTO createAnswer(ProductAnswerCreateDTO request) {
		try {
			ProductQuestionEO question = questionRepository.findById(request.getQuestionId())
				.orElseThrow(() -> new RuntimeException("Question not found: " + request.getQuestionId()));

			if (request.getAnswerText() == null || request.getAnswerText().isBlank()) {
				throw new IllegalArgumentException("Answer text is required");
			}

			ProductAnswerEO answer = ProductAnswerEO.builder()
				.question(question)
				.answeredBy(request.getAnsweredBy())
				.answerText(request.getAnswerText())
				.isAdminAnswer(Boolean.TRUE.equals(request.getIsAdminAnswer()))
				.build();

			answer = answerRepository.save(answer);

			// Update question status to ANSWERED if not already
			if (!"ANSWERED".equals(question.getStatus())) {
				question.setStatus("ANSWERED");
				questionRepository.save(question);
			}

			logger.info("Answer created id={} for questionId={}", answer.getId(), request.getQuestionId());
			return mapAnswerToDTO(answer);
		}
		catch (Exception e) {
			logger.error("Error creating answer for questionId={}: {}", request.getQuestionId(), e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ProductQAListResponseDTO getQAByProduct(Integer productId) {
		try {
			ProductVariantEO variant = variantRepository.findById(productId.longValue())
				.orElseThrow(() -> new RuntimeException("Product variant not found: " + productId));
			ProductEO product = variant.getProduct();

			List<ProductQuestionEO> questions = questionRepository.findByProductAndStatusOrderByCreatedAtDesc(product,
					"ANSWERED");
			return buildQAListResponse(product.getId(), questions);
		}
		catch (Exception e) {
			logger.error("Error fetching Q&A for variantId={}: {}", productId, e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ProductQAListResponseDTO getAllQAByProduct(Integer productId) {
		try {
			ProductVariantEO variant = variantRepository.findById(productId.longValue())
				.orElseThrow(() -> new RuntimeException("Product variant not found: " + productId));
			ProductEO product = variant.getProduct();

			List<ProductQuestionEO> questions = questionRepository.findByProductOrderByCreatedAtDesc(product);
			return buildQAListResponse(product.getId(), questions);
		}
		catch (Exception e) {
			logger.error("Error fetching all Q&A for variantId={}: {}", productId, e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ProductQAListResponseDTO getQuestionsByStatus(String status) {
		try {
			List<ProductQuestionEO> questions = questionRepository.findByStatusOrderByCreatedAtDesc(status);
			List<ProductQuestionDTO> dtos = questions.stream().map(this::mapQuestionToDTO).collect(Collectors.toList());
			return ProductQAListResponseDTO.builder().totalQuestions((long) questions.size()).questions(dtos).build();
		}
		catch (Exception e) {
			logger.error("Error fetching questions by status={}: {}", status, e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional
	public ProductQuestionDTO updateQuestionStatus(Long questionId, String status) {
		try {
			ProductQuestionEO question = questionRepository.findById(questionId)
				.orElseThrow(() -> new RuntimeException("Question not found: " + questionId));
			question.setStatus(status);
			question = questionRepository.save(question);
			logger.info("Question id={} status updated to {}", questionId, status);
			return mapQuestionToDTO(question);
		}
		catch (Exception e) {
			logger.error("Error updating question status id={}: {}", questionId, e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional
	public void deleteQuestion(Long questionId) {
		try {
			questionRepository.deleteById(questionId);
			logger.info("Question id={} deleted", questionId);
		}
		catch (Exception e) {
			logger.error("Error deleting question id={}: {}", questionId, e.getMessage(), e);
			throw e;
		}
	}

	@Override
	@Transactional
	public void deleteAnswer(Long answerId) {
		try {
			answerRepository.deleteById(answerId);
			logger.info("Answer id={} deleted", answerId);
		}
		catch (Exception e) {
			logger.error("Error deleting answer id={}: {}", answerId, e.getMessage(), e);
			throw e;
		}
	}

	// ── Private helpers ───────────────────────────────────────────────────────

	private ProductReviewDTO mapReviewToDTO(ProductReviewEO review) {
		List<String> imageUrls = Collections.emptyList();
		if (review.getImages() != null) {
			imageUrls = review.getImages().stream().map(ReviewImageEO::getImageUrl).collect(Collectors.toList());
		}

		String customerName = null;
		Integer customerId = null;
		if (review.getCustomer() != null) {
			customerId = review.getCustomer().getCustomerId();
			String fn = review.getCustomer().getFirstName();
			String ln = review.getCustomer().getLastName();
			customerName = ((fn != null ? fn : "") + " " + (ln != null ? ln : "")).trim();
		}

		return ProductReviewDTO.builder()
			.id(review.getId())
			.productId(review.getProduct() != null ? review.getProduct().getId() : null)
			.productName(review.getProduct() != null ? review.getProduct().getName() : null)
			.productVariantId(review.getProductVariant() != null ? review.getProductVariant().getId() : null)
			.customerId(customerId)
			.customerName(customerName)
			.rating(review.getRating())
			.title(review.getTitle())
			.reviewText(review.getReviewText())
			.status(review.getStatus())
			.imageUrls(imageUrls)
			.createdAt(review.getCreatedAt())
			.updatedAt(review.getUpdatedAt())
			.build();
	}

	private ProductReviewListResponseDTO buildReviewListResponse(Integer productId, ProductEO product,
			List<ProductReviewEO> reviews) {
		Double avgRating = reviewRepository.findAverageRatingByProduct(product);
		Long approvedCount = reviewRepository.countApprovedByProduct(product);

		// Rating distribution (only from the provided review list)
		Map<Integer, Long> distribution = reviews.stream()
			.collect(Collectors.groupingBy(ProductReviewEO::getRating, Collectors.counting()));
		// Ensure all keys 1-5 exist
		for (int i = 1; i <= 5; i++) {
			distribution.putIfAbsent(i, 0L);
		}

		List<ProductReviewDTO> dtos = reviews.stream().map(this::mapReviewToDTO).collect(Collectors.toList());

		return ProductReviewListResponseDTO.builder()
			.productId(productId)
			.averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
			.totalReviews(approvedCount != null ? approvedCount : 0L)
			.ratingDistribution(distribution)
			.reviews(dtos)
			.build();
	}

	private ProductAnswerDTO mapAnswerToDTO(ProductAnswerEO answer) {
		return ProductAnswerDTO.builder()
			.id(answer.getId())
			.questionId(answer.getQuestion() != null ? answer.getQuestion().getId() : null)
			.answeredBy(answer.getAnsweredBy())
			.answerText(answer.getAnswerText())
			.isAdminAnswer(answer.getIsAdminAnswer())
			.createdAt(answer.getCreatedAt())
			.updatedAt(answer.getUpdatedAt())
			.build();
	}

	private ProductQuestionDTO mapQuestionToDTO(ProductQuestionEO question) {
		List<ProductAnswerDTO> answerDTOs = Collections.emptyList();
		if (question.getAnswers() != null) {
			answerDTOs = question.getAnswers().stream().map(this::mapAnswerToDTO).collect(Collectors.toList());
		}

		String customerName = null;
		Integer customerId = null;
		if (question.getCustomer() != null) {
			customerId = question.getCustomer().getCustomerId();
			String fn = question.getCustomer().getFirstName();
			String ln = question.getCustomer().getLastName();
			customerName = ((fn != null ? fn : "") + " " + (ln != null ? ln : "")).trim();
		}

		return ProductQuestionDTO.builder()
			.id(question.getId())
			.productId(question.getProduct() != null ? question.getProduct().getId() : null)
			.customerId(customerId)
			.customerName(customerName)
			.questionText(question.getQuestionText())
			.status(question.getStatus())
			.answers(answerDTOs)
			.createdAt(question.getCreatedAt())
			.updatedAt(question.getUpdatedAt())
			.build();
	}

	private ProductQAListResponseDTO buildQAListResponse(Integer productId, List<ProductQuestionEO> questions) {
		List<ProductQuestionDTO> dtos = questions.stream().map(this::mapQuestionToDTO).collect(Collectors.toList());
		return ProductQAListResponseDTO.builder()
			.productId(productId)
			.totalQuestions((long) questions.size())
			.questions(dtos)
			.build();
	}

}
