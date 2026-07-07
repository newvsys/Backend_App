# UserController — API Documentation

Base URL: `http://localhost:8080`  
Controller prefix: `/user`

---

## Table of Contents

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | POST | `/user/register` | Register a new user |
| 2 | POST | `/user/generate-otp` | Generate / send an OTP |
| 3 | POST | `/user/verify-otp` | Verify an OTP |
| 4 | POST | `/user/login` | Login with email & password |
| 5 | POST | `/user/login-otp/send` | OTP Login — Step 1: send OTP |
| 6 | POST | `/user/login-otp/verify` | OTP Login — Step 2: verify OTP & get auth token |
| 7 | POST | `/user/reset-password` | Reset password using OTP |
| 8 | POST | `/user/update-customer` | Update customer & address details |
| 9 | GET | `/user/get-customer-details` | Get customer details by userId |
| 10 | GET | `/user/admin/users` | Admin: list all users (paginated) |
| 11 | GET | `/user/admin/users/{userId}` | Admin: get full user details by ID |
| 12 | GET | `/user/admin/customers` | Admin: list all customers (paginated) |
| 13 | GET | `/user/admin/customers/{customerId}` | Admin: get full customer details by ID |
| 14 | PUT | `/user/admin/users/{userId}/status` | Admin: activate or deactivate a user |

---

## 1. POST `/user/register` — Register New User

### Request Body (`UserCreateDTO`)

```json
{
  "firstName": "John",
  "phone": "9876543210",
  "email": "john.doe@example.com",
  "password": "SecurePass@123"
}
```

| Field       | Type   | Required | Description                              |
|-------------|--------|----------|------------------------------------------|
| `firstName` | String | No       | User's first name                        |
| `phone`     | String | Yes      | 10-digit mobile number (must be numeric) |
| `email`     | String | No       | User's email address                     |
| `password`  | String | No       | Plain-text password (hashed server-side) |

### Response — Success (200)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "User registered successfully"
}
```

### Response — Validation Failure (200)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Phone number is required"
}
```

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Invalid phone number format"
}
```

### Response — Server Error (500)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An unexpected error occurred during registration. Please try again later."
}
```

---

## 2. POST `/user/generate-otp` — Generate / Send OTP

### Request Body (`OtpRequestDTO`)

```json
{
  "phone": "9876543210",
  "purpose": "REGISTRATION",
  "ipAddress": "192.168.1.1",
  "deviceId": "device-uuid-abc123"
}
```

| Field       | Type   | Required | Description                                                          |
|-------------|--------|----------|----------------------------------------------------------------------|
| `phone`     | String | Yes      | 10-digit mobile number                                               |
| `purpose`   | String | No       | Reason for OTP (e.g., `REGISTRATION`, `LOGIN`, `RESET_PASSWORD`)     |
| `ipAddress` | String | No       | Caller's IP address (for rate-limiting / audit)                      |
| `deviceId`  | String | No       | Device identifier (for rate-limiting / audit)                        |

### Response — Success (200)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "OTP sent successfully"
}
```

### Response — Validation Failure (200)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Phone number is required"
}
```

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Invalid phone number format"
}
```

### Response — Server Error (500)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An unexpected error occurred during OTP generation. Please try again later."
}
```

---

## 3. POST `/user/verify-otp` — Verify OTP

### Request Body (`OtpVerifyRequestDTO`)

```json
{
  "phone": "9876543210",
  "otp": "123456",
  "purpose": "REGISTRATION"
}
```

| Field     | Type   | Required | Description                                                      |
|-----------|--------|----------|------------------------------------------------------------------|
| `phone`   | String | Yes      | 10-digit mobile number                                           |
| `otp`     | String | Yes      | 6-digit OTP code received by the user                           |
| `purpose` | String | No       | Should match the purpose used in `generate-otp` if applicable   |

### Response — Success (200)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "OTP verified successfully"
}
```

### Response — OTP Mismatch / Expired (200)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Invalid or expired OTP"
}
```

### Response — Validation Failure (200)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Phone number is required"
}
```

### Response — Server Error (500)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An unexpected error occurred during OTP verification. Please try again later."
}
```

