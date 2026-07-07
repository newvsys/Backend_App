package com.user.controller;

import com.user.dto.OrderResponseDTO;
import com.user.dto.PaymentStatusUpdateDTO;
import com.user.dto.ResponseDTO;
import com.user.dto.RetryPaymentRequestDTO;
import com.user.service.RazorpayService;
import com.user.service.OrderService;
import com.user.utility.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

	private static final Logger logger = LogManager.getLogger(PaymentController.class);

	@Autowired
	private RazorpayService razorpayService;

	@Autowired
	private OrderService orderService;

	@PostMapping("/verify")
	public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> body) {
		String razorpayOrderId = body.get("razorpay_order_id");
		String razorpayPaymentId = body.get("razorpay_payment_id");
		String razorpaySignature = body.get("razorpay_signature");

		// ── Input validation ────────────────────────────────────────────────────
		if (razorpayOrderId == null || razorpayOrderId.isBlank() || razorpayPaymentId == null
				|| razorpayPaymentId.isBlank() || razorpaySignature == null || razorpaySignature.isBlank()) {
			return ResponseEntity.badRequest()
				.body(Map.of("status", "failed", "message",
						"razorpay_order_id, razorpay_payment_id and razorpay_signature are required"));
		}
		// ────────────────────────────────────────────────────────────────────────

		boolean valid = razorpayService.verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);

		PaymentStatusUpdateDTO paymentStatusUpdateDTO = new PaymentStatusUpdateDTO();
		paymentStatusUpdateDTO.setRazorpayOrderId(razorpayOrderId);
		paymentStatusUpdateDTO.setRazorpayPaymentId(razorpayPaymentId);

		if (valid) {
			paymentStatusUpdateDTO.setPaymentStatus(Constants.ORDER_PAYMENT_STATUS_PAID);
			orderService.updateOrderPaymentStatus(paymentStatusUpdateDTO);
			return ResponseEntity.ok(Map.of("status", "success"));
		}
		else {
			paymentStatusUpdateDTO.setPaymentStatus(Constants.ORDER_PAYMENT_STATUS_FAILED);
			orderService.updateOrderPaymentStatus(paymentStatusUpdateDTO);
			return ResponseEntity.badRequest().body(Map.of("status", "failed"));
		}
	}

	/**
	 * POST /api/payments/retry-payment Re-initiates payment for a pending/failed order.
	 * Creates a fresh Razorpay order and returns the same response shape as /api/orders,
	 * so the frontend can open the payment modal again.
	 *
	 * Request body: { "orderId": "28", "orderNumber": "ORD-260608121943-000029" }
	 */
	@PostMapping("/retry-payment")
	public ResponseEntity<OrderResponseDTO> retryPayment(@RequestBody RetryPaymentRequestDTO request) {
		logger.info("retryPayment endpoint called: orderId={}, orderNumber={}", request.getOrderId(),
				request.getOrderNumber());
		try {
			if ((request.getOrderId() == null || request.getOrderId().isBlank())
					&& (request.getOrderNumber() == null || request.getOrderNumber().isBlank())) {
				OrderResponseDTO err = new OrderResponseDTO();
				err.setStatus(Constants.FAILURE_STATUS);
				err.setMessage("Either orderId or orderNumber is required.");
				return ResponseEntity.badRequest().body(err);
			}
			OrderResponseDTO response = orderService.retryPayment(request);
			if (Constants.STATUS_SUCCESS.equals(response.getStatus())) {
				return ResponseEntity.ok(response);
			}
			else {
				return ResponseEntity.badRequest().body(response);
			}
		}
		catch (Exception e) {
			logger.error("Unexpected error in retryPayment: {}", e.getMessage(), e);
			OrderResponseDTO err = new OrderResponseDTO();
			err.setStatus(Constants.FAILURE_STATUS);
			err.setMessage("An unexpected error occurred. Please try again later.");
			return ResponseEntity.status(500).body(err);
		}
	}

}
