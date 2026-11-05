package com.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.user.utility.AddressUtil;

@Entity
@Table(name = "order_addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAddressEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "address_id")
	private Integer addressId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private OrderEO order;

	@Column(name = "address_type")
	private String addressType; // BILLING / SHIPPING

	@Column(name = "recipient_name")
	private String recipientName;

	@Column(name = "address_line1")
	private String addressLine1;

	@Column(name = "land_mark")
	private String landMark;

	@Column(name = "address_line2")
	private String addressLine2;

	@Column(name = "city")
	private String city;

	@Column(name = "state")
	private String state;

	@Column(name = "country")
	private String country;

	@Column(name = "postal_code")
	private String postalCode;

	@Column(name = "contact_number")
	private String contactNumber;

	/** Ensures country defaults to "India" if not supplied on INSERT. */
	@PrePersist
	protected void onCreate() {
		sanitizeAddressFields();
		if (country == null || country.trim().isEmpty()) {
			country = "India";
		}
	}

	/** Ensures country is never blanked-out on UPDATE. */
	@PreUpdate
	protected void onUpdate() {
		sanitizeAddressFields();
		if (country == null || country.trim().isEmpty()) {
			country = "India";
		}
	}

	/**
	 * Strips special characters (e.g. , @ # $ % ^ &amp;) from the address text
	 * fields before persisting, keeping only letters, digits, whitespace and the
	 * address-friendly characters {@code - . /}.
	 */
	private void sanitizeAddressFields() {
		recipientName = AddressUtil.sanitize(recipientName);
		addressLine1 = AddressUtil.sanitize(addressLine1);
		addressLine2 = AddressUtil.sanitize(addressLine2);
		landMark = AddressUtil.sanitize(landMark);
		city = AddressUtil.sanitize(city);
		state = AddressUtil.sanitize(state);
		country = AddressUtil.sanitize(country);
	}

}
