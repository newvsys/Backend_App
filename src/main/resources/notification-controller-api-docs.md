# Notification Controller API Documentation (FCM Device Tokens)

**Base URL:** `http://localhost:8080`
**Content-Type:** `application/json`

> **Full URL pattern:** `http://localhost:8080/api/push/...`

---

## Overview

The `NotificationController` exposes the endpoints used by the **Admin web app** to register
and unregister **Firebase Cloud Messaging (FCM)** browser/device tokens, so the backend knows
where to deliver real-time push notifications (e.g. **"New Order Received"** alerts fired
automatically when `POST /api/orders` succeeds).

- Tokens are stored in the `device_tokens` table (`DeviceTokenEO`).
- Registering an already-known token **updates** the existing row (re-activates it, refreshes
  `userId`/`role`/`platform`/`updatedAt`) instead of creating a duplicate.
- Unregistering a token does **not** delete the row — it just sets `active = false` so it is
  skipped during future sends, while preserving history.
- Invalid/expired tokens reported by FCM (`UNREGISTERED`, `INVALID_ARGUMENT`,
  `SENDER_ID_MISMATCH`) are automatically deactivated by the backend the next time a send fails.
- These endpoints work regardless of whether Firebase is currently enabled/initialized — token
  registration always succeeds; only the actual *sending* of push notifications depends on
  `firebase.enabled=true` and a valid service-account file being configured
  (see `push-notification-api-docs.md`).

### Related Constants

| Constant                    | Value     | Used for                                          |
|------------------------------|-----------|-----------------------------------------------------|
| `Constants.ROLE_ADMIN`       | `admin`   | Default `role` when not supplied in the request     |
| `Constants.SUCCESS_STATUS`   | `SUCCESS` | `responseStatus` returned on success                |
| `Constants.FAILURE_STATUS`   | `FAILURE` | `responseStatus` returned on validation/server errors|

---

## Endpoints Summary

| # | Method | Endpoint                       | Description                                              |
|---|--------|---------------------------------|------------------------------------------------------------|
| 1 | `POST` | `/api/push/register-token`     | Register (or re-activate) an FCM device token for an admin |
| 2 | `POST` | `/api/push/unregister-token`   | Deactivate an FCM device token (e.g. on admin logout)      |

---

## 1. Register Device Token

**Endpoint:** `POST /api/push/register-token`
**Description:** Registers an FCM browser/device token obtained from the Firebase JS SDK on
the Admin web app so it can receive push notifications. Call this:
- Right after the Firebase Messaging SDK returns a token (on app load / after notification
  permission is granted).
- Whenever the token is refreshed (FCM tokens can rotate).
- On admin login, to (re-)associate the token with the logged-in `userId`.

If the token already exists in the database, the existing record is **updated** (not
duplicated) and re-activated.

### Request Payload — `DeviceTokenRegisterDTO`

| Field      | Type   | Required | Description                                                                 |
|------------|--------|----------|-------------------------------------------------------------------------------|
| `token`    | String | **Yes**  | The FCM registration token from the Firebase JS SDK                          |
| `userId`   | Long   | No       | ID of the admin user registering the token                                   |
| `role`     | String | No       | Defaults to `"admin"` (`Constants.ROLE_ADMIN`) when omitted                   |
| `platform` | String | No       | Defaults to `"WEB"` when omitted                                              |

### Request

```json
POST /api/push/register-token
Content-Type: application/json

{
  "token": "fcm-browser-token-abc123xyz...",
  "userId": 12,
  "role": "admin",
  "platform": "WEB"
}
```

### Request — Minimal (defaults applied)

```json
{
  "token": "fcm-browser-token-abc123xyz..."
}
```

### Response — Success (200 OK)

```json
{
  "responseMessage": "Device token registered successfully",
  "responseStatus": "SUCCESS"
}
```

### Response — Validation Failure (400 Bad Request)

Returned when `token` is missing, null, or blank.