---

## 4. POST `/user/login` — Login with Email & Password

### Request Body (`LoginRequestDto`)

```json
{
  "email": "john.doe@example.com",
  "password": "SecurePass@123"
}
```

| Field      | Type   | Required | Description              |
|------------|--------|----------|--------------------------|
| `email`    | String | Yes      | Registered email address |
| `password` | String | Yes      | Account password         |

### Response — Success (200)

```json
{
  "id": 101,
  "email": "john.doe@example.com",
  "role": "USER",
  "status": "SUCCESS",
  "message": "Login successful"
}
```

### Response — Invalid Credentials (200)

```json
{
  "id": null,
  "email": null,
  "role": null,
  "status": "FAILURE",
  "message": "Invalid credentials"
}
```

### Response — Server Error (200)

```json
{
  "id": null,
  "email": null,
  "role": null,
  "status": "FAILURE",
  "message": "An unexpected error occurred during login. Please try again later."
}
```

---

## 5. POST `/user/login-otp/send` — OTP Login Step 1: Send OTP

Validates the phone number against active registered users and dispatches an OTP if the user is found.

### Request Body (`OtpRequestDTO`)

```json
{
  "phone": "9876543210"
}
```

| Field   | Type   | Required | Description            |
|---------|--------|----------|------------------------|
| `phone` | String | Yes      | 10-digit mobile number |

### Response — Success (200)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "OTP sent successfully"
}
```

### Response — User Not Found (200)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "No active user found with this phone number"
}
```

### Response — Validation Failure (200)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Phone number is required"
}
```

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Invalid phone number format"
}
```

### Response — Server Error (500)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An unexpected error occurred during OTP generation. Please try again later."
}
```

---

## 6. POST `/user/login-otp/verify` — OTP Login Step 2: Verify OTP & Authenticate

Verifies the OTP and returns the same auth response shape as password-based login.

### Request Body (`OtpVerifyRequestDTO`)

```json
{
  "phone": "9876543210",
  "otp": "123456"
}
```

| Field   | Type   | Required | Description                           |
|---------|--------|----------|---------------------------------------|
| `phone` | String | Yes      | 10-digit mobile number                |
| `otp`   | String | Yes      | 6-digit OTP code received by the user |

### Response — Success (200)

```json
{
  "id": 101,
  "email": "john.doe@example.com",
  "role": "USER",
  "status": "SUCCESS",
  "message": "Login successful"
}
```

### Response — OTP Mismatch / Expired (200)

```json
{
  "id": null,
  "email": null,
  "role": null,
  "status": "FAILURE",
  "message": "Invalid or expired OTP"
}
```

### Response — Validation Failure (200)

```json
{
  "id": null,
  "email": null,
  "role": null,
  "status": "FAILURE",
  "message": "Phone number is required"
}
```

```json
{
  "id": null,
  "email": null,
  "role": null,
  "status": "FAILURE",
  "message": "OTP is required"
}
```

### Response — Server Error (500)

```json
{
  "id": null,
  "email": null,
  "role": null,
  "status": "FAILURE",
  "message": "An unexpected error occurred during login. Please try again later."
}
```

---

## 7. POST `/user/reset-password` — Reset Password

Resets a user's password after OTP verification. Requires either `email` or `phoneNo`.

### Request Body (`ResetPasswordRequestDTO`)

```json
{
  "phoneNo": "9876543210",
  "email": "john.doe@example.com",
  "otp": "123456",
  "newPassword": "NewSecurePass@456"
}
```

| Field         | Type   | Required              | Description                                      |
|---------------|--------|-----------------------|--------------------------------------------------|
| `phoneNo`     | String | Yes (if no email)     | 10-digit mobile number                           |
| `email`       | String | Yes (if no phoneNo)   | Registered email address                         |
| `otp`         | String | Yes                   | OTP code received on the phone or email          |
| `newPassword` | String | Yes                   | New password to set (hashed server-side)         |

### Response — Success (200)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Password reset successfully"
}
```

