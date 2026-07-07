# Admin User & Customer APIs — Request / Response Payloads

Base URL: `POST http://localhost:8080/user`

---

## 1. GET `/user/admin/users` — List All Users (Paginated)

### Request

```
GET /user/admin/users?search=john&status=ACTIVE&page=0&size=10
```

| Query Param | Type    | Required | Default | Description                              |
|-------------|---------|----------|---------|------------------------------------------|
| `search`    | String  | No       | —       | Search by name, email, or phone number   |
| `status`    | String  | No       | —       | Filter: `ACTIVE` or `INACTIVE`           |
| `page`      | Integer | No       | `0`     | Zero-based page number                   |
| `size`      | Integer | No       | `20`    | Number of records per page               |

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

### Response — Error (200 with failure status)

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

## 2. GET `/user/admin/users/{userId}` — Get User by ID

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

## 3. GET `/user/admin/customers` — List All Customers (Paginated)

### Request

```
GET /user/admin/customers?search=priya&status=ACTIVE&page=0&size=20
```

| Query Param | Type    | Required | Default | Description                                     |
|-------------|---------|----------|---------|-------------------------------------------------|
| `search`    | String  | No       | —       | Search by customer name, email, or mobile number |
| `status`    | String  | No       | —       | Filter: `ACTIVE` or `INACTIVE`                  |
| `page`      | Integer | No       | `0`     | Zero-based page number                          |
| `size`      | Integer | No       | `20`    | Number of records per page                      |

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

## 4. GET `/user/admin/customers/{customerId}` — Get Customer by ID

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

## 5. PUT `/user/admin/users/{userId}/status` — Update User Status

### Request

```
PUT /user/admin/users/101/status?status=INACTIVE
```

| Path Param | Type   | Required | Description      |
|------------|--------|----------|------------------|
| `userId`   | Long   | Yes      | User's system ID |

| Query Param | Type   | Required | Allowed Values            |
|-------------|--------|----------|---------------------------|
| `status`    | String | Yes      | `ACTIVE` or `INACTIVE`    |

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

## Field Reference

### AdminUserDTO

| Field               | Type            | Description                                  |
|---------------------|-----------------|----------------------------------------------|
| `userId`            | Long            | Internal user ID                             |
| `email`             | String          | User login email                             |
| `phone`             | String          | Registered phone number (10 digits)          |
| `firstName`         | String          | User's first name                            |
| `role`              | String          | `USER` or `ADMIN`                            |
| `status`            | String          | `ACTIVE` or `INACTIVE`                       |
| `phoneVerified`     | String          | `YES` or `NO`                                |
| `phoneVerifiedAt`   | OffsetDateTime  | When phone was verified                      |
| `userCreatedAt`     | OffsetDateTime  | Account creation timestamp                   |
| `userUpdatedAt`     | OffsetDateTime  | Last update timestamp                        |
| `lastLoginAt`       | OffsetDateTime  | Last login timestamp                         |
| `customerId`        | Integer         | Customer record ID                           |
| `customerFirstName` | String          | Customer first name                          |
| `customerLastName`  | String          | Customer last name                           |
| `customerEmail`     | String          | Customer email                               |
| `mobileNumber`      | String          | Customer mobile number                       |
| `customerType`      | String          | `REGISTERED` or `GUEST`                      |
| `customerStatus`    | String          | `ACTIVE` or `INACTIVE`                       |
| `customerCreatedAt` | OffsetDateTime  | Customer record creation timestamp           |
| `customerUpdatedAt` | OffsetDateTime  | Customer record last update timestamp        |
| `addresses`         | Array           | List of saved addresses                      |

### CustomerAddressDTO (inside `addresses`)

| Field           | Type    | Description                             |
|-----------------|---------|-----------------------------------------|
| `addressId`     | Integer | Unique address ID                       |
| `addressType`   | String  | `HOME`, `WORK`, `OTHER`                 |
| `recipientName` | String  | Name of the person at this address      |
| `addressLine1`  | String  | Primary address line                    |
| `addressLine2`  | String  | Secondary address line (optional)       |
| `landMark`      | String  | Nearby landmark (optional)              |
| `city`          | String  | City                                    |
| `state`         | String  | State                                   |
| `country`       | String  | Country (default: `India`)              |
| `postalCode`    | String  | 6-digit PIN code                        |
| `contactNumber` | String  | Contact number for this address         |

### AdminUserListResponseDTO

| Field             | Type    | Description                         |
|-------------------|---------|-------------------------------------|
| `users`           | Array   | List of `AdminUserDTO` objects       |
| `totalCount`      | Long    | Total matching records in DB        |
| `page`            | Integer | Current page (0-based)              |
| `size`            | Integer | Records requested per page          |
| `totalPages`      | Integer | Total number of pages               |
| `responseStatus`  | String  | `SUCCESS` or `FAILURE`              |
| `responseMessage` | String  | Human-readable result message       |

---

## Notes

- All timestamps are in **ISO-8601 offset format** (e.g., `2025-01-15T10:30:00+05:30`)
- `null` fields are **omitted** from the response (`@JsonInclude(NON_NULL)`)
- A user without a linked customer record will still return user-level fields; customer fields will be absent
- `updateUserStatus` mirrors the new status to the linked customer record automatically

