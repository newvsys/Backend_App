package com.user.utility;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Constants {

	// ── Courier Blocklist ──────────────────────────────────────────────────────
	/**
	 * Shiprocket courier_company_ids that are blocked / service not available. Add any
	 * courier IDs here that should never be auto-assigned. Example IDs are placeholders —
	 * replace with real IDs as needed.
	 */
	public static final Set<Integer> BLOCKLISTED_COURIER_COMPANY_IDS = Collections
		.unmodifiableSet(new HashSet<>(Arrays.asList(54
		// e.g. 999, 888 ← replace with real blocked courier IDs
		)));

	/** Maximum number of best-courier candidates to evaluate per shipment. */
	public static final int MAX_BEST_COURIER_COUNT = 10;

	// User Roles
	public static final String ROLE_USER = "user";

	// Order Payment & Status
	public static final String ORDER_PAYMENT_STATUS_PENDING = "PENDING";

	public static final String ORDER_PAYMENT_STATUS_PAID = "PAID";

	public static final String ORDER_PAYMENT_STATUS_FAILED = "FAILED";

	public static final String ORDER_STATUS_CONFIRMED = "Confirmed";

	public static final String ORDER_STATUS_PAYMENT_FAILED = "PAYMENT FAILED";

	public static final String ORDER_STATUS_DELIVERED = "Delivered";

	public static final String ORDER_STATUS_CANCELLED = "CANCELLED";

	public static final String ORDER_STATUS_RETURNED = "Returned";

	public static final String ORDER_STATUS_SHIPPED = "Out for Delivery";

	// Shipment

	public static final String SHIPMENT_STATUS_CREATED = "CREATED";

	public static final String SHIPMENT_STATUS_DELIVERED = "DELIVERED";

	public static final String SHIPMENT_STATUS_IN_TRANSIT = "IN_TRANSIT";

	public static final String SHIPMENT_STATUS_CANCELLED = "CANCELLED";

	public static final String SHIPMENT_STATUS_RETURN_REQUESTED = "RETURN_REQUESTED";

	public static final String SHIPMENT_STATUS_RETURN_PICKUP_INITIATED = "RETURN_PICKUP_INITIATED";

	public static final String SHIPMENT_STATUS_RETURN_PICKUP_INITIATED_REMARK = "Return pickup initiated using original shipment details.";

	public static final String SHIPMENT_STATUS_RECEIVED = "RECEIVED";

	// Shipment Types
	public static final String SHIPMENT_TYPE_FORWARD = "FORWARD";

	public static final String SHIPMENT_TYPE_RETURN_PICKUP = "RETURN_PICKUP";

	public static final String SHIPMENT_ORDER_STATUS_CREATED = "Order Confirmed";

	public static final String SHIPMENT_ORDER_STATUS_CREATED_REMARK = "Order Submitted shipment will be created.";

	public static final String SHIPMENT_ORDER_REFUND_PROCESSED = "Refund Processed";

	public static final String SHIPMENT_ORDER_REFUND_PROCESSED_REMARK = "Refund processed for cancelled/returnded order. ";

	// Add more shipment statuses as needed

	// Locale & General Status
	public static final String LOCALE = "en";

	public static final String FAILURE_STATUS = "FAILURE";

	public static final String SUCCESS_STATUS = "SUCCESS";

	public static final String STATUS_ACTIVE = "A";

	public static final String STATUS_INACTIVE = "I";

	public static final Integer COUNTRY_CODE = 91;

	// Messages For User Service.
	public static final String TECHNICAL_ERROR_LOGIN = "An unexpected error occurred during Login. Please try again later.";

	public static final String MOBILE_NO_VERIFICATION = "Mobile No Verification";

	public static final Integer OTP_EXPIRED_TIME_IN_MINS = 15;

	public static final String NO_IDENTIFIER_PROVIDED = "Phone number is required for Reset password.";

	public static final String NEW_PASSWORD_REQUIRED_RESETPASSWORD = "New password is required for Reset password.";

	public static final String TECHNICAL_ERROR_CUSTOMER_UPDATE = "An unexpected error occurred during Update Customer details. Please try again later.";

	public static final String ACTIVE_USER_ALREADY_EXISTS = "An active user already exists with this phone number. Please log in or reset your password";

	public static final String INACTIVE_USER_ALREADY_EXISTS = "An Inactive user already exists with this phone number.you can activate this user by verifying OTP sent to your registered phone number.";

	public static final String USER_CREATED_MSG = "User created successfully.";

	public static final String TECHNICAL_ERROR_REGISTER = "An unexpected error occurred during registration. Please try again later.";

	public static final String TECHNICAL_ERROR_OTP_GENERATION = "An unexpected error occurred during OTP Sent. Please Contact Support.";

	public static final String TECHNICAL_ERROR_OTP_VERIFICATION = "An unexpected error occurred during OTP Verification. Please Contact Support.";

	public static final String CUSTOMER_TYPE_GUEST = "GUEST";

	public static final String CUSTOMER_TYPE_REGISTERED = "REGISTERED";

	public static final String TECHNICAL_ERROR_RESETPASSWORD = "An unexpected error occurred during Reset Password. Please try again later.";

	public static final String OTP_VERIFICATION_FAILED = "Your OTP got Expired. Please try again.";

	public static final String OTP_VERIFICATION_FAILED_INVALIED_OTP = "Pls Enter valied OTP";

	public static final String OTP_GENERATION_FAILED = "An unexpected error occurred during generate OTP. Please try again later.";

	public static final String OTP_GENERATION_FAILED_MAX_RETRY_REACHED = "Maximum retry count exceeded. Please try again after 15 minutes";

	public static final String OTP_GENERATION_SUCCESSFUL = "OTP sent successfully";

	public static final String OTP_VERIFICATION_SUCCESS = "OTP verified successfully";

	public static final String PURPOSE_FORGOT_PASSWORD = "forgot-password";

	/** Purpose value used when generating / verifying OTP for login. */
	public static final String PURPOSE_LOGIN = "LOGIN";

	public static final String OTP_LOGIN_USER_NOT_FOUND = "No registered account found for this phone number. Please register first.";

	public static final String OTP_LOGIN_SUCCESS = "Login successful via OTP.";

	public static final String OTP_LOGIN_FAILED = "OTP login failed. Please try again.";

	public static final String NO_ACTIVE_USER = "No Active User contact support";

	public static final String MULTIPLE_ACTIVE_USERS = "Multiple active users found with the same email/phone, contact support";

	public static final String PASSWORD_RESET_SUCCESS = "Password reset successfull";

	public static final String LOGIN_SUCCESS = "Login successful";

	public static final String LOGIN_INVALID_CREDENTIAL = "Invalid credentials. Please check your email and password.";

	public static final String INVALID_PASSWORD = "Invalid password. Please try again.";

	public static final String MOBILE_NO_NOT_VERIFIED = "Mobile number not verified. Please verify your mobile number to proceed.";

	// Phone Validation Messages
	public static final String PHONE_REQUIRED = "Phone number is required";

	public static final String PHONE_INVALID = "Invalid phone number. Please enter a valid 10-digit number.";

	public static final String PHONE_VERIFIED_YES = "YES";

	public static final String PHONE_VERIFIED_NO = "NO";

	// OTP
	public static final int MAX_ATTEMPTS = 5;

	public static final int OTP_EXPIRY_MINUTES = 15;

	public static final String OTP_STATUS_ACTIVE = "A";

	public static final String OTP_STATUS_VERIFIED = "V";

	public static final String OTP_STATUS_EXPIRED = "E";

	public static final String OTP_STATUS_BLOCKED = "B";

	public static final Integer OTP_RETRY_CHANNEL = 11;

	// Communication Purposes & Channels
	public static final String COMMUNICATION_PURPOSE_REGISTRATION = "REGISTRATION";

	public static final String COMMUNICATION_PURPOSE_OTP = "OTP";

	public static final String COMMUNICATION_PURPOSE_LOGIN = "LOGIN";

	public static final String COMMUNICATION_PURPOSE_RESETPASSWORD = "RESETPASSWORD";

	public static final String COMMUNICATION_PURPOSE_ORDER_CONFIRMATION = "ORDER_CONFIRMATION";

	public static final String COMMUNICATION_PURPOSE_REFUND = "REFUND";

	public static final String COMMUNICATION_CHANNEL_EMAIL = "EMAIL";

	public static final String COMMUNICATION_CHANNEL_SMS = "SMS";

	public static final String COMMUNICATION_CHANNEL_BOTH = "BOTH";

	public static final String COMMUNICATION_STATUS_SENT = "SENT";

	public static final String KAFKA_COMMUNICATION_GROUP_ID = "communication-group";

	// Kafka Topics
	public static final String CUSTOMER_EVENTS_TOPIC = "customer-events";

	public static final String ORDER_EVENTS_TOPIC = "order-events";

	public static final String REFUND_EVENTS_TOPIC = "refund-events";

	public static final String SHIPROCKET_EVENTS_TOPIC = "shiprocket-events";

	// Unified order event types
	public static final String ORDER_EVENT_TYPE_CREATED = "ORDER_CREATED";

	public static final String ORDER_EVENT_TYPE_CANCELLED = "ORDER_CANCELLED";

	public static final String ORDER_EVENT_TYPE_SHIPPED = "ORDER_SHIPPED";

	public static final String ORDER_EVENT_TYPE_DELIVERED = "ORDER_DELIVERED";

	public static final String ORDER_EVENT_TYPE_REFUND = "ORDER_REFUND";

	public static final String ORDER_EVENT_TYPE_RETURN_REQUEST = "ORDER_RETURN_REQUESTED";

	// Reason Types & Status
	public static final String REASON_TYPE_CANCELLATION = "CANCELLATION";

	public static final String REASON_TYPE_RETURN = "RETURN";

	public static final String REASON_TYPE_EXCHANGE = "EXCHANGE";

	// Order Cancel Request Status
	public static final String ORDER_CANCEL_REQUEST_STATUS_REQUESTED = "REQUESTED";

	public static final String ORDER_CANCEL_REQUEST_STATUS_APPROVED = "APPROVED";

	public static final String ORDER_CANCEL_REQUEST_STATUS_REJECTED = "REJECTED";

	public static final String ORDER_CANCEL_FAILURE = "Order cancellation failed. Please Contact Support.";

	public static final String ORDER_CANCEL_SUCCESSFUL = "Cancel request processed successfully";

	public static final String ORDER_NOT_ELIGIBLE_FOR_CANCEL = "This order is not eligible for cancel please contact support.";

	// Order Item Status
	public static final String ORDER_ITEM_STATUS_CANCELLED = "CANCELLED";

	public static final String ORDER_ITEM_STATUS_CANCELLED_IN_PROGRESS = "CANCELLED_IN_PROGRESS";

	public static final String ORDER_ITEM_STATUS_RETURN_IN_PROGRESS = "RETURN_IN_PROGRESS";

	public static final String ORDER_STATUS_RETURN_REQUESTED = "Return Requested";

	// Return Request Status
	public static final String RETURN_STATUS_REQUESTED = "REQUESTED";

	public static final String RETURN_STATUS_APPROVED = "APPROVED";

	public static final String RETURN_STATUS_REJECTED = "REJECTED";

	public static final String RETURN_STATUS_PICKUP_IN_TRANSIT = "PICKUP_IN_TRANSIT";

	public static final String RETURN_STATUS_RECEIVED = "RECEIVED";

	public static final String RETURN_STATUS_COMPLETED = "COMPLETED";

	public static final String STATUS_SUCCESS = "success";

	public static final String ORDER_CREATED_SUCCESS = "order created successfully";

	public static final String ORDER_STATUS_CREATED = "Submitted";

	public static final String ORDER_CREATED_FAILURE = "order not submitted due to some technical error. Please try again later.";

	public static final String STORE_NAME = "Kuchi Mittai";

	public static final String ORDER_DESCRIPTION = "Order Payment";

	public static final String PAYMENT_CURRENCY = "INR";

	public static final String TECHNICAL_ERROR_CREATE_ORDER = "An unexpected error occurred during Order Creation. Please try again later.";

	public static final String ORDER_PRODUCT_MISSING = "Product is missing in the order. Please add at least one product to proceed.";

	public static final String ADDRESS_TYPE_BOTH = "BOTH";

	public static final String PHONE_NO_MISSING_CUST_USER = "Phone number is required for guest checkout";

	// Payment Methods
	public static final String PAYMENT_METHOD_COD = "COD";

	public static final String PAYMENT_METHOD_UPI = "UPI";

	public static final String PAYMENT_METHOD_CARD = "CARD";

	public static final String PAYMENT_METHOD_NETBANKING = "NETBANKING";

	public static final String PAYMENT_METHOD_WALLET = "WALLET";

	public static final String PAYMENT_METHOD_EMI = "EMI";

	public static final String PAYMENT_METHOD_PAY_LATER = "PAY_LATER";

	public static final String PAYMENT_REFUND_REFERENCE_MISSING = "Refund reference No is required for Process the Refund.";

	public static final String PAYMENT_REFUND_STATUS_INPROGRESS = "IN_PROGRESS";

	public static final String PAYMENT_REFUND_STATUS_SUCCESS = "SUCCESS";

	public static final String PAYMENT_REFUND_STATUS_FAILED = "FAILED";

	public static final String PAYMENT_REFUND_STATUS_APPROVED = "APPROVED";

	// Return Types
	public static final String RETURN_TYPE_RETURN = "RETURN";

	public static final String RETURN_TYPE_EXCHANGE = "EXCHANGE";

	// Warehouse
	/**
	 * Name of the default/primary warehouse used as the pickup location when no specific
	 * warehouse name is provided (e.g. in serviceability checks). Update this value to
	 * match the exact warehouse name stored in the database.
	 */
	public static final String DEFAULT_WAREHOUSE_NAME = "warehouse";

	// MSG91 Email
	public static final String MSG91_EMAIL_DOMAIN = "mail.kuchimittai.com";

	public static final String MSG91_EMAIL_TEMPLATE_ORDER_STATUS_UPDATE = "orderstatusupdatetem";

	public static final String MSG91_EMAIL_TEMPLATE_ORDER_CANCEL = "ordercanceltem";

	// Label / Branding
	/**
	 * Default company logo used on printed labels when no specific logo path is
	 * configured in the LabelConfig record.
	 */
	public static final String DEFAULT_LABEL_LOGO_PATH = "/public/companyLogo/CompanyLogo.png";

}