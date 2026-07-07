# Flash Message API Documentation

**Base URL:** `http://localhost:8080`  
**Content-Type:** `application/json`

> **Full URL pattern:** `http://localhost:8080/api/flash-messages/...`

---

## Overview

The Flash Message API lets administrators create and manage **marquee / ticker messages** displayed on the website banner. Multiple messages can coexist and are rendered in ascending `priority` order. Each message can optionally carry display-date windows, colour overrides, and a click-through URL.

### Status Values

| Value | Meaning                                     |
|-------|---------------------------------------------|
| `A`   | **Active** — message is visible on website  |
| `I`   | **Inactive** — message is hidden            |

### Message Types (suggested values for `type` field)

| Value     | Use-case example                        |
|-----------|-----------------------------------------|
| `INFO`    | General site announcement               |
| `OFFER`   | Promotional / discount message          |
| `WARNING` | Maintenance / downtime notice           |
| `NEWS`    | New product launch or company news      |

---

## Endpoints Summary

| # | Method    | Endpoint                                    | Description                                   |
|---|-----------|---------------------------------------------|-----------------------------------------------|
| 1 | `POST`    | `/api/flash-messages`                       | Create a new flash message                    |
| 2 | `PUT`     | `/api/flash-messages/{id}`                  | Update an existing flash message              |
| 3 | `PATCH`   | `/api/flash-messages/{id}/activate`         | Activate a flash message (status → `A`)       |
| 4 | `PATCH`   | `/api/flash-messages/{id}/deactivate`       | Deactivate a flash message (status → `I`)     |
| 5 | `GET`     | `/api/flash-messages/{id}`                  | Get a single flash message by ID              |
| 6 | `GET`     | `/api/flash-messages?status=A`              | Get all flash messages (optional status filter)|

---

## 1. Create Flash Message

**Endpoint:** `POST /api/flash-messages`  
**Description:** Creates a new flash / marquee message. `status` defaults to `A` (Active) and `priority` defaults to `100` if not provided.

### Request Payload

| Field       | Type          | Required | Description                                                                 |
|-------------|---------------|----------|-----------------------------------------------------------------------------|
| `message`   | String        | **Yes**  | The marquee text content displayed on the website                           |
| `title`     | String        | No       | Short internal label (admin reference only, not shown in the marquee)       |
| `type`      | String        | No       | Category tag: `INFO` \| `OFFER` \| `WARNING` \| `NEWS` etc.                |
| `bgColor`   | String        | No       | Background colour of the marquee banner (e.g. `"#FF5733"` or `"red"`)      |
| `textColor` | String        | No       | Text / font colour (e.g. `"#FFFFFF"`)                                       |
| `speed`     | String        | No       | Scroll speed hint: `"slow"` \| `"normal"` \| `"fast"` or a numeric value   |
| `priority`  | Integer       | No       | Display order — lower number = shown first. Defaults to `100`               |
| `linkUrl`   | String        | No       | URL the marquee text should link to when clicked                            |
| `startDate` | LocalDateTime | No       | Date-time from which the message should appear (`null` = no restriction)    |
| `endDate`   | LocalDateTime | No       | Date-time after which the message should disappear (`null` = no restriction)|
| `status`    | String        | No       | `A` = Active, `I` = Inactive. Defaults to `A`                               |
| `updatedBy` | String        | No       | Admin username creating the record                                          |

### Request — Promotional offer banner

```json
{
  "title": "Summer Sale 2026",
  "message": "🎉 Get 50% off on all items this weekend! Use code SUMMER50 at checkout.",
  "type": "OFFER",
  "bgColor": "#FF5733",
  "textColor": "#FFFFFF",
  "speed": "normal",
  "priority": 1,
  "linkUrl": "https://example.com/summer-sale",
  "startDate": "2026-07-01T00:00:00",
  "endDate": "2026-07-07T23:59:59",
  "status": "A",
  "updatedBy": "admin"
}
```

### Request — General info announcement (minimal)

```json
{
  "message": "Free delivery on all orders above ₹500!",
  "type": "INFO",
  "updatedBy": "admin"
}
```

### Request — Maintenance warning

