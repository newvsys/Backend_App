package com.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customer_addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddressEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "address_id")
	private Integer addressId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id")
	private CustomerEO customer;

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
		if (country == null || country.trim().isEmpty()) {
			country = "India";
		}
	}

	/** Ensures country is never blanked-out on UPDATE. */
	@PreUpdate
	protected void onUpdate() {
		if (country == null || country.trim().isEmpty()) {
			country = "India";
		}
	}

}