### Response — Validation Failure (200)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Email or Mobile No is required"
}
```

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Invalid phone number format"
}
```

### Response — Server Error (200)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An unexpected error occurred during password reset. Please try again later."
}
```

---

## 8. POST `/user/update-customer` — Update Customer & Address Details

Creates or updates the customer profile and address(es) linked to a user account.

### Request Body (`CustomerDTO`)

```json
{
  "customerId": 55,
  "userId": "101",
  "firstName": "John",
  "email": "john.doe@example.com",
  "mobileNumber": "9876543210",
  "customerType": "REGISTERED",
  "status": "ACTIVE",
  "addresses": [
    {
      "addressId": 12,
      "addressType": "HOME",
      "recipientName": "John Doe",
      "addressLine1": "42, Green Park",
      "addressLine2": "Near City Mall",
      "landMark": "Opposite Apollo Hospital",
      "city": "Chennai",
      "state": "Tamil Nadu",
      "country": "India",
      "postalCode": "600001",
      "contactNumber": "9876543210"
    },
    {
      "addressId": null,
      "addressType": "WORK",
      "recipientName": "John Doe",
      "addressLine1": "Level 5, Prestige Tower",
      "addressLine2": "MG Road",
      "landMark": null,
      "city": "Bengaluru",
      "state": "Karnataka",
      "country": "India",
      "postalCode": "560001",
      "contactNumber": "9876543210"
    }
  ]
}
```

| Field          | Type                      | Required | Description                                              |
|----------------|---------------------------|----------|----------------------------------------------------------|
| `customerId`   | Integer                   | No       | Existing customer ID; omit or `null` to create new       |
| `userId`       | String                    | Yes      | Linked user account ID                                   |
| `firstName`    | String                    | No       | Customer first name                                      |
| `email`        | String                    | No       | Customer email                                           |
| `mobileNumber` | String                    | No       | 10-digit mobile number                                   |
| `customerType` | String                    | No       | `REGISTERED` or `GUEST`                                  |
| `status`       | String                    | No       | `ACTIVE` or `INACTIVE`                                   |
| `addresses`    | Array\<CustomerAddressDTO\> | No       | List of addresses; `addressId: null` creates a new entry |

#### CustomerAddressDTO fields

| Field           | Type    | Required | Description                                    |
|-----------------|---------|----------|------------------------------------------------|
| `addressId`     | Integer | No       | Existing address ID; `null` to create new      |
| `addressType`   | String  | No       | `HOME`, `WORK`, or `OTHER`                     |
| `recipientName` | String  | No       | Name of recipient at this address              |
| `addressLine1`  | String  | No       | Primary address line                           |
| `addressLine2`  | String  | No       | Secondary address line (optional)              |
| `landMark`      | String  | No       | Nearby landmark (optional)                     |
| `city`          | String  | No       | City                                           |
| `state`         | String  | No       | State                                          |
| `country`       | String  | No       | Country (default: `India`)                     |
| `postalCode`    | String  | No       | 6-digit PIN code                               |
| `contactNumber` | String  | No       | Contact number for deliveries at this address  |

### Response — Success (200)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Customer updated successfully"
}
```

