package com.user.service;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

	@Value("${razorpay.key.secret}")
	private String keySecret;

	public boolean verifySignature(String orderId, String paymentId, String signature) {
		try {
			JSONObject attributes = new JSONObject();
			attributes.put("razorpay_order_id", orderId);
			attributes.put("razorpay_payment_id", paymentId);
			attributes.put("razorpay_signature", signature);

			Utils.verifyPaymentSignature(attributes, keySecret);
			return true;
		}
		catch (RazorpayException e) {
			return false;
		}
	}

}