package com.user.utility;

public class PhoneUtil {

	/**
	 * Checks if the phone number is empty or null.
	 */
	public static boolean isEmpty(String phone) {
		return phone == null || phone.trim().isEmpty();
	}

	/**
	 * Checks if the phone number is valid (10 digits, numeric).
	 */
	public static boolean isValid(String phone) {
		if (isEmpty(phone))
			return false;
		return phone.trim().matches("^[0-9]{10}$");
	}

}