### Response — Server Error (200)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An unexpected error occurred during customer update. Please try again later."
}
```

---

## 9. GET `/user/get-customer-details` — Get Customer Details

### Request

```
GET /user/get-customer-details?userId=101
```

| Query Param | Type | Required | Description                       |
|-------------|------|----------|-----------------------------------|
| `userId`    | Long | Yes      | User's system ID (must be > 0)    |

### Response — Success (200)

```json
{
  "customerId": 55,
  "userId": "101",
  "firstName": "John",
  "email": "john.doe@example.com",
  "mobileNumber": "9876543210",
  "customerType": "REGISTERED",
  "status": "ACTIVE",
  "addresses": [
    {
      "addressId": 12,
      "addressType": "HOME",
      "recipientName": "John Doe",
      "addressLine1": "42, Green Park",
      "addressLine2": "Near City Mall",
      "landMark": "Opposite Apollo Hospital",
      "city": "Chennai",
      "state": "Tamil Nadu",
      "country": "India",
      "postalCode": "600001",
      "contactNumber": "9876543210"
    }
  ]
}
```

### Response — Invalid userId (400)

```json
{
  "status": "failed",
  "message": "userId must be a positive number"
}
```

### Response — Not Found (404)

```json
{
  "status": "failed",
  "message": "No active customer found for userId: 101"
}
```

---

## 10. GET `/user/admin/users` — Admin: List All Users (Paginated)

### Request

```
GET /user/admin/users?search=john&status=ACTIVE&page=0&size=10
```

| Query Param | Type    | Required | Default | Description                            |
|-------------|---------|----------|---------|----------------------------------------|
| `search`    | String  | No       | —       | Search by name, email, or phone number |
| `status`    | String  | No       | —       | Filter: `ACTIVE` or `INACTIVE`         |
| `page`      | Integer | No       | `0`     | Zero-based page number                 |
| `size`      | Integer | No       | `20`    | Records per page                       |

### Response — Success (200)

```json
{
  "users": [
    {
      "userId": 101,
      "email": "john.doe@example.com",
      "phone": "9876543210",
      "firstName": "John",
      "role": "USER",
      "status": "ACTIVE",
      "phoneVerified": "YES",
      "phoneVerifiedAt": "2025-01-15T10:30:00+05:30",
      "userCreatedAt": "2025-01-15T10:28:00+05:30",
      "userUpdatedAt": "2025-03-20T08:00:00+05:30",
      "lastLoginAt": "2026-05-10T18:45:00+05:30",
      "customerId": 55,
      "customerFirstName": "John",
      "customerLastName": "Doe",
      "customerEmail": "john.doe@example.com",
      "mobileNumber": "9876543210",
      "customerType": "REGISTERED",
      "customerStatus": "ACTIVE",
      "customerCreatedAt": "2025-01-15T10:30:00+05:30",
      "customerUpdatedAt": "2025-03-20T08:00:00+05:30",
      "addresses": [
        {
          "addressId": 12,
          "addressType": "HOME",
          "recipientName": "John Doe",
          "addressLine1": "42, Green Park",
          "addressLine2": "Near City Mall",
          "landMark": "Opposite Apollo Hospital",
          "city": "Chennai",
          "state": "Tamil Nadu",
          "country": "India",
          "postalCode": "600001",
          "contactNumber": "9876543210"
        }
      ]
    }
  ],
  "totalCount": 150,
  "page": 0,
  "size": 10,
  "totalPages": 15,
  "responseStatus": "SUCCESS",
  "responseMessage": "Users fetched successfully. Total: 150"
}
```

### Response — Error (200)

```json
{
  "users": null,
  "totalCount": 0,
  "page": 0,
  "size": 10,
  "totalPages": 0,
  "responseStatus": "FAILURE",
  "responseMessage": "An error occurred while fetching users: <error details>"
}
```

---

## 11. GET `/user/admin/users/{userId}` — Admin: Get User by ID

### Request

```
GET /user/admin/users/101
```

| Path Param | Type | Required | Description      |
|------------|------|----------|------------------|
| `userId`   | Long | Yes      | User's system ID |

### Response — Success (200)

```json
{
  "userId": 101,
  "email": "john.doe@example.com",
  "phone": "9876543210",
  "firstName": "John",
  "role": "USER",
  "status": "ACTIVE",
  "phoneVerified": "YES",
  "phoneVerifiedAt": "2025-01-15T10:30:00+05:30",
  "userCreatedAt": "2025-01-15T10:28:00+05:30",
  "userUpdatedAt": "2025-03-20T08:00:00+05:30",
  "lastLoginAt": "2026-05-10T18:45:00+05:30",
  "customerId": 55,
  "customerFirstName": "John",
  "customerLastName": "Doe",
  "customerEmail": "john.doe@example.com",
  "mobileNumber": "9876543210",
  "customerType": "REGISTERED",
  "customerStatus": "ACTIVE",
  "customerCreatedAt": "2025-01-15T10:30:00+05:30",
  "customerUpdatedAt": "2025-03-20T08:00:00+05:30",
  "addresses": [
    {
      "addressId": 12,
      "addressType": "HOME",
      "recipientName": "John Doe",
      "addressLine1": "42, Green Park",
      "addressLine2": "Near City Mall",
      "landMark": "Opposite Apollo Hospital",
      "city": "Chennai",
      "state": "Tamil Nadu",
      "country": "India",
      "postalCode": "600001",
      "contactNumber": "9876543210"
    },
    {
      "addressId": 13,
      "addressType": "WORK",
      "recipientName": "John Doe",
      "addressLine1": "Level 5, Prestige Tower",
      "addressLine2": "MG Road",
      "landMark": null,
      "city": "Bengaluru",
      "state": "Karnataka",
      "country": "India",
      "postalCode": "560001",
      "contactNumber": "9876543210"
    }
  ]
}
```

### Response — Not Found (200 with empty object)

```json
{}
```

---

## 12. GET `/user/admin/customers` — Admin: List All Customers (Paginated)

### Request

```
GET /user/admin/customers?search=priya&status=ACTIVE&page=0&size=20
```

| Query Param | Type    | Required | Default | Description                                      |
|-------------|---------|----------|---------|--------------------------------------------------|
| `search`    | String  | No       | —       | Search by customer name, email, or mobile number |
| `status`    | String  | No       | —       | Filter: `ACTIVE` or `INACTIVE`                   |
| `page`      | Integer | No       | `0`     | Zero-based page number                           |
| `size`      | Integer | No       | `20`    | Records per page                                 |

### Response — Success (200)

```json
{
  "users": [
    {
      "userId": 102,
      "email": "priya.kumar@example.com",
      "phone": "8765432109",
      "firstName": "Priya",
      "role": "USER",
      "status": "ACTIVE",
      "phoneVerified": "YES",
      "phoneVerifiedAt": "2025-02-10T09:00:00+05:30",
      "userCreatedAt": "2025-02-10T08:55:00+05:30",
      "userUpdatedAt": "2025-04-01T12:00:00+05:30",
      "lastLoginAt": "2026-05-09T20:30:00+05:30",
      "customerId": 56,
      "customerFirstName": "Priya",
      "customerLastName": "Kumar",
      "customerEmail": "priya.kumar@example.com",
      "mobileNumber": "8765432109",
      "customerType": "REGISTERED",
      "customerStatus": "ACTIVE",
      "customerCreatedAt": "2025-02-10T09:00:00+05:30",
      "customerUpdatedAt": "2025-04-01T12:00:00+05:30",
      "addresses": [
        {
          "addressId": 20,
          "addressType": "HOME",
          "recipientName": "Priya Kumar",
          "addressLine1": "7, Lotus Street",
          "addressLine2": null,
          "landMark": "Near Metro Station",
          "city": "Mumbai",
          "state": "Maharashtra",
          "country": "India",
          "postalCode": "400001",
          "contactNumber": "8765432109"
        }
      ]
    }
  ],
  "totalCount": 320,
  "page": 0,
  "size": 20,
  "totalPages": 16,
  "responseStatus": "SUCCESS",
  "responseMessage": "Customers fetched successfully. Total: 320"
}
```

---

## 13. GET `/user/admin/customers/{customerId}` — Admin: Get Customer by ID

### Request

```
GET /user/admin/customers/56
```

| Path Param   | Type    | Required | Description          |
|--------------|---------|----------|----------------------|
| `customerId` | Integer | Yes      | Customer's system ID |

### Response — Success (200)

```json
{
  "userId": 102,
  "email": "priya.kumar@example.com",
  "phone": "8765432109",
  "firstName": "Priya",
  "role": "USER",
  "status": "ACTIVE",
  "phoneVerified": "YES",
  "phoneVerifiedAt": "2025-02-10T09:00:00+05:30",
  "userCreatedAt": "2025-02-10T08:55:00+05:30",
  "userUpdatedAt": "2025-04-01T12:00:00+05:30",
  "lastLoginAt": "2026-05-09T20:30:00+05:30",
  "customerId": 56,
  "customerFirstName": "Priya",
  "customerLastName": "Kumar",
  "customerEmail": "priya.kumar@example.com",
  "mobileNumber": "8765432109",
  "customerType": "REGISTERED",
  "customerStatus": "ACTIVE",
  "customerCreatedAt": "2025-02-10T09:00:00+05:30",
  "customerUpdatedAt": "2025-04-01T12:00:00+05:30",
  "addresses": [
    {
      "addressId": 20,
      "addressType": "HOME",
      "recipientName": "Priya Kumar",
      "addressLine1": "7, Lotus Street",
      "addressLine2": null,
      "landMark": "Near Metro Station",
      "city": "Mumbai",
      "state": "Maharashtra",
      "country": "India",
      "postalCode": "400001",
      "contactNumber": "8765432109"
    }
  ]
}
```

### Response — Not Found (200 with empty object)

```json
{}
```

---

## 14. PUT `/user/admin/users/{userId}/status` — Admin: Update User Status

Activates or deactivates a user account. The status change is automatically mirrored to the linked customer record.

### Request

```
PUT /user/admin/users/101/status?status=INACTIVE
```

| Path Param | Type | Required | Description      |
|------------|------|----------|------------------|
| `userId`   | Long | Yes      | User's system ID |

| Query Param | Type   | Required | Allowed Values         |
|-------------|--------|----------|------------------------|
| `status`    | String | Yes      | `ACTIVE` or `INACTIVE` |

No request body required.

### Response — Success (200)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "User status updated to 'INACTIVE' for userId: 101"
}
```