```json
{
  "title": "Scheduled Maintenance",
  "message": "⚠️ Our website will be under maintenance on July 5th from 2 AM to 4 AM IST. Apologies for the inconvenience.",
  "type": "WARNING",
  "bgColor": "#FFC107",
  "textColor": "#212121",
  "priority": 1,
  "startDate": "2026-07-04T20:00:00",
  "endDate": "2026-07-05T04:00:00",
  "updatedBy": "admin"
}
```

### Response — Success (200 OK)

```json
{
  "id": 1,
  "title": "Summer Sale 2026",
  "message": "🎉 Get 50% off on all items this weekend! Use code SUMMER50 at checkout.",
  "type": "OFFER",
  "bgColor": "#FF5733",
  "textColor": "#FFFFFF",
  "speed": "normal",
  "priority": 1,
  "linkUrl": "https://example.com/summer-sale",
  "startDate": "2026-07-01T00:00:00",
  "endDate": "2026-07-07T23:59:59",
  "status": "A",
  "createdBy": "admin",
  "updatedBy": "admin",
  "createdAt": "2026-07-03T10:00:00",
  "updatedAt": "2026-07-03T10:00:00"
}
```

### Response — Validation Failure (400 Bad Request)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "message is required"
}
```

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Request body must not be null"
}
```

### Response — Server Error (500)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An error occurred while creating the flash message"
}
```

---

## 2. Update Flash Message

**Endpoint:** `PUT /api/flash-messages/{id}`  
**Description:** Partially updates an existing flash message. Only the **non-null** fields in the request body are applied — omit a field to leave it unchanged.

### Path Parameter

| Parameter | Type | Required | Description                   |
|-----------|------|----------|-------------------------------|
| `id`      | Long | **Yes**  | ID of the flash message to update |

### Request Payload (all fields optional)

| Field       | Type          | Description                                                                 |
|-------------|---------------|-----------------------------------------------------------------------------|
| `message`   | String        | Updated marquee text                                                        |
| `title`     | String        | Updated internal title                                                      |
| `type`      | String        | Updated category tag                                                        |
| `bgColor`   | String        | Updated background colour                                                   |
| `textColor` | String        | Updated text colour                                                         |
| `speed`     | String        | Updated scroll speed                                                        |
| `priority`  | Integer       | Updated display priority                                                    |
| `linkUrl`   | String        | Updated click-through URL                                                   |
| `startDate` | LocalDateTime | Updated start date-time                                                     |
| `endDate`   | LocalDateTime | Updated end date-time                                                       |
| `status`    | String        | `A` = Active, `I` = Inactive                                                |
| `updatedBy` | String        | Admin username performing the update                                        |

### Request — Update message text and extend end date

```json
{
  "message": "🎉 Get 50% off on all items! Extended until July 10th. Use code SUMMER50.",
  "endDate": "2026-07-10T23:59:59",
  "updatedBy": "admin"
}
```

### Request — Change priority and colour

```json
{
  "priority": 2,
  "bgColor": "#4CAF50",
  "textColor": "#FFFFFF",
  "updatedBy": "admin"
}
```

### Request — Change status via update

```json
{
  "status": "I",
  "updatedBy": "admin"
}
```

### Response — Success (200 OK)

```json
{
  "id": 1,
  "title": "Summer Sale 2026",
  "message": "🎉 Get 50% off on all items! Extended until July 10th. Use code SUMMER50.",
  "type": "OFFER",
  "bgColor": "#FF5733",
  "textColor": "#FFFFFF",
  "speed": "normal",
  "priority": 1,
  "linkUrl": "https://example.com/summer-sale",
  "startDate": "2026-07-01T00:00:00",
  "endDate": "2026-07-10T23:59:59",
  "status": "A",
  "createdBy": "admin",
  "updatedBy": "admin",
  "createdAt": "2026-07-03T10:00:00",
  "updatedAt": "2026-07-03T11:30:00"
}
```

### Response — Not Found (404)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Flash message not found with id=99"
}
```

### Response — Validation Failure (400 Bad Request)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Request body must not be null"
}
```

### Response — Server Error (500)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An error occurred while updating the flash message"
}
```

---

## 3. Activate Flash Message

**Endpoint:** `PATCH /api/flash-messages/{id}/activate`  
**Description:** Sets the flash message status to `A` (Active), making it immediately visible on the website. No request body is required.

### Path Parameter

