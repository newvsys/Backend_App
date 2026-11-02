# Design Plan — Converting TryNat into a Zomato-like Meals Delivery Platform

> **Status:** Proposal / Architecture Blueprint
> **Author:** Engineering
> **Date:** 2026-10-28
> **Source baseline:** Spring Boot 3.3.4 monolith (`com.user`), PostgreSQL, Docker/AWS, Razorpay, Firebase, Cloudinary, Shiprocket.

---

## 1. Executive Summary

The current application is a **single-seller packaged-goods e-commerce backend** (cooking oils, snacks) built around:
- `ProductEO` + `ProductVariantEO` with **SKU-based inventory** (`InventoryEO`, stock quantities).
- **Third-party parcel courier** fulfillment via Shiprocket (AWB, labels, cartons, pickup).
- A **single `WarehouseEO`** origin.
- Razorpay payments, Firebase push, Cloudinary media, OTP auth, reviews, returns/refunds.

A Zomato-like platform is fundamentally different in **four** ways:

| Dimension | Today (TryNat) | Target (Food Delivery) |
|---|---|---|
| **Supply** | 1 seller, 1 warehouse | **N restaurants** (multi-tenant marketplace) |
| **Catalog** | Durable SKUs with stock counts | **Perishable menu items**, made-to-order, time-windowed availability |
| **Fulfillment** | Courier parcel (days, AWB) | **Hyperlocal rider delivery** (30–45 min, live GPS) |
| **Order lifecycle** | Pay → pack → ship → track by courier | **Real-time**: place → restaurant accept → cook → rider assign → pickup → live-track → deliver |

This plan covers the **domain model**, **new subsystems** (geo-search, dispatch engine, live tracking, rider app), **API surface**, **real-time architecture**, **reuse map**, and a **phased roadmap**.

---

## 2. Gap Analysis — Reuse / Modify / Build-New / Retire

### Reuse as-is (high leverage)
- **Auth & OTP** — `UserEO`, `CustomerEO`, `OtpService`, `OTPStoreEO` → customer login works unchanged.
- **Payments** — `RazorpayService`, `PaymentEO`, `RefundTransactionEO` → reuse for order payment + refunds.
- **Notifications** — `PushNotificationService` (Firebase), email, `FlashMessage` → reuse, add new event types.
- **Media** — `CloudinaryMediaService` → restaurant logos, dish photos.
- **Reviews/Q&A** — `ProductReviewEO`, `ReviewImageEO` → generalize to restaurant + dish + rider ratings.
- **Order core** — `OrderEO`, `OrderItemEO`, `OrderAddressEO`, `OrderNumberService`, `OrderCancelRequestEO`.