### Response — Invalid Status (200)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Status must be 'ACTIVE' or 'INACTIVE'"
}
```

### Response — User Not Found (200)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "User not found for userId: 999"
}
```

---

## DTO Field Reference

### UserCreateDTO

| Field       | Type   | Description                              |
|-------------|--------|------------------------------------------|
| `firstName` | String | User's first name                        |
| `phone`     | String | 10-digit mobile number (required)        |
| `email`     | String | Email address                            |
| `password`  | String | Plain-text password (hashed server-side) |

### OtpRequestDTO

| Field       | Type   | Description                                                      |
|-------------|--------|------------------------------------------------------------------|
| `phone`     | String | 10-digit mobile number (required)                                |
| `purpose`   | String | OTP purpose: `REGISTRATION`, `LOGIN`, `RESET_PASSWORD`, etc.     |
| `ipAddress` | String | Caller's IP (used for rate-limiting / audit)                     |
| `deviceId`  | String | Device identifier (used for rate-limiting / audit)               |

### OtpVerifyRequestDTO

| Field     | Type   | Description                                |
|-----------|--------|--------------------------------------------|
| `phone`   | String | 10-digit mobile number (required)          |
| `otp`     | String | 6-digit OTP code (required)                |
| `purpose` | String | Should match the purpose used during send  |