```json
{
  "responseMessage": "Device token is required",
  "responseStatus": "FAILURE"
}
```

### Response — Server Error (500)

```json
{
  "responseMessage": "Failed to register device token",
  "responseStatus": "FAILURE"
}
```

---

## 2. Unregister Device Token

**Endpoint:** `POST /api/push/unregister-token`
**Description:** Deactivates a previously registered FCM token so it stops receiving push
notifications. Call this on **admin logout**, or when the Admin web app detects that
notification permission has been revoked.

The record is retained in the database (not deleted) with `active = false`.

### Request Payload

| Field   | Type   | Required | Description                     |
|---------|--------|----------|----------------------------------|
| `token` | String | **Yes**  | The FCM registration token to deactivate |

### Request

```json
POST /api/push/unregister-token
Content-Type: application/json

{
  "token": "fcm-browser-token-abc123xyz..."
}
```

### Response — Success (200 OK)

> Returned even if the token was not found in the database (unregister is idempotent/best-effort).

```json
{
  "responseMessage": "Device token unregistered successfully",
  "responseStatus": "SUCCESS"
}
```

### Response — Validation Failure (400 Bad Request)

Returned when `token` is missing, null, or blank (or the request body itself is null).

```json
{
  "responseMessage": "Device token is required",
  "responseStatus": "FAILURE"
}
```

### Response — Server Error (500)

```json
{
  "responseMessage": "Failed to unregister device token",
  "responseStatus": "FAILURE"
}
```

---

## Error Reference

| HTTP Status | `responseStatus` | Scenario                                          |
|-------------|-------------------|-----------------------------------------------------|
| `200 OK`    | `SUCCESS`         | Token registered / unregistered successfully         |
| `400`       | `FAILURE`         | `token` missing or blank in the request body        |
| `500`       | `FAILURE`         | Unexpected server-side error (e.g. DB failure)      |

---

## Field Reference — `device_tokens` table (`DeviceTokenEO`)

| Column       | Type          | Description                                                                 |
|--------------|---------------|-------------------------------------------------------------------------------|
| `id`         | Long          | Auto-generated primary key                                                    |
| `token`      | String(512)   | FCM registration token — **unique**                                          |
| `user_id`    | Long          | Optional link to the admin user who registered this token                    |
| `role`       | String(32)    | e.g. `admin` — used to target notifications without a user join              |
| `platform`   | String(32)    | e.g. `WEB` — device/browser platform                                        |
| `active`     | Boolean       | `true` = eligible to receive pushes; `false` = unregistered/invalid          |
| `created_at` | OffsetDateTime| Timestamp the token was first registered                                     |
| `updated_at` | OffsetDateTime| Timestamp of the last register/unregister/deactivation                       |

---

## Front-End Integration Guide (Admin Web App)

### 1. On app load — request permission & get token

```js
import { getMessaging, getToken } from "firebase/messaging";

const messaging = getMessaging(firebaseApp);
const token = await getToken(messaging, { vapidKey: "<YOUR_VAPID_KEY>" });

if (token) {
  await fetch("/api/push/register-token", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token, userId: currentAdminUserId, role: "admin", platform: "WEB" }),
  });
}
```

### 2. Handle foreground notifications

```js
import { onMessage } from "firebase/messaging";

onMessage(messaging, (payload) => {
  // payload.notification.title === "New Order Received"
  // payload.data.orderNumber / payload.data.orderId can be used to route/navigate
  showToast(payload.notification.title, payload.notification.body);
});
```

### 3. On logout

```js
await fetch("/api/push/unregister-token", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ token }),
});
```

### 4. On token refresh

Firebase may rotate the token periodically — listen for refresh and re-call
`/api/push/register-token` with the new token so the old one is superseded.

---

## Related Documentation

See `push-notification-api-docs.md` for:
- Backend Firebase setup (`firebase.enabled`, `firebase.service-account-file`)
- The exact notification payload sent to the Admin web app on new orders
- How invalid/expired tokens are automatically cleaned up