| Parameter | Type | Required | Description                     |
|-----------|------|----------|---------------------------------|
| `id`      | Long | **Yes**  | ID of the flash message to activate |

### Request

```
PATCH /api/flash-messages/1/activate
```

### Response — Success (200 OK)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Flash message activated successfully"
}
```

### Response — Not Found (404)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Flash message not found with id=99"
}
```

### Response — Validation Failure (400 Bad Request)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "id is required"
}
```

### Response — Server Error (500)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An error occurred while activating the flash message"
}
```

---

## 4. Deactivate Flash Message

**Endpoint:** `PATCH /api/flash-messages/{id}/deactivate`  
**Description:** Sets the flash message status to `I` (Inactive), hiding it from the website immediately. The record is **retained** in the database. No request body is required.

### Path Parameter

| Parameter | Type | Required | Description                       |
|-----------|------|----------|-----------------------------------|
| `id`      | Long | **Yes**  | ID of the flash message to deactivate |

### Request

```
PATCH /api/flash-messages/1/deactivate
```

### Response — Success (200 OK)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Flash message deactivated successfully"
}
```

### Response — Not Found (404)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Flash message not found with id=99"
}
```

### Response — Validation Failure (400 Bad Request)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "id is required"
}
```

### Response — Server Error (500)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An error occurred while deactivating the flash message"
}
```

---

## 5. Get Flash Message by ID

**Endpoint:** `GET /api/flash-messages/{id}`  
**Description:** Returns a single flash message by its numeric ID.

### Path Parameter

| Parameter | Type | Required | Description              |
|-----------|------|----------|--------------------------|
| `id`      | Long | **Yes**  | ID of the flash message  |

### Request

```
GET /api/flash-messages/1
```

### Response — Success (200 OK)

```json
{
  "id": 1,
  "title": "Summer Sale 2026",
  "message": "🎉 Get 50% off on all items this weekend! Use code SUMMER50 at checkout.",
  "type": "OFFER",
  "bgColor": "#FF5733",
  "textColor": "#FFFFFF",
  "speed": "normal",
  "priority": 1,
  "linkUrl": "https://example.com/summer-sale",
  "startDate": "2026-07-01T00:00:00",
  "endDate": "2026-07-07T23:59:59",
  "status": "A",
  "createdBy": "admin",
  "updatedBy": "admin",
  "createdAt": "2026-07-03T10:00:00",
  "updatedAt": "2026-07-03T10:00:00"
}
```

### Response — Not Found (404)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Flash message not found with id=99"
}
```

### Response — Validation Failure (400 Bad Request)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "id is required"
}
```

### Response — Server Error (500)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An error occurred while fetching the flash message"
}
```

---

## 6. Get Flash Messages (by Status)

**Endpoint:** `GET /api/flash-messages`  
**Description:** Returns all flash messages ordered by `priority` ascending (lower number first), then by `id` ascending. Pass the optional `status` query parameter to filter results.

> **Recommended for the public website:** call `GET /api/flash-messages?status=A` to fetch only the messages currently meant to be displayed.

### Query Parameters

| Parameter | Type   | Required | Description                                                                  |
|-----------|--------|----------|------------------------------------------------------------------------------|
| `status`  | String | No       | `A` = Active only, `I` = Inactive only. **Omit** to return all messages.    |

### Request — All messages (admin dashboard)

```
GET /api/flash-messages
```

### Request — Active messages only (public website / frontend)

```
GET /api/flash-messages?status=A
```

### Request — Inactive messages only

```
GET /api/flash-messages?status=I
```

### Response — Success (200 OK) — multiple messages

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Flash messages fetched successfully",
  "totalCount": 3,
  "flashMessages": [
    {
      "id": 1,
      "title": "Summer Sale 2026",
      "message": "🎉 Get 50% off on all items this weekend! Use code SUMMER50 at checkout.",
      "type": "OFFER",
      "bgColor": "#FF5733",
      "textColor": "#FFFFFF",
      "speed": "normal",
      "priority": 1,
      "linkUrl": "https://example.com/summer-sale",
      "startDate": "2026-07-01T00:00:00",
      "endDate": "2026-07-07T23:59:59",
      "status": "A",
      "createdBy": "admin",
      "updatedBy": "admin",
      "createdAt": "2026-07-03T10:00:00",
      "updatedAt": "2026-07-03T10:00:00"
    },
    {
      "id": 2,
      "title": "Free Delivery Banner",
      "message": "Free delivery on all orders above ₹500!",
      "type": "INFO",
      "bgColor": "#4CAF50",
      "textColor": "#FFFFFF",
      "speed": "normal",
      "priority": 2,
      "linkUrl": null,
      "startDate": null,
      "endDate": null,
      "status": "A",
      "createdBy": "admin",
      "updatedBy": null,
      "createdAt": "2026-07-03T10:05:00",
      "updatedAt": "2026-07-03T10:05:00"
    },
    {
      "id": 3,
      "title": "Old Campaign",
      "message": "Spring offers have ended.",
      "type": "INFO",
      "bgColor": null,
      "textColor": null,
      "speed": null,
      "priority": 100,
      "linkUrl": null,
      "startDate": null,
      "endDate": "2026-06-30T23:59:59",
      "status": "I",
      "createdBy": "admin",
      "updatedBy": "admin",
      "createdAt": "2026-06-01T09:00:00",
      "updatedAt": "2026-07-01T00:01:00"
    }
  ]
}
```

