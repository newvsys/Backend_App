package com.user.utility;

/**
 * Utilities for cleaning up user-supplied address text before it is persisted
 * or sent to downstream systems (e.g. Shiprocket), which reject special
 * characters in address fields.
 */
public class AddressUtil {

	// Characters that are NOT allowed in address text. Anything that is not a
	// letter, digit or whitespace and is not one of the address-friendly
	// characters ( - . / ) is treated as a special character and removed.
	// This strips the likes of , @ # $ % ^ & * ! etc. as requested.
	private static final String DISALLOWED_CHARS_REGEX = "[^A-Za-z0-9\\s./-]";

	private AddressUtil() {
		// utility class – no instances
	}

	/**
	 * Removes special characters (e.g. , @ # $ % ^ &amp;) from the given address
	 * text. Letters, digits, whitespace and the address-friendly characters
	 * {@code - . /} are preserved. Collapses any resulting runs of whitespace
	 * into a single space and trims the result.
	 *
	 * @param value the raw address text (may be {@code null})
	 * @return the sanitized text, or {@code null} if the input was {@code null}
	 */
	public static String sanitize(String value) {
		if (value == null) {
			return null;
		}
		String cleaned = value.replaceAll(DISALLOWED_CHARS_REGEX, " ");
		// Collapse multiple spaces introduced by removals and trim the edges.
		cleaned = cleaned.replaceAll("\\s+", " ").trim();
		return cleaned;
	}

}

