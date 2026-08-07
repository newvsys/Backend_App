# Push Notification API (Firebase Cloud Messaging)

Sends real-time push notifications to the Admin web app, e.g. "New order received", using
Firebase Cloud Messaging (FCM).

## How it works

1. When a new order is created successfully (`POST /api/orders`), the backend automatically
   sends a push notification titled **"New Order Received"** to every registered admin
   device token.
2. The Admin web app must register its FCM browser token (obtained via the Firebase JS SDK)
   with the backend so it knows where to send notifications.
3. Sending is best-effort — if Firebase isn't configured/enabled, or a send fails, order
   creation is never affected.

## Backend setup

1. Create a Firebase project and generate a **service account key** JSON
   (Project Settings → Service Accounts → Generate new private key).
2. Place the file on the server (or in `src/main/resources` for local/dev use) and set:
   ```properties
   firebase.enabled=true
   firebase.service-account-file=classpath:firebase-service-account.json
   # or: file:/etc/secrets/firebase-service-account.json
   ```
   Environment variable overrides are also supported:
   `FIREBASE_ENABLED=true`, `FIREBASE_SERVICE_ACCOUNT_FILE=file:/path/to/key.json`
3. **Never commit** the service-account JSON file to source control.

## Endpoints

### Register a device token
`POST /api/push/register-token`

Request body:
```json
{
  "token": "<FCM browser token from Firebase JS SDK>",
  "userId": 12,
  "role": "admin",
  "platform": "WEB"
}
```
`role` and `platform` are optional — default to `admin` and `WEB`.

Response:
```json
{ "responseMessage": "Device token registered successfully", "responseStatus": "success" }
```

### Unregister a device token
`POST /api/push/unregister-token`

Request body:
```json
{ "token": "<FCM browser token>" }
```

Call this on admin logout to stop sending notifications to that browser/device.

## Notification payload sent to Admin web app

```json
{
  "notification": {
    "title": "New Order Received",
    "body": "Order #ORD-100234 placed by John - INR 799.00"
  },
  "data": {
    "type": "ORDER_CREATED",
    "orderNumber": "ORD-100234",
    "orderId": "1023",
    "amount": "799.00"
  }
}
```

The Admin web app's Firebase Messaging service worker / foreground handler can use the
`data.type` field to route the click (e.g. navigate to `/orders/{orderNumber}`).

## Invalid / expired tokens

If FCM reports a token as unregistered/invalid, the backend automatically deactivates it
in the `device_tokens` table so future sends skip it.

