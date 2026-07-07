# Product Reviews & Q&A API Documentation

**Base URL:** `http://localhost:8080/api`  
**Controller base path:** `/reviews`  
**Full prefix:** `/api/reviews`

---

## Table of Contents

### Reviews
1. [Submit a Review](#1-submit-a-review)
2. [Get Approved Reviews for a Product (Public)](#2-get-approved-reviews-for-a-product)
3. [Get All Reviews for a Product (Admin)](#3-get-all-reviews-for-a-product-admin)
4. [Get Reviews by Status (Admin)](#4-get-reviews-by-status-admin)
5. [Update Review Status (Admin)](#5-update-review-status-admin)
6. [Delete a Review (Admin)](#6-delete-a-review-admin)

### Questions & Answers
7. [Post a Question](#7-post-a-question)
8. [Post an Answer](#8-post-an-answer)
9. [Get Answered Q&A for a Product (Public)](#9-get-answered-qa-for-a-product)
10. [Get All Q&A for a Product (Admin)](#10-get-all-qa-for-a-product-admin)
11. [Get Questions by Status (Admin)](#11-get-questions-by-status-admin)
12. [Update Question Status (Admin)](#12-update-question-status-admin)
13. [Delete a Question (Admin)](#13-delete-a-question-admin)
14. [Delete an Answer (Admin)](#14-delete-an-answer-admin)

---

## Status Values

| Entity   | Status Values                          |
|----------|----------------------------------------|
| Review   | `PENDING` · `APPROVED` · `REJECTED`    |
| Question | `PENDING` · `ANSWERED` · `CLOSED`      |

---

# Review APIs

---

## 1. Submit a Review

**POST** `/api/reviews`

Allows a customer to submit a rating and review for a product. Review is created with status `PENDING` and must be approved by an admin before it becomes publicly visible.

### Request

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "productId": 101,
  "customerId": 55,
  "rating": 4,
  "title": "Great quality product!",
  "reviewText": "The product arrived on time and the quality is excellent. Very happy with the purchase. Would definitely recommend to others.",
  "imageUrls": [
    "https://storage.example.com/review-images/img1.jpg",
    "https://storage.example.com/review-images/img2.jpg"
  ]
}
```

| Field        | Type    | Required | Description                              |
|--------------|---------|----------|------------------------------------------|
| `productId`  | Integer | Yes      | ID of the product being reviewed         |
| `customerId` | Integer | No       | ID of the customer (null for guests)     |
| `rating`     | Integer | Yes      | Rating from **1** (worst) to **5** (best)|
| `title`      | String  | No       | Short headline for the review            |
| `reviewText` | String  | No       | Full review body text                    |
| `imageUrls`  | Array   | No       | List of pre-uploaded image URLs          |

### Response — `200 OK`

```json
{
  "id": 312,
  "productId": 101,
  "customerId": 55,
  "customerName": "Arun Kumar",
  "rating": 4,
  "title": "Great quality product!",
  "reviewText": "The product arrived on time and the quality is excellent. Very happy with the purchase. Would definitely recommend to others.",
  "status": "PENDING",
  "imageUrls": [
    "https://storage.example.com/review-images/img1.jpg",
    "https://storage.example.com/review-images/img2.jpg"
  ],
  "createdAt": "2026-05-13T10:30:00+05:30",
  "updatedAt": "2026-05-13T10:30:00+05:30"
}
```

### Error Responses

| Status | Scenario                              |
|--------|---------------------------------------|
| `400`  | `rating` is null, < 1, or > 5        |
| `400`  | `productId` is null                   |
| `500`  | Product not found or server error     |

---

## 2. Get Approved Reviews for a Product

**GET** `/api/reviews/product/{productId}`

Returns all **approved** reviews for a given product along with rating summary and distribution. This is the public-facing endpoint for the product detail page.

### Path Parameters

| Parameter   | Type    | Description      |
|-------------|---------|------------------|
| `productId` | Integer | ID of the product|

### Example Request

```
GET /api/reviews/product/101
```

### Response — `200 OK`

```json
{
  "productId": 101,
  "averageRating": 4.2,
  "totalReviews": 38,
  "ratingDistribution": {
    "1": 2,
    "2": 3,
    "3": 5,
    "4": 12,
    "5": 16
  },
  "reviews": [
    {
      "id": 312,
      "productId": 101,
      "customerId": 55,
      "customerName": "Arun Kumar",
      "rating": 4,
      "title": "Great quality product!",
      "reviewText": "The product arrived on time and the quality is excellent.",
      "status": "APPROVED",
      "imageUrls": [
        "https://storage.example.com/review-images/img1.jpg"
      ],
      "createdAt": "2026-05-13T10:30:00+05:30",
      "updatedAt": "2026-05-13T11:00:00+05:30"
    },
    {
      "id": 298,
      "productId": 101,
      "customerId": 42,
      "customerName": "Priya Sharma",
      "rating": 5,
      "title": "Absolutely love it",
      "reviewText": "Best purchase I made this month. Highly recommended!",
      "status": "APPROVED",
      "imageUrls": [],
      "createdAt": "2026-05-10T14:15:00+05:30",
      "updatedAt": "2026-05-10T14:15:00+05:30"
    }
  ]
}
```

| Field                | Type    | Description                                              |
|----------------------|---------|----------------------------------------------------------|
| `productId`          | Integer | The product ID                                           |
| `averageRating`      | Double  | Average of all approved ratings (rounded to 1 decimal)  |
| `totalReviews`       | Long    | Total count of approved reviews                         |
| `ratingDistribution` | Object  | Count per star level (keys 1–5 always present)          |
| `reviews`            | Array   | List of approved review objects                         |

---

## 3. Get All Reviews for a Product (Admin)

**GET** `/api/reviews/product/{productId}/all`

Returns **all reviews** (PENDING, APPROVED, REJECTED) for a product. Intended for admin moderation.

### Example Request

```
GET /api/reviews/product/101/all
```

### Response — `200 OK`

```json
{
  "productId": 101,
  "averageRating": 4.2,
  "totalReviews": 38,
  "ratingDistribution": {
    "1": 2,
    "2": 3,
    "3": 5,
    "4": 13,
    "5": 17
  },
  "reviews": [
    {
      "id": 315,
      "productId": 101,
      "customerId": 60,
      "customerName": "Ramesh V",
      "rating": 2,
      "title": "Not as expected",
      "reviewText": "The product looks different from the photos.",
      "status": "PENDING",
      "imageUrls": [],
      "createdAt": "2026-05-13T09:00:00+05:30",
      "updatedAt": "2026-05-13T09:00:00+05:30"
    },
    {
      "id": 312,
      "productId": 101,
      "customerId": 55,
      "customerName": "Arun Kumar",
      "rating": 4,
      "title": "Great quality product!",
      "reviewText": "The product arrived on time and the quality is excellent.",
      "status": "APPROVED",
      "imageUrls": [],
      "createdAt": "2026-05-13T10:30:00+05:30",
      "updatedAt": "2026-05-13T11:00:00+05:30"
    }
  ]
}
```

---

## 4. Get Reviews by Status (Admin)

**GET** `/api/reviews/status/{status}`

Returns all reviews across all products filtered by a specific status. Useful for the admin moderation queue.

### Path Parameters

| Parameter | Type   | Values                           |
|-----------|--------|----------------------------------|
| `status`  | String | `PENDING` · `APPROVED` · `REJECTED` |

### Example Request

```
GET /api/reviews/status/PENDING
```

### Response — `200 OK`

```json
{
  "productId": null,
  "averageRating": null,
  "totalReviews": 5,
  "ratingDistribution": null,
  "reviews": [
    {
      "id": 315,
      "productId": 101,
      "customerId": 60,
      "customerName": "Ramesh V",
      "rating": 2,
      "title": "Not as expected",
      "reviewText": "The product looks different from the photos.",
      "status": "PENDING",
      "imageUrls": [],
      "createdAt": "2026-05-13T09:00:00+05:30",
      "updatedAt": "2026-05-13T09:00:00+05:30"
    },
    {
      "id": 316,
      "productId": 205,
      "customerId": 77,
      "customerName": "Suresh M",
      "rating": 5,
      "title": "Superb!",
      "reviewText": "Works perfectly as described.",
      "status": "PENDING",
      "imageUrls": [],
      "createdAt": "2026-05-13T08:45:00+05:30",
      "updatedAt": "2026-05-13T08:45:00+05:30"
    }
  ]
}
```

---

## 5. Update Review Status (Admin)

**PATCH** `/api/reviews/{reviewId}/status?status={status}`

Approve or reject a review. Updates the review status and saves the change.

### Path Parameters

| Parameter  | Type | Description            |
|------------|------|------------------------|
| `reviewId` | Long | ID of the review       |

### Query Parameters

| Parameter | Type   | Values                              |
|-----------|--------|-------------------------------------|
| `status`  | String | `APPROVED` · `REJECTED` · `PENDING` |

### Example Request

```
PATCH /api/reviews/315/status?status=APPROVED
```

### Response — `200 OK`

```json
{
  "id": 315,
  "productId": 101,
  "customerId": 60,
  "customerName": "Ramesh V",
  "rating": 2,
  "title": "Not as expected",
  "reviewText": "The product looks different from the photos.",
  "status": "APPROVED",
  "imageUrls": [],
  "createdAt": "2026-05-13T09:00:00+05:30",
  "updatedAt": "2026-05-13T12:00:00+05:30"
}
```

### Error Responses

| Status | Scenario             |
|--------|----------------------|
| `500`  | Review ID not found  |

---

## 6. Delete a Review (Admin)

**DELETE** `/api/reviews/{reviewId}`

Permanently deletes a review and all its associated images.

### Path Parameters

| Parameter  | Type | Description       |
|------------|------|-------------------|
| `reviewId` | Long | ID of the review  |

### Example Request

```
DELETE /api/reviews/315
```

### Response — `204 No Content`

_(empty body)_

---

# Q&A APIs

---

## 7. Post a Question

**POST** `/api/reviews/questions`

Allows a customer to post a question about a product. Question is created with status `PENDING`.

### Request

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "productId": 101,
  "customerId": 55,
  "questionText": "Does this product come with a warranty? If yes, how many years?"
}
```

| Field          | Type    | Required | Description                            |
|----------------|---------|----------|----------------------------------------|
| `productId`    | Integer | Yes      | ID of the product the question is about|
| `customerId`   | Integer | No       | ID of the asking customer              |
| `questionText` | String  | Yes      | The question text                      |

### Response — `200 OK`

```json
{
  "id": 88,
  "productId": 101,
  "customerId": 55,
  "customerName": "Arun Kumar",
  "questionText": "Does this product come with a warranty? If yes, how many years?",
  "status": "PENDING",
  "answers": [],
  "createdAt": "2026-05-13T11:00:00+05:30",
  "updatedAt": "2026-05-13T11:00:00+05:30"
}
```

### Error Responses

| Status | Scenario                    |
|--------|-----------------------------|
| `400`  | `questionText` is blank     |
| `500`  | Product not found           |

---

## 8. Post an Answer

**POST** `/api/reviews/questions/{questionId}/answers`

Post an answer to an existing question. The question status is automatically changed to `ANSWERED` when the first answer is posted.

### Path Parameters

| Parameter    | Type | Description          |
|--------------|------|----------------------|
| `questionId` | Long | ID of the question   |

### Request

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "answeredBy": "Kuchimittai Support Team",
  "answerText": "Yes, this product comes with a 2-year manufacturer warranty. You can register the warranty on our website within 30 days of purchase.",
  "isAdminAnswer": true
}
```

| Field           | Type    | Required | Description                                             |
|-----------------|---------|----------|---------------------------------------------------------|
| `answeredBy`    | String  | No       | Name of the person/team answering                       |
| `answerText`    | String  | Yes      | The answer text                                         |
| `isAdminAnswer` | Boolean | No       | `true` = admin/seller answer, `false` = community answer (default: `false`) |

> **Note:** `questionId` is taken from the URL path — do not include it in the request body.

### Response — `200 OK`

```json
{
  "id": 54,
  "questionId": 88,
  "answeredBy": "Kuchimittai Support Team",
  "answerText": "Yes, this product comes with a 2-year manufacturer warranty. You can register the warranty on our website within 30 days of purchase.",
  "isAdminAnswer": true,
  "createdAt": "2026-05-13T11:20:00+05:30",
  "updatedAt": "2026-05-13T11:20:00+05:30"
}
```

### Error Responses

| Status | Scenario                    |
|--------|-----------------------------|
| `400`  | `answerText` is blank       |
| `500`  | Question ID not found       |

---

## 9. Get Answered Q&A for a Product

**GET** `/api/reviews/questions/product/{productId}`

Returns all questions with status `ANSWERED` for a product, with their answers. This is the public-facing endpoint for the product page.

### Path Parameters

| Parameter   | Type    | Description       |
|-------------|---------|-------------------|
| `productId` | Integer | ID of the product |

### Example Request

```
GET /api/reviews/questions/product/101
```

### Response — `200 OK`

```json
{
  "productId": 101,
  "totalQuestions": 2,
  "questions": [
    {
      "id": 88,
      "productId": 101,
      "customerId": 55,
      "customerName": "Arun Kumar",
      "questionText": "Does this product come with a warranty? If yes, how many years?",
      "status": "ANSWERED",
      "answers": [
        {
          "id": 54,
          "questionId": 88,
          "answeredBy": "Kuchimittai Support Team",
          "answerText": "Yes, this product comes with a 2-year manufacturer warranty. You can register the warranty on our website within 30 days of purchase.",
          "isAdminAnswer": true,
          "createdAt": "2026-05-13T11:20:00+05:30",
          "updatedAt": "2026-05-13T11:20:00+05:30"
        }
      ],
      "createdAt": "2026-05-13T11:00:00+05:30",
      "updatedAt": "2026-05-13T11:20:00+05:30"
    },
    {
      "id": 75,
      "productId": 101,
      "customerId": 40,
      "customerName": "Meena R",
      "questionText": "Is this product available in different colors?",
      "status": "ANSWERED",
      "answers": [
        {
          "id": 48,
          "questionId": 75,
          "answeredBy": "Kuchimittai Support Team",
          "answerText": "Currently this product is available in Red, Blue, and Black. We will be adding more colors soon.",
          "isAdminAnswer": true,
          "createdAt": "2026-05-10T09:00:00+05:30",
          "updatedAt": "2026-05-10T09:00:00+05:30"
        },
        {
          "id": 50,
          "questionId": 75,
          "answeredBy": "Suresh M",
          "answerText": "I bought the blue color and it looks great!",
          "isAdminAnswer": false,
          "createdAt": "2026-05-11T14:30:00+05:30",
          "updatedAt": "2026-05-11T14:30:00+05:30"
        }
      ],
      "createdAt": "2026-05-09T16:45:00+05:30",
      "updatedAt": "2026-05-10T09:00:00+05:30"
    }
  ]
}
```

---

## 10. Get All Q&A for a Product (Admin)

**GET** `/api/reviews/questions/product/{productId}/all`

Returns **all questions** (PENDING, ANSWERED, CLOSED) for a product. Intended for admin review.

### Example Request

```
GET /api/reviews/questions/product/101/all
```

### Response — `200 OK`

```json
{
  "productId": 101,
  "totalQuestions": 4,
  "questions": [
    {
      "id": 90,
      "productId": 101,
      "customerId": 62,
      "customerName": "Kavitha S",
      "questionText": "Can I return this product if it is defective?",
      "status": "PENDING",
      "answers": [],
      "createdAt": "2026-05-13T13:00:00+05:30",
      "updatedAt": "2026-05-13T13:00:00+05:30"
    },
    {
      "id": 88,
      "productId": 101,
      "customerId": 55,
      "customerName": "Arun Kumar",
      "questionText": "Does this product come with a warranty?",
      "status": "ANSWERED",
      "answers": [
        {
          "id": 54,
          "questionId": 88,
          "answeredBy": "Kuchimittai Support Team",
          "answerText": "Yes, 2-year manufacturer warranty.",
          "isAdminAnswer": true,
          "createdAt": "2026-05-13T11:20:00+05:30",
          "updatedAt": "2026-05-13T11:20:00+05:30"
        }
      ],
      "createdAt": "2026-05-13T11:00:00+05:30",
      "updatedAt": "2026-05-13T11:20:00+05:30"
    }
  ]
}
```

---

## 11. Get Questions by Status (Admin)

**GET** `/api/reviews/questions/status/{status}`

Returns all questions across all products filtered by a specific status. Useful for the admin Q&A moderation queue.

### Path Parameters

| Parameter | Type   | Values                              |
|-----------|--------|-------------------------------------|
| `status`  | String | `PENDING` · `ANSWERED` · `CLOSED`   |

### Example Request

```
GET /api/reviews/questions/status/PENDING
```

### Response — `200 OK`

```json
{
  "productId": null,
  "totalQuestions": 3,
  "questions": [
    {
      "id": 90,
      "productId": 101,
      "customerId": 62,
      "customerName": "Kavitha S",
      "questionText": "Can I return this product if it is defective?",
      "status": "PENDING",
      "answers": [],
      "createdAt": "2026-05-13T13:00:00+05:30",
      "updatedAt": "2026-05-13T13:00:00+05:30"
    },
    {
      "id": 91,
      "productId": 205,
      "customerId": 70,
      "customerName": "Selvam P",
      "questionText": "What is the maximum weight this product can hold?",
      "status": "PENDING",
      "answers": [],
      "createdAt": "2026-05-13T10:00:00+05:30",
      "updatedAt": "2026-05-13T10:00:00+05:30"
    }
  ]
}
```

---

## 12. Update Question Status (Admin)

**PATCH** `/api/reviews/questions/{questionId}/status?status={status}`

Manually update the status of a question (e.g., close a resolved question).

### Path Parameters

| Parameter    | Type | Description        |
|--------------|------|--------------------|
| `questionId` | Long | ID of the question |

### Query Parameters

| Parameter | Type   | Values                            |
|-----------|--------|-----------------------------------|
| `status`  | String | `PENDING` · `ANSWERED` · `CLOSED` |

### Example Request

```
PATCH /api/reviews/questions/90/status?status=CLOSED
```

### Response — `200 OK`

```json
{
  "id": 90,
  "productId": 101,
  "customerId": 62,
  "customerName": "Kavitha S",
  "questionText": "Can I return this product if it is defective?",
  "status": "CLOSED",
  "answers": [],
  "createdAt": "2026-05-13T13:00:00+05:30",
  "updatedAt": "2026-05-13T14:30:00+05:30"
}
```

### Error Responses

| Status | Scenario              |
|--------|-----------------------|
| `500`  | Question ID not found |

---

## 13. Delete a Question (Admin)

**DELETE** `/api/reviews/questions/{questionId}`

Permanently deletes a question **and all its answers** (cascade delete).

### Path Parameters

| Parameter    | Type | Description        |
|--------------|------|--------------------|
| `questionId` | Long | ID of the question |

### Example Request

```
DELETE /api/reviews/questions/90
```

### Response — `204 No Content`

_(empty body)_

---

## 14. Delete an Answer (Admin)

**DELETE** `/api/reviews/answers/{answerId}`

Permanently deletes a single answer. The question's status is **not** automatically reverted.

### Path Parameters

| Parameter  | Type | Description      |
|------------|------|------------------|
| `answerId` | Long | ID of the answer |

### Example Request

```
DELETE /api/reviews/answers/54
```

### Response — `204 No Content`

_(empty body)_

---

## Common Error Responses

| HTTP Status | Meaning                                                  |
|-------------|----------------------------------------------------------|
| `200 OK`    | Request successful                                       |
| `204 No Content` | Delete successful (no body returned)              |
| `400 Bad Request` | Validation failed (missing required field, invalid rating value, blank text) |
| `500 Internal Server Error` | Server error or resource not found      |

---

## Sample cURL Commands

### Submit a review
```bash
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 101,
    "customerId": 55,
    "rating": 4,
    "title": "Great quality product!",
    "reviewText": "The product arrived on time and the quality is excellent.",
    "imageUrls": []
  }'
```

### Get approved reviews for a product
```bash
curl -X GET http://localhost:8080/api/reviews/product/101
```

### Approve a review (admin)
```bash
curl -X PATCH "http://localhost:8080/api/reviews/315/status?status=APPROVED"
```

### Post a question
```bash
curl -X POST http://localhost:8080/api/reviews/questions \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 101,
    "customerId": 55,
    "questionText": "Does this product come with a warranty?"
  }'
```

### Post an answer (admin)
```bash
curl -X POST http://localhost:8080/api/reviews/questions/88/answers \
  -H "Content-Type: application/json" \
  -d '{
    "answeredBy": "Kuchimittai Support Team",
    "answerText": "Yes, 2-year manufacturer warranty is included.",
    "isAdminAnswer": true
  }'
```

### Get answered Q&A for a product
```bash
curl -X GET http://localhost:8080/api/reviews/questions/product/101
```

### Get all pending questions (admin)
```bash
curl -X GET http://localhost:8080/api/reviews/questions/status/PENDING
```

### Close a question (admin)
```bash
curl -X PATCH "http://localhost:8080/api/reviews/questions/90/status?status=CLOSED"
```

### Delete a review (admin)
```bash
curl -X DELETE http://localhost:8080/api/reviews/315
```

### Delete a question (admin)
```bash
curl -X DELETE http://localhost:8080/api/reviews/questions/90
```

---

## Data Flow Diagrams

### Review Lifecycle
```
Customer submits review
        │
        ▼
  status = PENDING
        │
        ▼
  Admin reviews it
     /       \
APPROVED   REJECTED
    │
    ▼
Visible on product page
(GET /api/reviews/product/{id})
```

### Question & Answer Lifecycle
```
Customer posts question
        │
        ▼
  status = PENDING
        │
        ▼
  Admin/Community answers it
  (POST /questions/{id}/answers)
        │
        ▼
  status auto-changed → ANSWERED
        │
        ▼
  Visible on product page
  (GET /questions/product/{id})
        │
        ▼ (optional)
  Admin closes it → CLOSED
```