### LoginRequestDto

| Field      | Type   | Description                    |
|------------|--------|--------------------------------|
| `email`    | String | Registered email (required)    |
| `password` | String | Account password (required)    |

### AuthResponseDto

| Field    | Type    | Description                              |
|----------|---------|------------------------------------------|
| `id`     | Integer | User's system ID                         |
| `email`  | String  | User's email address                     |
| `role`   | String  | `USER` or `ADMIN`                        |
| `status` | String  | `SUCCESS` or `FAILURE`                   |
| `message`| String  | Human-readable result message            |

### ResetPasswordRequestDTO

| Field         | Type   | Description                                     |
|---------------|--------|-------------------------------------------------|
| `phoneNo`     | String | 10-digit mobile number (required if no email)   |
| `email`       | String | Email address (required if no phoneNo)          |
| `otp`         | String | OTP code received by the user                   |
| `newPassword` | String | New password to set (hashed server-side)        |

### CustomerDTO

| Field          | Type                      | Description                              |
|----------------|---------------------------|------------------------------------------|
| `customerId`   | Integer                   | Existing customer ID (`null` = create)   |
| `userId`       | String                    | Linked user account ID                   |
| `firstName`    | String                    | Customer first name                      |
| `email`        | String                    | Customer email                           |
| `mobileNumber` | String                    | 10-digit mobile number                   |
| `customerType` | String                    | `REGISTERED` or `GUEST`                  |
| `status`       | String                    | `ACTIVE` or `INACTIVE`                   |
| `addresses`    | Array\<CustomerAddressDTO\> | List of customer addresses               |