### Response — Success (200 OK) — active messages only (`?status=A`)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Flash messages fetched successfully",
  "totalCount": 2,
  "flashMessages": [
    {
      "id": 1,
      "title": "Summer Sale 2026",
      "message": "🎉 Get 50% off on all items this weekend! Use code SUMMER50 at checkout.",
      "type": "OFFER",
      "bgColor": "#FF5733",
      "textColor": "#FFFFFF",
      "speed": "normal",
      "priority": 1,
      "linkUrl": "https://example.com/summer-sale",
      "startDate": "2026-07-01T00:00:00",
      "endDate": "2026-07-07T23:59:59",
      "status": "A",
      "createdBy": "admin",
      "updatedBy": "admin",
      "createdAt": "2026-07-03T10:00:00",
      "updatedAt": "2026-07-03T10:00:00"
    },
    {
      "id": 2,
      "title": "Free Delivery Banner",
      "message": "Free delivery on all orders above ₹500!",
      "type": "INFO",
      "bgColor": "#4CAF50",
      "textColor": "#FFFFFF",
      "speed": "normal",
      "priority": 2,
      "linkUrl": null,
      "startDate": null,
      "endDate": null,
      "status": "A",
      "createdBy": "admin",
      "updatedBy": null,
      "createdAt": "2026-07-03T10:05:00",
      "updatedAt": "2026-07-03T10:05:00"
    }
  ]
}
```

### Response — Empty List (200 OK)

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Flash messages fetched successfully",
  "totalCount": 0,
  "flashMessages": []
}
```

### Response — Server Error (500)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "An error occurred while fetching flash messages",
  "totalCount": 0,
  "flashMessages": null
}
```

---

## Error Reference

| HTTP Status | `responseStatus` | Scenario                                               |
|-------------|-----------------|--------------------------------------------------------|
| `200 OK`    | `SUCCESS`       | Request processed successfully                         |
| `400`       | `FAILURE`       | Missing required field (`message`) or null request body|
| `404`       | `FAILURE`       | Flash message not found for the given ID               |
| `500`       | `FAILURE`       | Unexpected server-side error                           |

---

## Field Reference — `FlashMessageResponseDTO`

| Field       | Type          | Description                                                                   |
|-------------|---------------|-------------------------------------------------------------------------------|
| `id`        | Long          | Auto-generated primary key                                                    |
| `title`     | String        | Internal label (not shown on the website)                                     |
| `message`   | String        | The marquee text shown on the website banner                                  |
| `type`      | String        | Category tag — `INFO` / `OFFER` / `WARNING` / `NEWS` etc.                    |
| `bgColor`   | String        | CSS background colour for the marquee banner (e.g. `"#FF5733"`)              |
| `textColor` | String        | CSS text colour (e.g. `"#FFFFFF"`)                                            |
| `speed`     | String        | Scroll speed hint — `"slow"` / `"normal"` / `"fast"` or numeric              |
| `priority`  | Integer       | Display order — lower number = rendered first when multiple messages are active|
| `linkUrl`   | String        | Optional click-through URL for the marquee text                               |
| `startDate` | LocalDateTime | Date-time the message should start appearing (`null` = no restriction)        |
| `endDate`   | LocalDateTime | Date-time the message should stop appearing (`null` = no restriction)         |
| `status`    | String        | `A` = Active (visible), `I` = Inactive (hidden)                               |
| `createdBy` | String        | Admin who created the record                                                  |
| `updatedBy` | String        | Admin who last updated the record                                             |
| `createdAt` | LocalDateTime | Record creation timestamp                                                     |
| `updatedAt` | LocalDateTime | Last update timestamp                                                         |

---

## Database Table — `flash_messages`

The table is auto-created by Hibernate (`ddl-auto=update`) on application startup.

```sql
CREATE TABLE flash_messages (
    id          BIGSERIAL       PRIMARY KEY,
    title       VARCHAR(200),
    message     TEXT            NOT NULL,
    type        VARCHAR(50),
    bg_color    VARCHAR(20),
    text_color  VARCHAR(20),
    speed       VARCHAR(20),
    priority    INTEGER         NOT NULL DEFAULT 100,
    link_url    VARCHAR(500),
    start_date  TIMESTAMP,
    end_date    TIMESTAMP,
    status      CHAR(1)         NOT NULL DEFAULT 'A',  -- 'A'=Active, 'I'=Inactive
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);
```

### Sample Seed Data

```sql
-- Active promotional offer
INSERT INTO flash_messages
    (title, message, type, bg_color, text_color, speed, priority, link_url, start_date, end_date, status, created_by)