### Modify / Generalize
- `ProductEO` → **`MenuItemEO`** (belongs to a restaurant, no stock count, has availability window).
- `ProductCategoriesEO` → **menu sections** (Starters, Main Course) scoped per restaurant + global cuisines.
- `InventoryEO` → replaced by lightweight **item availability toggle** (in-stock/86'd) — no quantity ledger.
- `WarehouseEO` → **`RestaurantEO`** (many, each with geo-location + operating hours).
- `DeliveryChargeEO` → **distance/zone-based dynamic delivery fee** engine.

### Build New (the heart of the platform)
- **Restaurant/Merchant domain** — onboarding, KYC/FSSAI, menus, hours, serviceability.
- **Geo-search & serviceability** — "restaurants near me", ETA, delivery-radius check.
- **Rider (delivery partner) domain** — rider app, availability, live location, earnings.
- **Dispatch/Assignment engine** — match order → nearest available rider.
- **Real-time order state machine** — WebSocket/push for live status + map tracking.
- **Merchant dashboard** — accept/reject, mark ready, KPT (kitchen prep time).
- **Dynamic pricing** — delivery fee, surge, packaging charge, item-level GST.

### Retire (Shiprocket parcel stack)
`ShiprocketService`, `ShippingEO`, AWB/label/carton/courier-selection, `ShipmentTrackingHistoryEO`, `CartonEO`, `LabelConfigEO` — **not used** for hyperlocal food. Keep code archived; replace with in-house dispatch + rider tracking.

---

## 3. Target Domain Model (New / Changed Entities)

```
RestaurantEO
  id, name, slug, description, cuisineTypes[], fssaiLicense, gstin,
  ownerUserId, phone, email, logoUrl, bannerUrl,
  latitude, longitude, geohash, addressLine, city, pincode,
  avgPrepTimeMins, deliveryRadiusKm, status(ACTIVE/PAUSED/CLOSED),
  isPureVeg, priceForTwo, rating, ratingCount, commissionPct

RestaurantHoursEO           // weekly operating schedule
  id, restaurantId, dayOfWeek, openTime, closeTime, isClosed

MenuSectionEO               // "Starters", "Biryani", per restaurant
  id, restaurantId, name, displayOrder

MenuItemEO                  // was ProductEO
  id, restaurantId, sectionId, name, description, imageUrl,
  price, isVeg, spiceLevel, servesCount, isBestseller,
  isAvailable(bool), availableFrom, availableTo, packagingCharge,
  gstPct, status

MenuItemAddonGroupEO        // "Choose size", "Add-ons", "Spice level"
  id, menuItemId, name, minSelect, maxSelect, isRequired

MenuItemAddonEO             // "Extra cheese +30", "Large +50"
  id, addonGroupId, name, price, isDefault

RiderEO                     // delivery partner
  id, userId, name, phone, vehicleType, vehicleNumber,
  status(OFFLINE/AVAILABLE/ON_DELIVERY), currentLat, currentLng,
  currentGeohash, rating, totalDeliveries, isKycVerified

RiderLocationPingEO         // time-series (or Redis) live GPS
  riderId, lat, lng, heading, speed, recordedAt

DeliveryEO                  // replaces ShippingEO
  id, orderId, riderId, status(SEARCHING/ASSIGNED/ARRIVED_PICKUP/
     PICKED_UP/ARRIVED_DROP/DELIVERED/CANCELLED),
  pickupLat/Lng, dropLat/Lng, distanceKm, deliveryFee,
  assignedAt, pickedUpAt, deliveredAt, estimatedDeliveryAt, otp

OrderEO (extended)
  + restaurantId, deliveryId, orderType(DELIVERY/PICKUP),
  + kitchenPrepTimeMins, packagingCharge, deliveryFee, taxAmount,
  + scheduledFor (nullable = ASAP), instructions

OrderStatusEvent            // state-machine audit (like your *StatusHistory EOs)
  orderId, fromStatus, toStatus, actor(CUSTOMER/RESTAURANT/RIDER/SYSTEM), at

CustomerAddressEO (extended)
  + latitude, longitude, geohash, addressType(HOME/WORK/OTHER), label
```

### Order State Machine
```
CREATED → PAYMENT_CONFIRMED → RESTAURANT_PENDING
   → RESTAURANT_ACCEPTED → PREPARING → READY_FOR_PICKUP
   → RIDER_ASSIGNED → PICKED_UP → OUT_FOR_DELIVERY → DELIVERED
Terminal/alt: RESTAURANT_REJECTED, CANCELLED, REFUNDED
```
Each transition emits an event → push notification + WebSocket broadcast + audit row.

---

## 4. New Subsystems

### 4.1 Geo-Search & Serviceability
- Store `latitude/longitude` + **geohash** on restaurants, riders, addresses.
- **PostgreSQL + PostGIS** (or `cube`/`earthdistance`) for radius queries; cache hot results in Redis.
- APIs: nearby restaurants, is-serviceable(address), ETA estimate (haversine × road factor + prep time).
- Index strategy: geohash prefix index + GiST spatial index.

### 4.2 Dispatch / Assignment Engine
- Triggered when order hits `READY_FOR_PICKUP` (or earlier, predictive).
- Find candidate riders within radius, `AVAILABLE`, ranked by distance/rating/load.
- **Offer → accept/timeout** loop (e.g. 20s TTL) with fallback to next rider.
- Implemented as async worker (Spring `@Async` now; **Kafka/Redis queue** at scale).
- Prevent double-assignment via Redis distributed lock on rider id.

### 4.3 Real-Time Tracking
- **WebSocket (STOMP over STtomp/SockJS)** channel per order: `/topic/order/{id}`.
- Rider app pushes GPS pings (every 3–5s) → Redis → fan-out to subscribed customer.
- Fallback to FCM push for status changes (works when app backgrounded).

### 4.4 Rider App Backend
- Rider auth (reuse OTP), go-online/offline, accept/reject offers, status updates,
  proof-of-delivery (OTP + photo via Cloudinary), earnings ledger.

### 4.5 Merchant Dashboard Backend
- Restaurant onboarding/KYC, menu CRUD (reuse product CRUD patterns),
  live order queue, accept/reject + set prep time, 86 (mark item unavailable),
  toggle open/closed, payouts/settlement report.

### 4.6 Dynamic Pricing
- Delivery fee = base + (perKm × distance) + surge multiplier (demand/weather/time).
- Item GST (5% food), packaging charge, platform fee; itemized in order summary.

---

## 5. Module / Service Decomposition

Start as a **modular monolith** (fastest, reuses current deployment), extract to services later.

```
com.user
 ├─ identity      (users, riders, merchants, OTP, auth)      [reuse]
 ├─ restaurant    (RestaurantEO, hours, menu, addons)        [new]
 ├─ catalog       (search, geo, cuisines)                    [new]
 ├─ cart & order  (OrderEO extended, pricing, checkout)      [modify]
 ├─ payment       (Razorpay, refunds)                        [reuse]
 ├─ dispatch      (assignment engine, rider matching)        [new]
 ├─ delivery      (DeliveryEO, tracking, POD)                [new]
 ├─ realtime      (WebSocket hub, location fan-out)          [new]
 ├─ notification  (FCM, email, SMS, in-app)                  [reuse+extend]
 └─ media         (Cloudinary)                               [reuse]
```

**Future microservice split** (when load demands): `dispatch`, `realtime/location`, and `catalog/geo-search` are the first candidates to extract (highest, spikiest load).

---

## 6. Representative New APIs

```
# Customer
GET  /api/restaurants/nearby?lat=&lng=&cuisine=&sort=rating|distance|eta
GET  /api/restaurants/{id}/menu
POST /api/serviceability/check            { addressId | lat,lng }
POST /api/cart/validate                   (item availability + pricing)
POST /api/orders                          (place order, returns paymentIntent)
GET  /api/orders/{id}/track               (snapshot) + WS /topic/order/{id}
POST /api/orders/{id}/cancel

# Restaurant / Merchant
POST /api/merchant/restaurants            (onboard)
PUT  /api/merchant/restaurants/{id}/status  (open/close/pause)
POST /api/merchant/menu/items             (reuse product CRUD)
PUT  /api/merchant/menu/items/{id}/availability   (86 toggle)
GET  /api/merchant/orders/live
POST /api/merchant/orders/{id}/accept     { prepTimeMins }
POST /api/merchant/orders/{id}/reject     { reason }
POST /api/merchant/orders/{id}/ready

# Rider
POST /api/rider/status                    (online/offline)
POST /api/rider/location                  (GPS ping)
POST /api/rider/offers/{id}/accept|reject
POST /api/rider/deliveries/{id}/picked-up
POST /api/rider/deliveries/{id}/delivered { otp, photoUrl }
GET  /api/rider/earnings
```

---

## 7. Tech Stack Additions

| Concern | Addition |
|---|---|
| Geo queries | **PostGIS** extension (or `earthdistance`+`cube`) |
| Real-time | **Spring WebSocket + STOMP**, SockJS fallback |
| Live location / caching | **Redis** (rider geo-sets via `GEOADD/GEOSEARCH`, order pub/sub) |
| Async / events at scale | **Kafka** (order events, dispatch offers, location stream) |
| Maps / ETA / routing | Google Maps (Distance Matrix, Directions) or Mapbox/OSRM |
| SMS (rider/customer OTP) | MSG91 / Twilio |
| Observability | Micrometer + Prometheus + Grafana; distributed tracing |

---

## 8. Data Migration & Compatibility

- Create **new tables** alongside existing; do not drop product/shipping tables immediately.
- Seed one "restaurant" from current `WarehouseEO` if a hybrid launch is desired.
- `OrderEO` is **extended (nullable new columns)** — backward compatible; existing orders remain valid.
- Shiprocket tables become read-only/archived.
- Feature-flag the new flow so the two models can coexist during rollout.

---

## 9. Phased Roadmap

**Phase 0 — Foundations (2–3 wks)**
Add PostGIS + Redis; geohash utilities; extend `CustomerAddressEO` with lat/lng; WebSocket skeleton.

**Phase 1 — Restaurant & Menu (3–4 wks)**
`RestaurantEO`, hours, `MenuItemEO`, sections, add-ons; merchant onboarding + menu CRUD (adapt existing product APIs); media reuse.

**Phase 2 — Discovery (2–3 wks)**
Nearby search, serviceability, ETA; cuisine filters; restaurant detail + menu APIs; Redis caching.

**Phase 3 — Ordering & Pricing (3–4 wks)**
Cart validation, dynamic delivery fee + GST + packaging, checkout, Razorpay reuse, order state machine + events.

**Phase 4 — Merchant Ops (2–3 wks)**
Live order queue, accept/reject/ready, 86 toggle, prep-time; push to merchant device.

**Phase 5 — Rider & Dispatch (4–5 wks)**
Rider app backend, go-online, assignment engine, offer/accept loop, Redis locks.

**Phase 6 — Live Tracking (3 wks)**
GPS ping ingestion, WebSocket fan-out, live map + ETA, proof-of-delivery (OTP+photo).

**Phase 7 — Ratings, Payouts, Hardening (3–4 wks)**
Restaurant/dish/rider ratings (extend reviews), merchant settlement + rider earnings, surge pricing, load testing, observability.

---

## 10. Key Risks & Decisions

- **Dispatch correctness** (no double-assignment, no orphaned orders) — needs distributed locks + timeouts + dead-letter handling.
- **Location scale** — thousands of pings/sec → Redis geo + Kafka, not raw DB writes.
- **ETA accuracy** — start with haversine×factor; upgrade to Maps Distance Matrix.
- **Perishability** — no stock ledger; availability is a real-time boolean + hours window.
- **Multi-tenancy & payouts** — commission, settlement, and GST invoicing per restaurant.
- **Monolith-first** is recommended to reuse your current Docker/AWS pipeline; extract hotspots later.

---

## 11. Reuse Scorecard

| Existing asset | Verdict |
|---|---|
| Auth/OTP, Customer, User | ✅ Reuse |
| Razorpay payments & refunds | ✅ Reuse |
| Firebase push, email, flash msgs | ✅ Reuse + new events |
| Cloudinary media | ✅ Reuse |
| Reviews / Q&A / images | 🔁 Generalize (restaurant/dish/rider) |
| Order core + order number | 🔁 Extend |
| Product / variant / inventory | 🔁 Reshape into Menu items |
| Warehouse | 🔁 Becomes Restaurant |
| Delivery charge | 🔁 Becomes dynamic fee engine |
| **Shiprocket / AWB / label / carton** | ❌ Retire (replace with in-house dispatch) |
```