### CustomerAddressDTO

| Field           | Type    | Description                                   |
|-----------------|---------|-----------------------------------------------|
| `addressId`     | Integer | Address ID (`null` = new address)             |
| `addressType`   | String  | `HOME`, `WORK`, or `OTHER`                    |
| `recipientName` | String  | Name of recipient at this address             |
| `addressLine1`  | String  | Primary address line                          |
| `addressLine2`  | String  | Secondary address line (optional)             |
| `landMark`      | String  | Nearby landmark (optional)                    |
| `city`          | String  | City                                          |
| `state`         | String  | State                                         |
| `country`       | String  | Country (default: `India`)                    |
| `postalCode`    | String  | 6-digit PIN code                              |
| `contactNumber` | String  | Contact number for deliveries at this address |

### ResponseDTO

| Field             | Type   | Description                     |
|-------------------|--------|---------------------------------|
| `responseStatus`  | String | `SUCCESS` or `FAILURE`          |
| `responseMessage` | String | Human-readable result message   |

### AdminUserDTO

| Field               | Type           | Description                           |
|---------------------|----------------|---------------------------------------|
| `userId`            | Long           | Internal user ID                      |
| `email`             | String         | User login email                      |
| `phone`             | String         | Registered phone number (10 digits)   |
| `firstName`         | String         | User's first name                     |
| `role`              | String         | `USER` or `ADMIN`                     |
| `status`            | String         | `ACTIVE` or `INACTIVE`                |
| `phoneVerified`     | String         | `YES` or `NO`                         |
| `phoneVerifiedAt`   | OffsetDateTime | When phone was verified               |
| `userCreatedAt`     | OffsetDateTime | Account creation timestamp            |
| `userUpdatedAt`     | OffsetDateTime | Last update timestamp                 |
| `lastLoginAt`       | OffsetDateTime | Last login timestamp                  |
| `customerId`        | Integer        | Linked customer record ID             |
| `customerFirstName` | String         | Customer first name                   |
| `customerLastName`  | String         | Customer last name                    |
| `customerEmail`     | String         | Customer email                        |
| `mobileNumber`      | String         | Customer mobile number                |
| `customerType`      | String         | `REGISTERED` or `GUEST`               |
| `customerStatus`    | String         | `ACTIVE` or `INACTIVE`                |
| `customerCreatedAt` | OffsetDateTime | Customer record creation timestamp    |
| `customerUpdatedAt` | OffsetDateTime | Customer record last update timestamp |
| `addresses`         | Array          | List of `CustomerAddressDTO` objects  |

### AdminUserListResponseDTO

| Field             | Type    | Description                          |
|-------------------|---------|--------------------------------------|
| `users`           | Array   | List of `AdminUserDTO` objects        |
| `totalCount`      | Long    | Total matching records in DB         |
| `page`            | Integer | Current page (0-based)               |
| `size`            | Integer | Records requested per page           |
| `totalPages`      | Integer | Total number of pages                |
| `responseStatus`  | String  | `SUCCESS` or `FAILURE`               |
| `responseMessage` | String  | Human-readable result message        |

---

## Notes

- All timestamps are in **ISO-8601 offset format** (e.g., `2025-01-15T10:30:00+05:30`)
- `null` fields are **omitted** from responses where `@JsonInclude(NON_NULL)` is applied
- Phone numbers must be **exactly 10 digits, numeric only**
- A user without a linked customer record will still return user-level fields in admin endpoints; customer fields will be absent
- `PUT /admin/users/{userId}/status` automatically mirrors the new status to the linked customer record
- OTP login is a **two-step flow**: call `/login-otp/send` first, then `/login-otp/verify`
- Password reset is a **two-step flow**: call `/generate-otp` first (with `purpose: RESET_PASSWORD`), then `/reset-password`