VALUES
    ('Summer Sale 2026',
     '🎉 Get 50% off on all items this weekend! Use code SUMMER50 at checkout.',
     'OFFER', '#FF5733', '#FFFFFF', 'normal', 1,
     'https://example.com/summer-sale',
     '2026-07-01 00:00:00', '2026-07-07 23:59:59', 'A', 'admin');

-- Always-on free-delivery info banner
INSERT INTO flash_messages
    (title, message, type, bg_color, text_color, speed, priority, status, created_by)
VALUES
    ('Free Delivery Banner',
     'Free delivery on all orders above ₹500!',
     'INFO', '#4CAF50', '#FFFFFF', 'normal', 2, 'A', 'admin');
```

---

## Front-End Integration Guide

### Recommended API Call

The front-end should call the **active-only** endpoint on page load:

```
GET /api/flash-messages?status=A
```

### Rendering Logic (pseudo-code)

```js
const { flashMessages } = await fetch('/api/flash-messages?status=A').then(r => r.json());

// Filter by date window (optional — server does not filter by date automatically)
const now = new Date();
const visible = flashMessages.filter(m => {
  const afterStart = !m.startDate || new Date(m.startDate) <= now;
  const beforeEnd  = !m.endDate   || new Date(m.endDate)   >= now;
  return afterStart && beforeEnd;
});

// Render marquee for each visible message in priority order
visible.forEach(m => renderMarquee(m));
```

> **Note:** The server returns all Active (`status=A`) messages regardless of `startDate`/`endDate`. It is the **front-end's responsibility** to apply the date-window check if needed, or the back-end can be extended to filter by date in the future.

### Marquee HTML Example

```html
<!-- Single message marquee -->
<div style="background-color: {{ m.bgColor }}; color: {{ m.textColor }}">
  <marquee scrollamount="{{ speedToPixels(m.speed) }}">
    <a href="{{ m.linkUrl }}" target="_blank">{{ m.message }}</a>
  </marquee>
</div>
```

---

## Admin Workflow Examples

### Create and immediately activate a flash message

```
POST /api/flash-messages
Body: { "message": "New arrivals are live!", "type": "NEWS", "priority": 1, "status": "A", "updatedBy": "admin" }
```

### Create an inactive draft, then activate it later

```
POST /api/flash-messages
Body: { "message": "Big Sale coming soon!", "status": "I", "updatedBy": "admin" }

--- (later, when ready to go live) ---

PATCH /api/flash-messages/3/activate
```

### Temporarily hide a message without deleting it

```
PATCH /api/flash-messages/1/deactivate
```

### Reactivate a previously deactivated message

```
PATCH /api/flash-messages/1/activate
```

### Update message text in place

```
PUT /api/flash-messages/1
Body: { "message": "Sale extended until July 15th! Use code SUMMER50.", "endDate": "2026-07-15T23:59:59", "updatedBy": "admin" }
```

