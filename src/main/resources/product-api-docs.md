# Product API Documentation

**Base URL:** `http://localhost:8080/api`  
**Content-Type:** `application/json` (unless noted)

> The application context path is `/api`, so all controller paths are relative to that.  
> Full URL pattern: `http://localhost:8080/api/products/...`

---

## Endpoints Summary

| #  | Method   | Endpoint                                          | Description                                      |
|----|----------|---------------------------------------------------|--------------------------------------------------|
| 1  | `POST`   | `/api/products/categories`                        | Create a product category                        |
| 2  | `PUT`    | `/api/products/categories/{category_id}`          | Update a product category                        |
| 3  | `DELETE` | `/api/products/categories/{category_id}`          | Soft-delete a category                           |
| 4  | `GET`    | `/api/products/categories`                        | Get all active categories                        |
| 5  | `GET`    | `/api/products/categories/{category_id}`          | Get category by ID                               |
| 6  | `GET`    | `/api/products/categories/{category_id}/products` | Get active products by category ID               |
| 7  | `POST`   | `/api/products/product`                           | Create a product                                 |
| 8  | `GET`    | `/api/products/product`                           | Get all active products                          |
| 9  | `GET`    | `/api/products/product/{product_id}`              | Get product by ID                                |
| 10 | `PUT`    | `/api/products/{product_id}`                      | Update a product                                 |
| 11 | `DELETE` | `/api/products/{product_id}`                      | Soft-delete a product                            |
| 12 | `GET`    | `/api/products/productSlug/{productSlug}`         | **Get product by slug (includes `videoUrl`)**    |
| 13 | `POST`   | `/api/products/productsVariant`                   | Create a product variant (images + video)        |
| 14 | `PUT`    | `/api/products/productsVariant`                   | Update a product variant (images + video)        |
| 15 | `DELETE` | `/api/products/productsVariant/{variant_id}`      | Soft-delete a product variant                    |
| 16 | `DELETE` | `/api/products/productsVariant/{variant_id}/video`| Delete the video of a product variant            |
| 17 | `GET`    | `/api/products/productsVariant/{product_id}`      | Get variants by product ID (images + `isMainImage`) |
| 18 | `POST`   | `/api/products/productAttributes`                 | Create a product attribute                       |
| 19 | `PUT`    | `/api/products/productAttributes/{attributeId}`   | Update a product attribute                       |
| 20 | `DELETE` | `/api/products/productAttributes/{attributeId}`   | Delete a product attribute                       |
| 21 | `GET`    | `/api/products/productImage/{productId}`          | Get images by product variant ID                 |
| 22 | `DELETE` | `/api/products/productImage/{image_id}`           | Delete a product image by ID                     |
| 23 | `GET`    | `/api/products/shop/{category}`                   | Get products by category name with filters       |
| 24 | `GET`    | `/api/products/search`                            | **Search products with full filter support**     |
| 25 | `POST`   | `/api/products/uom`                               | Create a Unit of Measure                         |
| 26 | `PUT`    | `/api/products/uom/{uom_id}`                      | Update a Unit of Measure                         |
| 27 | `DELETE` | `/api/products/uom/{uom_id}`                      | Soft-delete a Unit of Measure                    |
| 28 | `GET`    | `/api/products/uom/{uom_id}`                      | Get Unit of Measure by ID                        |
| 29 | `GET`    | `/api/products/uom`                               | Get all active Units of Measure                  |
| 30 | `PUT`    | `/api/products/productImage/{image_id}/main`      | Set an image as the main image for its variant   |
| 31 | `POST`   | `/api/products/labels/print`                      | **Generate a thermal-printer label PDF**         |
| 32 | `GET`    | `/api/products/labels/jobs`                       | **List label print job history** (filterable)    |
| 33 | `GET`    | `/api/products/labels/jobs/{jobId}`               | **Get a single label print job by ID**           |
| 34 | `POST`   | `/api/products/labels/jobs/{jobId}/reprint`       | **Reprint/re-generate a label PDF by job ID**    |
| 35 | `POST`   | `/api/products/labels/resize-pdf`                 | **Download & resize any PDF to 4"×6" thermal label** |
| 36 | `POST`   | `/api/products/labels/config`                     | **Create a label configuration (MASTER)**        |
| 37 | `PUT`    | `/api/products/labels/config/{configId}`          | **Update a label configuration**                 |
| 38 | `DELETE` | `/api/products/labels/config/{configId}`          | **Soft-delete a label configuration**            |
| 39 | `GET`    | `/api/products/labels/config/{configId}`          | **Get label configuration by ID**                |
| 40 | `GET`    | `/api/products/labels/config`                     | **List all label configurations**                |
| 41 | `PUT`    | `/api/products/labels/config/{configId}/set-default` | **Set a label configuration as system default** |

> **Inventory APIs have been moved to a dedicated controller.**  
> See [`inventory-api-docs.md`](./inventory-api-docs.md) for all inventory endpoints (`/api/inventory/...`).

---

## Create Product Variant

### `POST /api/products/productsVariant`

Creates a new product variant with optional images and an optional video file.

**Content-Type:** `multipart/form-data`

### Request Parts

| Part             | Annotation      | Type              | Required | Description |
|------------------|-----------------|-------------------|----------|-------------|
| `productVariant` | `@RequestPart`  | JSON string       | ✅ Yes   | Variant details serialized as JSON |
| `images`         | `@RequestParam` | `MultipartFile[]` | ❌ No    | One or more images. **Only `.png` allowed.** |
| `video`          | `@RequestParam` | `MultipartFile`   | ❌ No    | Single video. **Allowed: `.mp4`, `.mov`, `.avi`, `.webm`** |
| `mainImageIndex` | `@RequestParam` | `int`             | ❌ No    | 0-based index of the image to mark as main. Defaults to `0` (first image). Clamped to valid range. |

> `images` and `video` use `@RequestParam` (not `@RequestPart`) so Spring resolves them to `null` when absent, avoiding a 400 error caused by missing part `Content-Type` headers in browser multipart requests.

### `productVariant` JSON Fields

| Field           | Type         | Required | Description                       |
|-----------------|--------------|----------|-----------------------------------|
| `productId`     | `Integer`    | ✅ Yes   | ID of the parent product          |
| `skuCode`       | `String`     | ✅ Yes   | Unique SKU code                   |
| `packSize`      | `String`     | ❌ No    | Pack size (e.g. `"250g"`)         |
| `uom`           | `String`     | ❌ No    | Unit of measure (e.g. `"grams"`)  |
| `containerType` | `String`     | ❌ No    | Container type (e.g. `"box"`)     |
| `mrp`           | `BigDecimal` | ❌ No    | Maximum retail price              |
| `sellingPrice`  | `BigDecimal` | ❌ No    | Selling price                     |
| `length`        | `BigDecimal` | ❌ No    | Length (cm)                       |
| `breadth`       | `BigDecimal` | ❌ No    | Breadth (cm)                      |
| `height`        | `BigDecimal` | ❌ No    | Height (cm)                       |
| `weight`        | `BigDecimal` | ❌ No    | Weight (kg)                       |

### Request Examples (curl)

```bash
# With images and video — second image (index 1) set as main
curl -X POST http://localhost:8080/api/products/productsVariant \
  -F 'productVariant={"productId":3,"skuCode":"KK-250G","packSize":"250g","mrp":399.00,"sellingPrice":350.00}' \
  -F 'images=@/path/to/image1.png' \
  -F 'images=@/path/to/image2.png' \
  -F 'video=@/path/to/product-demo.mp4' \
  -F 'mainImageIndex=1'

# Images only — first image as main (default)
curl -X POST http://localhost:8080/api/products/productsVariant \
  -F 'productVariant={"productId":3,"skuCode":"KK-250G","sellingPrice":350.00}' \
  -F 'images=@/path/to/image1.png'

# No files
curl -X POST http://localhost:8080/api/products/productsVariant \
  -F 'productVariant={"productId":3,"skuCode":"KK-500G","sellingPrice":650.00}'
```

### Response — `200 OK`

```json
{
  "variantId": 14,
  "skuCode": "KK-250G",
  "packSize": "250g",
  "uom": null,
  "containerType": null,
  "mrp": 399.00,
  "sellingPrice": 350.00,
  "status": "A",
  "productId": 3,
  "length": null,
  "breadth": null,
  "height": null,
  "weight": null,
  "videoUrl": "1717583245123_product-demo.mp4",
  "productImages": [
    {
      "id": 21,
      "image": "1717583245001_image1.png",
      "imagePath": "/app/uploads/1717583245001_image1.png",
      "isMainImage": "Y"
    },
    {
      "id": 22,
      "image": "1717583245002_image2.png",
      "imagePath": "/app/uploads/1717583245002_image2.png",
      "isMainImage": "N"
    }
  ],
  "attributes": null
}
```

### Response Field Reference

| Field           | Type      | Description |
|-----------------|-----------|-------------|
| `variantId`     | `Integer` | Created variant ID |
| `skuCode`       | `String`  | SKU code |
| `videoUrl`      | `String`  | Stored video filename (e.g. `"1717583245123_demo.mp4"`), or `null` |
| `productImages` | `Array`   | Each entry: `id`, `image` (filename), `imagePath`, `isMainImage` (`"Y"` / `"N"`) |
| `attributes`    | `Array`   | `null` on creation — add via `POST /productAttributes` |

### `mainImageIndex` Behaviour

| Scenario | Result |
|----------|--------|
| `mainImageIndex` not provided | Defaults to `0` — first uploaded image is marked `isMainImage = "Y"` |
| `mainImageIndex=1` with 3 images | Second image gets `"Y"`, all others get `"N"` |
| `mainImageIndex` out of range (e.g. `5` for 3 images) | Clamped to `0` — first image becomes main |
| No images uploaded | No `productImages` entries; `mainImageIndex` is ignored |

### File Storage

- Images and video are saved to the directory set by `category.image.upload-dir` in `application.properties`.
- Stored filename format: `{unix_timestamp}_{originalFilename}`.
- `videoUrl` stores the **filename only** (not the full path).

### Validation Rules

| File type | Allowed extensions              | Error if violated |
|-----------|---------------------------------|-------------------|
| Image     | `.png` only                     | `"Only PNG images are allowed."` |
| Video     | `.mp4`, `.mov`, `.avi`, `.webm` | `"Only MP4, MOV, AVI, WEBM video files are allowed."` |

---

## Update Product Variant

### `PUT /api/products/productsVariant`

Updates an existing product variant. Optionally uploads new images and/or a replacement video. Returns the full variant DTO including all images (existing + new) with `isMainImage` and attributes.

**Content-Type:** `multipart/form-data`

### Request Parts

| Part             | Annotation      | Type              | Required | Description |
|------------------|-----------------|-------------------|----------|-------------|
| `productVariant` | `@RequestPart`  | JSON string       | ✅ Yes   | Variant fields to update (only provided fields are changed) |
| `images`         | `@RequestParam` | `MultipartFile[]` | ❌ No    | New images to append. **Only `.png` allowed.** |
| `video`          | `@RequestParam` | `MultipartFile`   | ❌ No    | Replacement video. **Allowed: `.mp4`, `.mov`, `.avi`, `.webm`** |
| `mainImageIndex` | `@RequestParam` | `int`             | ❌ No    | 0-based index of which **new** image to mark as main. Defaults to `0`. When new images are provided, all existing images are reset to `"N"` and the specified new image is set to `"Y"`. |

### `productVariant` JSON Fields (all optional except `variantId`)

| Field           | Type         | Required | Description |
|-----------------|--------------|----------|-------------|
| `variantId`     | `Long`       | ✅ Yes   | ID of the variant to update |
| `skuCode`       | `String`     | ❌ No    | New SKU code |
| `packSize`      | `String`     | ❌ No    | New pack size |
| `uom`           | `String`     | ❌ No    | New unit of measure |
| `containerType` | `String`     | ❌ No    | New container type |
| `mrp`           | `BigDecimal` | ❌ No    | New MRP |
| `sellingPrice`  | `BigDecimal` | ❌ No    | New selling price |
| `status`        | `String`     | ❌ No    | `"A"` (active) or `"I"` (inactive) |
| `length`        | `BigDecimal` | ❌ No    | Length (cm) |
| `breadth`       | `BigDecimal` | ❌ No    | Breadth (cm) |
| `height`        | `BigDecimal` | ❌ No    | Height (cm) |
| `weight`        | `BigDecimal` | ❌ No    | Weight (kg) |

### Image Behaviour on Update

| Scenario | Result |
|----------|--------|
| No new images uploaded | Existing images unchanged; existing main image preserved |
| New images uploaded | Appended to existing. All existing images reset to `isMainImage = "N"`. New image at `mainImageIndex` gets `"Y"` |
| `mainImageIndex` out of range | Clamped to `0` — first new image becomes main |

### Request Examples (curl)

```bash
# Update variant fields only (no new files)
curl -X PUT http://localhost:8080/api/products/productsVariant \
  -F 'productVariant={"variantId":14,"sellingPrice":299.00,"status":"A"}'

# Update fields + add new images (second new image as main)
curl -X PUT http://localhost:8080/api/products/productsVariant \
  -F 'productVariant={"variantId":14,"sellingPrice":299.00}' \
  -F 'images=@/path/to/new_image1.png' \
  -F 'images=@/path/to/new_image2.png' \
  -F 'mainImageIndex=1'

# Replace video
curl -X PUT http://localhost:8080/api/products/productsVariant \
  -F 'productVariant={"variantId":14}' \
  -F 'video=@/path/to/new-demo.mp4'
```

### Response — `200 OK`

```json
{
  "variantId": 14,
  "skuCode": "KK-250G",
  "packSize": "250g",
  "mrp": 399.00,
  "sellingPrice": 299.00,
  "status": "A",
  "productId": 3,
  "videoUrl": "1717583245123_product-demo.mp4",
  "productImages": [
    {
      "id": 21,
      "image": "1717583245001_image1.png",
      "imagePath": "/app/uploads/1717583245001_image1.png",
      "isMainImage": "N"
    },
    {
      "id": 23,
      "image": "1717590000001_new_image1.png",
      "imagePath": "/app/uploads/1717590000001_new_image1.png",
      "isMainImage": "N"
    },
    {
      "id": 24,
      "image": "1717590000002_new_image2.png",
      "imagePath": "/app/uploads/1717590000002_new_image2.png",
      "isMainImage": "Y"
    }
  ],
  "attributes": [
    { "id": 5, "variantId": 14, "attributeName": "Weight", "attributeValue": "250g" }
  ]
}
```

---

## Delete Product Variant Video

### `DELETE /api/products/productsVariant/{variant_id}/video`

Deletes the video file from the filesystem and clears the `videoUrl` field for the specified product variant.

#### Path Parameter

| Parameter    | Type   | Description |
|--------------|--------|-------------|
| `variant_id` | `Long` | ID of the product variant whose video should be deleted |

#### Response — `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Video deleted successfully"
}
```

#### Error Responses

```json
{ "responseStatus": "FAILURE", "responseMessage": "Product variant not found with ID: 14" }
{ "responseStatus": "FAILURE", "responseMessage": "No video exists for variant ID: 14" }
```

#### curl Example

```bash
curl -X DELETE http://localhost:8080/api/products/productsVariant/14/video
```

#### Behaviour Notes

- The physical video file is deleted from the upload directory on the server.
- `videoUrl` is set to `null` in the database.
- If the file no longer exists on disk (already manually removed), the DB field is still cleared — no error is thrown.

---

## Get Product by Slug

### `GET /api/products/productSlug/{productSlug}`

Returns full product details for the given slug. The selected (cheapest) variant's `videoUrl` is included at the top level, and every entry in `productvarlist` also has its own `videoUrl`.

**Example:**
```
GET /api/products/productSlug/walnuts
```

### Response — `200 OK`

```json
{
  "id": 14,
  "title": "Premium Walnuts",
  "description": "Fresh whole walnuts, rich in omega-3",
  "slug": "walnuts",
  "category": "Dry Fruits & Nuts",
  "sku": "WN-250G",
  "price": 350.00,
  "mrp": 399.00,
  "currency": "INR",
  "mainImage": "1717583245001_walnut.png",
  "videoUrl": "1717583245123_walnut-demo.mp4",
  "stock": 50,
  "inStock": 1,
  "isReturnable": "N",
  "returnPolicy": null,
  "attributes": [
    { "id": 5, "variantId": 14, "attributeName": "Weight", "attributeValue": "250g" }
  ],
  "productvarlist": [
    {
      "id": 15,
      "title": "Premium Walnuts",
      "sku": "WN-500G",
      "price": 650.00,
      "mrp": 749.00,
      "currency": "INR",
      "mainImage": "1717583245900_walnut-500g.png",
      "videoUrl": "1717583245456_walnut-500g-demo.mp4",
      "stock": 30,
      "inStock": 1,
      "attributes": [
        { "id": 6, "variantId": 15, "attributeName": "Weight", "attributeValue": "500g" }
      ]
    }
  ]
}
```

### Response Field Reference — `ProductDTO`

| Field            | Type          | Description |
|------------------|---------------|-------------|
| `id`             | `Integer`     | Variant ID (cheapest variant selected as main) |
| `title`          | `String`      | Product name |
| `description`    | `String`      | Product description |
| `slug`           | `String`      | URL-friendly slug |
| `category`       | `String`      | Category name |
| `sku`            | `String`      | SKU of the selected variant |
| `price`          | `BigDecimal`  | Selling price |
| `mrp`            | `BigDecimal`  | MRP |
| `currency`       | `String`      | Always `"INR"` |
| `mainImage`      | `String`      | First image filename of the selected variant |
| `videoUrl`       | `String`      | Video filename of the selected variant, or `null` |
| `stock`          | `Integer`     | Available stock qty |
| `inStock`        | `Integer`     | `1` if stock > 0, else `0` |
| `isReturnable`   | `String`      | `"Y"` or `"N"` |
| `returnPolicy`   | `Object/null` | Return policy details, or `null` |
| `attributes`     | `Array`       | Attribute name-value pairs |
| `productvarlist` | `Array`       | Other variants — each a `ProductDTO` with its own `videoUrl`, `mainImage`, etc. |

---

## Get Variants by Product ID

### `GET /api/products/productsVariant/{product_id}`

Returns all active variants for a product. Each variant includes its full `productImages` list (with `isMainImage`) and `attributes`.

> **Auto-fix**: If no image for a variant has `isMainImage = "Y"`, the first image is automatically promoted and persisted as the main image.

**Example:**
```
GET /api/products/productsVariant/3
```

### Response — `200 OK`

```json
[
  {
    "variantId": 14,
    "skuCode": "KK-250G",
    "packSize": "250g",
    "mrp": 399.00,
    "sellingPrice": 350.00,
    "status": "A",
    "productId": 3,
    "videoUrl": "1717583245123_product-demo.mp4",
    "productImages": [
      {
        "id": 21,
        "image": "1717583245001_image1.png",
        "imagePath": "/app/uploads/1717583245001_image1.png",
        "isMainImage": "Y"
      },
      {
        "id": 22,
        "image": "1717583245002_image2.png",
        "imagePath": "/app/uploads/1717583245002_image2.png",
        "isMainImage": "N"
      }
    ],
    "attributes": [
      { "id": 5, "variantId": 14, "attributeName": "Weight", "attributeValue": "250g" }
    ]
  }
]
```

---

## Database Migration

```sql
ALTER TABLE product_variants ADD COLUMN video_url VARCHAR(500);
ALTER TABLE product_images ADD COLUMN is_main_image CHAR(1) DEFAULT 'N';
```

---

## Set Main Product Image

### `PUT /api/products/productImage/{image_id}/main`

Marks the specified image as the main image (`isMainImage = "Y"`) for its product variant and resets all other images of the same variant to `"N"`.

#### Path Parameter

| Parameter  | Type   | Description |
|------------|--------|-------------|
| `image_id` | `Long` | ID of the image to promote as main |

#### Response — `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Main image updated successfully"
}
```

#### Error Response (image not found)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Image not found"
}
```

#### curl Example

```bash
curl -X PUT http://localhost:8080/api/products/productImage/22/main
```

#### Behaviour Notes

- Atomically resets **all** images for the same variant to `isMainImage = "N"` before setting the target to `"Y"`, ensuring exactly one main image per variant at all times.
- The `image_id` refers to `ProductImageEO.id` (returned in `productImages[].id` on all variant responses).

---

## Get Product Images

### `GET /api/products/productImage/{productId}`

Returns all images for the given product variant ID.

#### Path Parameter

| Parameter   | Type     | Description |
|-------------|----------|-------------|
| `productId` | `String` | Variant ID |

#### Response — `200 OK`

```json
[
  {
    "id": 21,
    "image": "1717583245001_image1.png",
    "imagePath": "/app/uploads/1717583245001_image1.png",
    "isMainImage": "Y"
  },
  {
    "id": 22,
    "image": "1717583245002_image2.png",
    "imagePath": "/app/uploads/1717583245002_image2.png",
    "isMainImage": "N"
  }
]
```

---

## Delete Product Image

### `DELETE /api/products/productImage/{image_id}`

Permanently deletes a product image by its ID.

> **Auto-promote**: If the deleted image was the main image (`isMainImage = "Y"`), the first remaining image for the same variant is automatically promoted to main.

#### Path Parameter

| Parameter  | Type   | Description |
|------------|--------|-------------|
| `image_id` | `Long` | ID of the image to delete |

#### Response — `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Image deleted successfully"
}
```

#### Error Response (not found)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "Image not found with ID: 6"
}
```

#### curl Example

```bash
curl -X DELETE http://localhost:8080/api/products/productImage/6
```

---

## Search API

### `GET /api/products/search`

Search products by keyword and/or category IDs with price range, stock filter, sort, and pagination.

### Query Parameters

| Parameter    | Type      | Required | Default       | Description |
|--------------|-----------|----------|---------------|-------------|
| `query`      | `String`  | ❌ No    | `null`        | Keyword — matched against product name/description (case-insensitive, partial, with basic stemming e.g. `"sweets"` → `"sweet"`). Falls back to category name/description if no product match. |
| `categoryId` | `Long[]`  | ❌ No    | `null`        | Repeat for multiple: `categoryId=1&categoryId=2`. Combined with `query`: text search then in-memory filter to these categories. Pass `categoryId=0` to fetch products from **all active categories** (max 500 results). |
| `inStock`    | `Boolean` | ❌ No    | `null`        | `true` — stock ≥ 1 only. |
| `minPrice`   | `Integer` | ❌ No    | `null`        | Min selling price (inclusive). |
| `price`      | `Integer` | ❌ No    | `null`        | Max selling price (inclusive). |
| `sort`       | `String`  | ❌ No    | `defaultSort` | `lowPrice` or `highPrice`. |
| `page`       | `int`     | ❌ No    | `1`           | 1-based page number. |
| `limit`      | `int`     | ❌ No    | `5`           | Results per page. |
| `_rsc`       | `String`  | ❌ No    | `null`        | Next.js RSC token (ignored server-side). |

> At least one of `query` or `categoryId` must be provided. If neither, `[]` is returned.

### Search Logic

| Scenario | Behaviour |
|----------|-----------|
| `categoryId` + `query` | Native SQL text search → filter to given categories in-memory |
| `categoryId` only | Direct DB fetch by category IDs |
| `categoryId=0` (with or without `query`) | Fetch **all active products** (max 500), apply query/price/stock filters |
| `query` only | Text search on products → fallback to category name/description → fetch those products |
| Neither | Returns `[]` |

### Price Filter

| `minPrice` | `price` | Filter |
|------------|---------|--------|
| ✅ | ✅ | `minPrice ≤ sellingPrice ≤ price` |
| ❌ | ✅ | `sellingPrice ≤ price` |
| ✅ | ❌ | No filter |
| ❌ | ❌ | No filter |

> Products with no variants matching the price filter are excluded from results.

### Request Examples

```
GET /api/products/search?query=walnuts
GET /api/products/search?categoryId=2&categoryId=5
GET /api/products/search?query=cashew&categoryId=5
GET /api/products/search?query=organic&categoryId=4&inStock=true&minPrice=100&price=1000&sort=highPrice&page=1&limit=20
GET /api/products/search?query=snacks&page=2&limit=10
```

### Response — `200 OK`

```json
[
  {
    "id": 12,
    "title": "Kaju Katli",
    "description": "Premium cashew sweet",
    "slug": "kaju-katli",
    "category": "Sweets & Savouries",
    "sku": "KK-250G",
    "price": 350.00,
    "mrp": 399.00,
    "currency": "INR",
    "mainImage": "1716123456789_kaju.png",
    "videoUrl": null,
    "stock": 25,
    "inStock": 1,
    "isReturnable": "N",
    "returnPolicy": null,
    "attributes": [
      { "id": 5, "variantId": 12, "attributeName": "Weight", "attributeValue": "250g" }
    ],
    "productvarlist": []
  }
]
```

### Response Field Reference

| Field            | Type          | Description |
|------------------|---------------|-------------|
| `id`             | `Integer`     | Variant ID (cheapest matching variant) |
| `title`          | `String`      | Product name |
| `description`    | `String`      | Product description |
| `slug`           | `String`      | URL-friendly slug |
| `category`       | `String`      | Category name |
| `sku`            | `String`      | SKU of selected variant |
| `price`          | `BigDecimal`  | Selling price |
| `mrp`            | `BigDecimal`  | MRP |
| `currency`       | `String`      | Always `"INR"` |
| `mainImage`      | `String`      | First image filename |
| `videoUrl`       | `String`      | Video filename, or `null` |
| `stock`          | `Integer`     | Available qty |
| `inStock`        | `Integer`     | `1` if stock > 0, else `0` |
| `isReturnable`   | `String`      | `"Y"` or `"N"` |
| `returnPolicy`   | `Object/null` | Return policy or `null` |
| `attributes`     | `Array`       | Attribute name-value pairs |
| `productvarlist` | `Array`       | Empty in search — populated in slug endpoint only |

### Error Responses

| HTTP Status | Scenario |
|-------------|----------|
| `200 []`    | No products matched |
| `200 []`    | `page` exceeds total results |
| `200 []`    | Neither `query` nor `categoryId` provided |

---

## Shop by Category API

### `GET /api/products/shop/{category}`

Fetch products by exact category name (path variable).

| Parameter  | Type      | Required | Default       | Description |
|------------|-----------|----------|---------------|-------------|
| `category` | `String`  | ✅ Yes   | —             | Exact category name (path variable) |
| `inStock`  | `Boolean` | ❌ No    | `null`        | Filter by stock availability |
| `price`    | `Integer` | ❌ No    | `null`        | Max price only (no `minPrice`) |
| `sort`     | `String`  | ❌ No    | `defaultSort` | `lowPrice` or `highPrice` |
| `page`     | `int`     | ❌ No    | `1`           | Page number |

**Example:**
```
GET /api/products/shop/Dairy?inStock=true&price=500&sort=lowPrice&page=1
```

---

## Shop vs Search — Comparison

| Feature | `GET /shop/{category}` | `GET /search` |
|---|---|---|
| **Category input** | Path variable — exact name (`/shop/Dairy`) | Query param IDs — `categoryId=1&categoryId=2` |
| **Category matching** | Exact string match only | By numeric ID (no typo risk) |
| **Multiple categories** | ❌ Single category only | ✅ Repeat param: `categoryId=1&categoryId=2` |
| **Keyword search (`query`)** | ❌ Not supported | ✅ Searches product name, description; falls back to category name/description |
| **Stemming** | ❌ | ✅ e.g. `"sweets"` also matches `"sweet"` |
| **`minPrice`** | ❌ Not supported | ✅ `minPrice` param |
| **`maxPrice` (`price`)** | ✅ | ✅ |
| **`inStock` filter** | ✅ | ✅ |
| **Sort** | ✅ `lowPrice` / `highPrice` | ✅ `lowPrice` / `highPrice` |
| **`limit` (page size)** | ❌ No limit param — returns all | ✅ `limit` param (default `5`) |
| **Fallback logic** | ❌ None | ✅ Falls back to category name/description search if no product text match |
| **Typical use case** | Browse page for a known category by name | Search bar, filter panel, multi-category browsing |

**When to use which:**
- Use **`/shop`** when navigating to a specific category page where you already know the exact category name (e.g. from a nav link).
- Use **`/search`** for the search bar, filter sidebar, or any scenario needing keyword search, price range, multiple categories, or controlled pagination.

---

## Unit of Measure (UOM) APIs

### Data Model — `unit_of_measure`

| Field            | Type       | Description |
|------------------|------------|-------------|
| `uomId`          | `Long`     | Auto-generated primary key |
| `uomCode`        | `String`   | Short unique code, e.g. `"KG"`, `"PCS"`, `"MTR"` |
| `uomName`        | `String`   | Full name, e.g. `"Kilogram"`, `"Pieces"` |
| `uomType`        | `String`   | Category/dimension, e.g. `"WEIGHT"`, `"COUNT"`, `"VOLUME"`, `"LENGTH"` |
| `baseUomFlag`    | `String`   | `"Y"` if this is the base unit for its type; `"N"` otherwise (default `"N"`) |
| `decimalAllowed` | `String`   | `"Y"` if fractional quantities allowed; `"N"` for whole-number only (default `"N"`) |
| `status`         | `String`   | `"ACTIVE"` (default) or `"INACTIVE"` |
| `description`    | `String`   | Optional free-text description |
| `createdAt`      | `DateTime` | Auto-set on creation |
| `createdBy`      | `String`   | User who created the record |
| `updatedAt`      | `DateTime` | Auto-set on update |
| `updatedBy`      | `String`   | User who last updated the record |

---

### `POST /api/products/uom` — Create Unit of Measure

**Content-Type:** `application/json`

#### Request Body

```json
{
  "uomCode": "KG",
  "uomName": "Kilogram",
  "uomType": "WEIGHT",
  "baseUomFlag": "Y",
  "decimalAllowed": "Y",
  "description": "Standard weight unit",
  "createdBy": "admin"
}
```

#### Request Fields

| Field            | Type     | Required | Description |
|------------------|----------|----------|-------------|
| `uomCode`        | `String` | ✅ Yes   | Unique code (max 20 chars). Duplicate active codes are rejected. |
| `uomName`        | `String` | ✅ Yes   | Descriptive name (max 100 chars) |
| `uomType`        | `String` | ✅ Yes   | Dimension category (max 50 chars) |
| `baseUomFlag`    | `String` | ❌ No    | `"Y"` or `"N"`. Defaults to `"N"` |
| `decimalAllowed` | `String` | ❌ No    | `"Y"` or `"N"`. Defaults to `"N"` |
| `description`    | `String` | ❌ No    | Free-text description (max 255 chars) |
| `createdBy`      | `String` | ❌ No    | Audit — who created this record |

#### Response — `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Unit of Measure created successfully"
}
```

#### Error Response (duplicate code)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "UOM with code 'KG' already exists"
}
```

---

### `PUT /api/products/uom/{uom_id}` — Update Unit of Measure

**Content-Type:** `application/json`

#### Path Parameter

| Parameter | Type   | Description |
|-----------|--------|-------------|
| `uom_id`  | `Long` | ID of the UOM to update |

#### Request Body (all fields optional — only provided fields are updated)

```json
{
  "uomCode": "KG",
  "uomName": "Kilogram",
  "uomType": "WEIGHT",
  "baseUomFlag": "Y",
  "decimalAllowed": "Y",
  "status": "ACTIVE",
  "description": "Updated description",
  "updatedBy": "admin"
}
```

#### Request Fields

| Field            | Type     | Required | Description |
|------------------|----------|----------|-------------|
| `uomCode`        | `String` | ❌ No    | New unique code |
| `uomName`        | `String` | ❌ No    | New name |
| `uomType`        | `String` | ❌ No    | New type/dimension |
| `baseUomFlag`    | `String` | ❌ No    | `"Y"` or `"N"` |
| `decimalAllowed` | `String` | ❌ No    | `"Y"` or `"N"` |
| `status`         | `String` | ❌ No    | `"ACTIVE"` or `"INACTIVE"` |
| `description`    | `String` | ❌ No    | Updated description |
| `updatedBy`      | `String` | ❌ No    | Audit — who updated this record |

#### Response — `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Unit of Measure updated successfully"
}
```

#### Error Response (not found)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "UOM not found or inactive"
}
```

---

### `DELETE /api/products/uom/{uom_id}` — Delete Unit of Measure

Performs a **soft delete** — sets `status` to `"INACTIVE"`. The record is not physically removed.

#### Path Parameter

| Parameter | Type   | Description |
|-----------|--------|-------------|
| `uom_id`  | `Long` | ID of the UOM to delete |

#### Response — `200 OK`

```json
{
  "responseStatus": "SUCCESS",
  "responseMessage": "Unit of Measure deleted successfully"
}
```

#### Error Response (not found or already inactive)

```json
{
  "responseStatus": "FAILURE",
  "responseMessage": "UOM not found or already inactive"
}
```

---

### `GET /api/products/uom/{uom_id}` — Get UOM by ID

Returns a single active UOM by its ID.

#### Path Parameter

| Parameter | Type   | Description |
|-----------|--------|-------------|
| `uom_id`  | `Long` | ID of the UOM |

#### Response — `200 OK`

```json
{
  "uomId": 1,
  "uomCode": "KG",
  "uomName": "Kilogram",
  "uomType": "WEIGHT",
  "baseUomFlag": "Y",
  "decimalAllowed": "Y",
  "status": "ACTIVE",
  "description": "Standard weight unit",
  "createdAt": "2026-06-12T10:00:00",
  "createdBy": "admin",
  "updatedAt": null,
  "updatedBy": null
}
```

#### Response — `404 Not Found`

Returned when no active UOM exists for the given ID.

---

### `GET /api/products/uom` — Get All Active UOMs

Returns a list of all UOMs with `status = "ACTIVE"`.

#### Response — `200 OK`

```json
[
  {
    "uomId": 1,
    "uomCode": "KG",
    "uomName": "Kilogram",
    "uomType": "WEIGHT",
    "baseUomFlag": "Y",
    "decimalAllowed": "Y",
    "status": "ACTIVE",
    "description": "Standard weight unit",
    "createdAt": "2026-06-12T10:00:00",
    "createdBy": "admin",
    "updatedAt": null,
    "updatedBy": null
  },
  {
    "uomId": 2,
    "uomCode": "PCS",
    "uomName": "Pieces",
    "uomType": "COUNT",
    "baseUomFlag": "Y",
    "decimalAllowed": "N",
    "status": "ACTIVE",
    "description": "Countable unit",
    "createdAt": "2026-06-12T10:05:00",
    "createdBy": "admin",
    "updatedAt": null,
    "updatedBy": null
  }
]
```

Returns `[]` if no active UOMs exist.

---

### UOM Request/Response Examples (curl)

```bash
# Create
curl -X POST http://localhost:8080/api/products/uom \
  -H "Content-Type: application/json" \
  -d '{"uomCode":"KG","uomName":"Kilogram","uomType":"WEIGHT","baseUomFlag":"Y","decimalAllowed":"Y","createdBy":"admin"}'

# Update
curl -X PUT http://localhost:8080/api/products/uom/1 \
  -H "Content-Type: application/json" \
  -d '{"uomName":"Kilogram (updated)","updatedBy":"admin"}'

# Delete (soft)
curl -X DELETE http://localhost:8080/api/products/uom/1

# Get by ID
curl http://localhost:8080/api/products/uom/1

# Get all active
curl http://localhost:8080/api/products/uom
```

---

### Common UOM Type Values

| `uomType`  | Example Codes      | Description |
|------------|--------------------|-------------|
| `WEIGHT`   | `KG`, `G`, `MG`   | Weight-based measurements |
| `VOLUME`   | `L`, `ML`          | Volume-based measurements |
| `LENGTH`   | `MTR`, `CM`, `MM`  | Length-based measurements |
| `COUNT`    | `PCS`, `DOZEN`     | Countable units |
| `PACK`     | `BOX`, `BAG`       | Packaging units |

---

## Label Print API

### `POST /api/products/labels/print`

Generates a thermal-printer-ready PDF containing one **2" × 2" (144 × 144 pt) label per page**.  
Each label is built from live `InventoryDetails` records and includes a CODE-128 barcode.

**Content-Type:** `application/json`

---

### Request Body

```json
{
  "brandName": "FreshFarms",
  "batchNo":   "BATCH-2026-06-A"
}
```

or using barcode values instead of a batch number:

```json
{
  "brandName": "FreshFarms",
  "barcodes":  "BC001,BC002,BC003"
}
```

---

### Request Fields

| Field       | Type     | Required | Description |
|-------------|----------|----------|-------------|
| `brandName` | `String` | ✅ Yes   | Brand name printed on **every** label. |
| `batchNo`   | `String` | ❌ No*   | Batch / lot number. **Takes first preference** — all `InventoryDetails` rows matching this batch are used. |
| `barcodes`  | `String` | ❌ No*   | Comma-separated barcode values (e.g. `"BC001,BC002"`). Used **only when `batchNo` is absent**. |

> \* At least one of `batchNo` **or** `barcodes` must be provided.

---

### Item Resolution Logic

| Scenario | Behaviour |
|----------|-----------|
| `batchNo` provided | Fetches all `InventoryDetails` rows where `batchNo` matches. One label is generated per row. |
| `batchNo` absent, `barcodes` provided | Splits the comma-separated string and bulk-fetches matching `InventoryDetails` rows. |
| Both provided | `batchNo` wins — `barcodes` is ignored. |
| No match found | Returns `FAILURE` with a descriptive message; no PDF is generated. |

---

### Label Content

Each label prints the following fields (in top-to-bottom order) followed by a separator line and a CODE-128 barcode:

| Label Field      | Source |
|------------------|--------|
| **Brand Name**   | `request.brandName` |
| **Product Name** | `ProductEO.name` via `ProductVariant → Product` |
| **Variant Details** | `packSize` + `uom` + `containerType` (space-joined, non-null parts only; `"N/A"` if all empty) |
| **Net Quantity** | `packSize` + `uom` (e.g. `"250 g"`; `"N/A"` if none) |
| **MFG Date**     | `InventoryDetails.mfd` formatted as `MM/yyyy` (e.g. `"01/2026"`); `"N/A"` if null |
| **EXP Date**     | `InventoryDetails.expiryDate` formatted as `MM/yyyy`; `"N/A"` if null |
| **Batch/Lot No** | `InventoryDetails.batchNo` |
| **MRP**          | `"Rs."` + `ProductVariant.mrp` (e.g. `"Rs.399.00"`); `"N/A"` if null |
| **Barcode**      | `InventoryDetails.barcode` — CODE-128 image + text below |

> Long text values are automatically truncated with `..` if they overflow the label width.  
> Non-Latin-1 characters are stripped (PDFBox Type1 font limitation).

---

### PDF Layout

| Property         | Value |
|------------------|-------|
| Format           | PDF |
| Page size        | **144 × 144 pt** (2" × 2") — one page per label |
| Label border     | 0.5 pt black rectangle |
| Font             | Helvetica (bold for field keys, regular for values) |
| Font size        | 5.5 pt |
| Barcode type     | CODE-128 |
| Barcode height   | 22 pt (~7.8 mm) |
| Printer target   | Direct-thermal roll printer |

---

### Response — `200 OK` (success)

```json
{
  "status":     "SUCCESS",
  "message":    "Label PDF generated successfully",
  "pdfUrl":     "/labels/labels_1718000000000.pdf",
  "labelCount": 3
}
```

### Response — `200 OK` (failure)

```json
{
  "status":     "FAILURE",
  "message":    "No inventory details found for Batch No: BATCH-2026-06-A",
  "pdfUrl":     null,
  "labelCount": 0
}
```

---

### Response Fields

| Field        | Type      | Description |
|--------------|-----------|-------------|
| `status`     | `String`  | `"SUCCESS"` or `"FAILURE"` |
| `message`    | `String`  | Human-readable result or error detail |
| `pdfUrl`     | `String`  | Relative path to the generated PDF, e.g. `"/labels/labels_1718000000000.pdf"`. `null` on failure. |
| `labelCount` | `int`     | Number of individual labels included in the PDF. `0` on failure. |

---

### Validation Errors

| Condition | `message` |
|-----------|-----------|
| `brandName` missing or blank | `"Brand Name is mandatory"` |
| Both `batchNo` and `barcodes` absent/blank | `"Either Batch No or at least one Barcode value must be provided"` |
| No `InventoryDetails` found for `batchNo` | `"No inventory details found for Batch No: {batchNo}"` |
| No `InventoryDetails` found for any barcode | `"No inventory details found for provided barcode(s)"` |
| PDF generation exception | `"Failed to generate label PDF: {exceptionMessage}"` |

---

### File Storage

- Generated PDFs are saved to the directory configured by `label.pdf.dir` in `application.properties` (default: `/public/labels/`).
- Filename format: `labels_{unix_timestamp_ms}.pdf` (e.g. `labels_1718000000000.pdf`).
- The `pdfUrl` field contains the relative path starting from `/labels/`.

---

### curl Examples

```bash
# By batch number
curl -X POST http://localhost:8080/api/products/labels/print \
  -H "Content-Type: application/json" \
  -d '{"brandName":"FreshFarms","batchNo":"BATCH-2026-06-A"}'

# By individual barcodes
curl -X POST http://localhost:8080/api/products/labels/print \
  -H "Content-Type: application/json" \
  -d '{"brandName":"FreshFarms","barcodes":"BC001,BC002,BC003"}'
```

---

### application.properties Key

```properties
# Directory where generated label PDFs are stored
label.pdf.dir=/public/labels/
```

---

## Label Print Job History & Reprint APIs

Every call to `POST /api/products/labels/print` — whether successful or failed — is persisted
in the `label_print_jobs` table.  Use the APIs below to browse history, re-download existing
PDFs, and reprint (regenerate) a job.

---

### `GET /api/products/labels/jobs`

Returns a paginated list of label print jobs, newest first.

| Query Param | Type     | Required | Description |
|-------------|----------|----------|-------------|
| `batchNo`   | `String` | No       | Filter by original batch number |
| `barcode`   | `String` | No       | Filter by a single barcode value (searches inside resolved barcodes) |
| `page`      | `int`    | No       | 1-based page number (default `1`) |
| `limit`     | `int`    | No       | Page size (default `20`) |

> Only one of `batchNo` / `barcode` is applied at a time. `batchNo` takes precedence.

#### Example

```bash
# All jobs, page 1
curl http://localhost:8080/api/products/labels/jobs

# Filter by batch number
curl "http://localhost:8080/api/products/labels/jobs?batchNo=BATCH-2026-06-A"

# Filter by barcode
curl "http://localhost:8080/api/products/labels/jobs?barcode=BC001"
```

#### Response (array of `LabelPrintJobDTO`)

```json
[
  {
    "jobId": 12,
    "brandName": "FreshFarms",
    "batchNo": "BATCH-2026-06-A",
    "barcodes": null,
    "resolvedBarcodes": "BC001,BC002,BC003",
    "labelCount": 3,
    "pdfUrl": "/labels/labels_1718000000000.pdf",
    "pdfFileExists": true,
    "status": "SUCCESS",
    "errorMessage": null,
    "printedAt": "2026-06-12T10:30:00+05:30",
    "printedBy": "SYSTEM"
  }
]
```

#### `LabelPrintJobDTO` Fields

| Field             | Type      | Description |
|-------------------|-----------|-------------|
| `jobId`           | `Long`    | Unique job ID (use this for reprint) |
| `brandName`       | `String`  | Brand name used in this print job |
| `batchNo`         | `String`  | Batch number supplied in original request (`null` if barcode-based) |
| `barcodes`        | `String`  | Comma-separated barcodes from original request (`null` if batch-based) |
| `resolvedBarcodes`| `String`  | All barcode values actually printed, comma-separated |
| `labelCount`      | `int`     | Number of labels in the generated PDF |
| `pdfUrl`          | `String`  | Relative URL to the PDF file (for direct download) |
| `pdfFileExists`   | `boolean` | `true` if the PDF file still exists on disk |
| `status`          | `String`  | `"SUCCESS"` or `"FAILURE"` |
| `errorMessage`    | `String`  | Error detail when `status = "FAILURE"` |
| `printedAt`       | `OffsetDateTime` | Timestamp of the job |
| `printedBy`       | `String`  | User/system that triggered the job |

---

### `GET /api/products/labels/jobs/{jobId}`

Returns a single label print job by its ID.  Use `pdfUrl` to re-download the PDF directly
(the file must still exist on disk — check `pdfFileExists`).

#### Path Variables

| Variable | Type   | Description         |
|----------|--------|---------------------|
| `jobId`  | `Long` | The job ID to fetch |

#### Example

```bash
curl http://localhost:8080/api/products/labels/jobs/12
```

#### Response

Same `LabelPrintJobDTO` structure as above.  Returns HTTP 404 with `{"error": "..."}` if not found.

---

### `POST /api/products/labels/jobs/{jobId}/reprint`

Regenerates the label PDF for an existing job using its stored `brandName` + `batchNo`/`barcodes`.
A **new** job record is created for the reprint (old record is preserved).

#### Path Variables

| Variable | Type   | Description             |
|----------|--------|-------------------------|
| `jobId`  | `Long` | The original job ID to reprint |

#### Example

```bash
curl -X POST http://localhost:8080/api/products/labels/jobs/12/reprint
```

#### Response — `LabelPrintResponseDTO`

```json
{
  "status": "SUCCESS",
  "message": "Label PDF generated successfully",
  "pdfUrl": "/labels/labels_1718099000000.pdf",
  "labelCount": 3
}
```

Returns the same structure as `POST /api/products/labels/print`.

---

### Re-download an existing PDF

If the PDF file still exists on disk (`pdfFileExists: true`), use the `pdfUrl` from any
`LabelPrintJobDTO` to download it directly via the static-file route:

```
GET http://localhost:8080{pdfUrl}
# e.g. GET http://localhost:8080/labels/labels_1718000000000.pdf
```

If the file no longer exists, use the **reprint** endpoint to regenerate it.

---

### DB Table: `label_print_jobs`

The table is auto-created by Hibernate (`spring.jpa.hibernate.ddl-auto`).  
Reference DDL (for manual creation):

```sql
CREATE TABLE label_print_jobs (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    brand_name       VARCHAR(255)  NOT NULL,
    batch_no         VARCHAR(100),
    barcodes         VARCHAR(2000),
    resolved_barcodes VARCHAR(4000),
    label_count      INT           NOT NULL DEFAULT 0,
    pdf_url          VARCHAR(500),
    pdf_file_path    VARCHAR(1000),
    status           VARCHAR(20)   NOT NULL,
    error_message    VARCHAR(1000),
    printed_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    printed_by       VARCHAR(100)
);
CREATE INDEX idx_label_jobs_batch  ON label_print_jobs (batch_no);
CREATE INDEX idx_label_jobs_status ON label_print_jobs (status);
CREATE INDEX idx_label_jobs_printed_at ON label_print_jobs (printed_at DESC);
```

---

## PDF Resize for Thermal Label API

### `POST /api/products/labels/resize-pdf`

Downloads an external PDF from any HTTP/HTTPS URL, scales every page to fit a
**4" × 6" (288 × 432 pt)** thermal label while preserving the original aspect ratio,
saves the resized PDF to the public folder, and returns the new PDF URL.

**Content-Type:** `application/json`

---

### When to Use

| Scenario | Recommended Endpoint |
|---|---|
| Generate labels from inventory data (batchNo / barcodes) | `POST /api/products/labels/print` |
| Resize an **existing PDF** (e.g. Shiprocket shipping label) to thermal 4"×6" | `POST /api/products/labels/resize-pdf` |

---

### Request Body

```json
{
  "pdfUrl": "https://sr-core-cdn.shiprocket.in/label/s/10008561/019eb10e-391e-7b3f-a08f-092de1165b20.pdf"
}
```

### Request Fields

| Field    | Type     | Required | Description |
|----------|----------|----------|-------------|
| `pdfUrl` | `String` | ✅ Yes   | Full HTTP/HTTPS URL of the PDF to download and resize. Redirects are followed automatically. |

---

### How Resizing Works

1. **Download** — The PDF is fetched over HTTP/HTTPS (15 s connect timeout, 60 s read timeout, redirect-following enabled).
2. **Scale-to-fit per page** — For each page in the source PDF:
   - Target page size: **288 × 432 pt** (4" × 6" at 72 dpi).
   - Scale factor = `min(288 / srcWidth, 432 / srcHeight)` — always ≤ 1 for typical label sizes.
   - The scaled content is **centered** horizontally and vertically on the target page.
   - Aspect ratio of the original content is fully preserved (no stretching).
3. **Save** — The resized PDF is written to the `label.pdf.dir` folder and served via the static file route.

---

### Target Page Specification

| Property       | Value |
|----------------|-------|
| Width          | **288 pt** (4 inches) |
| Height         | **432 pt** (6 inches) |
| Scaling        | Scale-to-fit, centred, aspect-ratio preserved |
| Pages          | All pages in the source PDF are resized |
| Font rendering | Fully vector-based — no rasterisation; fonts remain crisp at any print DPI |

---

### Response — `200 OK` (success)

```json
{
  "status":    "SUCCESS",
  "message":   "PDF resized successfully to 4\"×6\" thermal label format",
  "pdfUrl":    "/labels/resized_1718000000000.pdf",
  "pageCount": 1
}
```

### Response — `200 OK` (failure)

```json
{
  "status":    "FAILURE",
  "message":   "Failed to resize PDF: Connection refused",
  "pdfUrl":    null,
  "pageCount": 0
}
```

---

### Response Fields

| Field       | Type     | Description |
|-------------|----------|-------------|
| `status`    | `String` | `"SUCCESS"` or `"FAILURE"` |
| `message`   | `String` | Human-readable result or error detail |
| `pdfUrl`    | `String` | Relative URL to the resized PDF (e.g. `"/labels/resized_1718000000000.pdf"`). `null` on failure. |
| `pageCount` | `int`    | Number of pages in the resized PDF. `0` on failure. |

---

### Validation / Error Conditions

| Condition | `status` | `message` |
|-----------|----------|-----------|
| `pdfUrl` missing or blank | `FAILURE` | `"pdfUrl is required"` |
| URL unreachable / connection refused | `FAILURE` | `"Failed to resize PDF: {exception detail}"` |
| URL returns a non-PDF or corrupt file | `FAILURE` | `"Failed to resize PDF: {exception detail}"` |
| Source PDF has zero pages | `SUCCESS` | `pageCount: 0` — empty PDF is written |

---

### File Storage

- Resized PDFs are saved to the directory configured by `label.pdf.dir` (default: `/public/labels/`).
- Filename format: `resized_{unix_timestamp_ms}.pdf` (e.g. `resized_1718000000000.pdf`).
- The `pdfUrl` field contains the relative path starting from `/labels/`.

---

### curl Examples

```bash
# Resize a Shiprocket label PDF
curl -X POST http://localhost:8080/api/products/labels/resize-pdf \
  -H "Content-Type: application/json" \
  -d '{"pdfUrl":"https://sr-core-cdn.shiprocket.in/label/s/10008561/019eb10e-391e-7b3f-a08f-092de1165b20.pdf"}'

# Resize any other hosted PDF
curl -X POST http://localhost:8080/api/products/labels/resize-pdf \
  -H "Content-Type: application/json" \
  -d '{"pdfUrl":"https://example.com/my-label.pdf"}'
```

---

### Download the Resized PDF

Once the resize is successful, use the `pdfUrl` from the response to download the file
via the static-file route:

```
GET http://localhost:8080{pdfUrl}
# e.g. GET http://localhost:8080/labels/resized_1718000000000.pdf
```

---

### application.properties Key

```properties
# Directory where resized label PDFs are stored (same folder as generated labels)
label.pdf.dir=/public/labels/
```

---
## Label Configuration (MASTER) APIs
These APIs manage the **label_config** master table. A label configuration controls:
- Label page dimensions (width � height in inches)
- Which data fields appear on each label (brand, product, variant, MRP, dates, barcode, etc.)
- Whether a logo is printed at the top of each label
When calling POST /api/products/labels/print, include "labelConfigId" in the request body to apply a specific configuration. If omitted, the system auto-selects the config flagged as isDefault = true.
---
### 36. Create Label Configuration � POST /api/products/labels/config
#### Request Body
`json
{
  "configName": "2x2 Standard",
  "description": "Default 2-inch square label for all products",
  "labelWidthInches": 2.0,
  "labelHeightInches": 2.0,
  "showLogo": false,
  "logoPath": null,
  "showBrandName": true,
  "showProductName": true,
  "showVariantDetails": true,
  "showNetQuantity": true,
  "showMfgDate": true,
  "showExpDate": true,
  "showBatchNo": true,
  "showMrp": true,
  "showBarcode": true,
  "fssaiCode": "10014011000015",
  "showFssaiCode": true,
  "columnsPerRow": 1,
  "isDefault": true,
  "status": "ACTIVE",
  "createdBy": "admin"
}
`
| Field               | Type    | Required | Description |
|---------------------|---------|----------|-------------|
| configName          | String  | **Yes**  | Unique name (e.g. "2x2 Standard", "4x4 Thermal") |
| labelWidthInches    | Float   | **Yes**  | Label width in inches. 72 pt = 1 inch. |
| labelHeightInches   | Float   | **Yes**  | Label height in inches. |
| showLogo            | Boolean | No       | Print logo at top of label. Default: false. When true and `logoPath` is null/empty, falls back to the default company logo at `/public/companyLogo/CompanyLogo.png`. |
| logoPath            | String  | No       | Filesystem path to logo image (PNG/JPG). If blank, the default company logo is used whenever `showLogo = true`. |
| showBrandName       | Boolean | No       | Default: true |
| showProductName     | Boolean | No       | Default: true |
| showVariantDetails  | Boolean | No       | Default: true |
| showNetQuantity     | Boolean | No       | Default: true |
| showMfgDate         | Boolean | No       | Default: true |
| showExpDate         | Boolean | No       | Default: true |
| showBatchNo         | Boolean | No       | Default: true |
| showMrp             | Boolean | No       | Default: true |
| showBarcode         | Boolean | No       | Default: true (CODE-128 barcode image + text) |
| fssaiCode           | String  | No       | FSSAI licence number to print on the label (e.g. "10014011000015"). Stored at config level; rendered on every label produced with this config. |
| showFssaiCode       | Boolean | No       | Whether to print the FSSAI line. Default: true. Rendered after MRP, before the barcode. Hidden when false **or** when `fssaiCode` is blank/null. |
| columnsPerRow       | Integer | No       | Number of fields to place on each row of the label. `1` = single column (default), `2` = two fields per row, `3` = three, etc. Defaults to `1` if omitted or ≤ 0. |
| isDefault           | Boolean | No       | When true, this config becomes the system default. Any previously-default config is automatically demoted. |
| status              | String  | No       | ACTIVE or INACTIVE. Default: ACTIVE. |
| createdBy           | String  | No       | Identifier for audit logging. |
#### Response � 200 OK
`json
{
  "id": 1,
  "configName": "2x2 Standard",
  "description": "Default 2-inch square label for all products",
  "labelWidthInches": 2.0,
  "labelHeightInches": 2.0,
  "showLogo": false,
  "logoPath": null,
  "showBrandName": true,
  "showProductName": true,
  "showVariantDetails": true,
  "showNetQuantity": true,
  "showMfgDate": true,
  "showExpDate": true,
  "showBatchNo": true,
  "showMrp": true,
  "showBarcode": true,
  "fssaiCode": "10014011000015",
  "showFssaiCode": true,
  "columnsPerRow": 1,
  "isDefault": true,
  "status": "ACTIVE",
  "createdAt": "2026-06-15T10:00:00",
  "createdBy": "admin",
  "updatedAt": null,
  "updatedBy": null
}
`
#### Error Response � 400 Bad Request
`json
{ "error": "A label configuration with name '2x2 Standard' already exists" }
`
---
### 37. Update Label Configuration � PUT /api/products/labels/config/{configId}
Send only the fields you want to change (all fields are optional in update).
`json
{
  "labelWidthInches": 4.0,
  "labelHeightInches": 4.0,
  "showLogo": true,
  "logoPath": "/var/app/public/logo.png",
  "fssaiCode": "10014011000015",
  "showFssaiCode": true,
  "columnsPerRow": 2,
  "isDefault": false,
  "updatedBy": "admin"
}
`
All fields are optional. Only the fields you include will be updated.  
Returns the updated `LabelConfigResponseDTO` (same shape as create response).

> **`columnsPerRow`**: patch with `2` to display two fields per row, `3` for three columns, etc. Only applied if the value is > 0.
> **`fssaiCode`**: patch with the new licence number, or omit to leave the existing value unchanged.
> **`showFssaiCode`**: patch with `false` to hide the FSSAI line on this config's labels.
---
### 38. Delete Label Configuration � DELETE /api/products/labels/config/{configId}
Soft-deletes the config by setting status = INACTIVE. Active print jobs that reference this config are unaffected.
#### Response � 200 OK
`json
{
  "status": "SUCCESS",
  "message": "Label configuration '2x2 Standard' deactivated successfully"
}
`
---
### 39. Get Label Configuration by ID � GET /api/products/labels/config/{configId}
#### Response � 200 OK
Full LabelConfigResponseDTO object (same shape as create response).
#### Response � 404 Not Found
`json
{ "error": "Label configuration not found for ID: 99" }
`
---
### 40. List All Label Configurations � GET /api/products/labels/config
Optional query parameter:
| Param  | Values           | Description |
|--------|------------------|-------------|
| status | ACTIVE, INACTIVE | Filter by status. Omit to get all. |
**Examples:**
`
GET /api/products/labels/config              ? all configs
GET /api/products/labels/config?status=ACTIVE ? only active configs
`
#### Response � 200 OK
`json
[
  {
    "id": 1,
    "configName": "2x2 Standard",
    "labelWidthInches": 2.0,
    "labelHeightInches": 2.0,
    "showLogo": false,
    "showBrandName": true,
    "showProductName": true,
    "showVariantDetails": true,
    "showNetQuantity": true,
    "showMfgDate": true,
    "showExpDate": true,
    "showBatchNo": true,
    "showMrp": true,
    "showBarcode": true,
    "fssaiCode": "10014011000015",
    "showFssaiCode": true,
    "columnsPerRow": 1,
    "isDefault": true,
    "status": "ACTIVE",
    "createdAt": "2026-06-15T10:00:00",
    "createdBy": "admin"
  },
  {
    "id": 2,
    "configName": "4x4 No-Logo",
    "labelWidthInches": 4.0,
    "labelHeightInches": 4.0,
    "showLogo": false,
    "showBrandName": true,
    "showProductName": true,
    "showVariantDetails": false,
    "showNetQuantity": true,
    "showMfgDate": false,
    "showExpDate": false,
    "showBatchNo": true,
    "showMrp": true,
    "showBarcode": true,
    "fssaiCode": null,
    "showFssaiCode": false,
    "columnsPerRow": 2,
    "isDefault": false,
    "status": "ACTIVE",
    "createdAt": "2026-06-15T11:00:00",
    "createdBy": "admin"
  }
]
`
---
### 41. Set Default Label Configuration � PUT /api/products/labels/config/{configId}/set-default
Marks the specified config as the system default. Any other config that was previously flagged as default is automatically demoted.
#### Response � 200 OK
`json
{
  "status": "SUCCESS",
  "message": "Label configuration '4x4 No-Logo' is now the system default"
}
`
#### Error Responses
`json
{ "status": "FAILURE", "message": "Label configuration not found for ID: 99" }
{ "status": "FAILURE", "message": "Cannot set an INACTIVE configuration as default. Activate it first." }
`
---
### Updated: Label Print Request with Configuration � POST /api/products/labels/print
The labelConfigId field is now **optional** in the print request:
`json
{
  "brandName": "MyBrand",
  "batchNo": "BATCH-2026-001",
  "labelConfigId": 2
}
`
| Field          | Type   | Description |
|----------------|--------|-------------|
| labelConfigId  | Long   | Optional. ID of the label_config to use for this print job. If absent, the system-default config is used. If no default exists, built-in 2"�2" layout with all fields shown is applied. |
The labelConfigId is saved in the label_print_jobs record and reused when reprinting (/labels/jobs/{jobId}/reprint).
#### Label Job Response now includes config info
`json
{
  "jobId": 42,
  "brandName": "MyBrand",
  "batchNo": "BATCH-2026-001",
  "resolvedBarcodes": "BC001,BC002",
  "labelCount": 2,
  "pdfUrl": "/labels/labels_1718000000000.pdf",
  "pdfFileExists": true,
  "status": "SUCCESS",
  "printedAt": "2026-06-15T10:30:00Z",
  "printedBy": "SYSTEM",
  "labelConfigId": 2,
  "labelConfigName": "4x4 No-Logo"
}
`
---
### Label Rendering Rules
| Configuration                  | Effect on PDF                                               |
|-------------------------------|-------------------------------------------------------------|
| labelWidthInches × labelHeightInches | Page size of each label (1 inch = 72 pt). Fonts and all measurements scale proportionally from the base 2"×2" size. |
| showLogo = true + logoPath set         | Logo loaded from the specified path and printed centred at top of label. Height = 20% of label height (max 36 pt scale). |
| showLogo = true + logoPath blank/null  | Falls back to the default company logo at `/public/companyLogo/CompanyLogo.png`. |
| showBrandName = false                  | "Brand:" row omitted from the label.                       |
| showBarcode = false                    | Barcode image, separator line, and barcode text all omitted.|
| showFssaiCode = true + fssaiCode set   | "FSSAI: \<licence-no\>" line printed after MRP, before the barcode. |
| showFssaiCode = false **or** fssaiCode blank/null | FSSAI line is omitted entirely.                |
| columnsPerRow = 1 (default)            | All fields printed single-column (one field per row).      |
| columnsPerRow = 2                      | Fields arranged two per row; each column gets half the label width. |
| columnsPerRow = N                      | N fields per row; column width = labelWidth / N. Last row may have fewer than N fields. |
| isDefault = true                       | Auto-selected when no labelConfigId is provided at print time. Only one config can be default at a time. |
#### Common Label Size Presets (inches � 72 pt/inch)
| Size      | Width (pt) | Height (pt) | Use Case |
|-----------|-----------|------------|----------|
| 2" � 2"   | 144       | 144        | Small product labels (default) |
| 2" � 3"   | 144       | 216        | Medium product labels |
| 2" � 4"   | 144       | 288        | Shipping / address labels |
| 4" � 4"   | 288       | 288        | Larger thermal labels |
| 4" � 6"   | 288       | 432        | Standard shipping labels |
---
### Database Table: label_config
`sql
CREATE TABLE label_config (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_name          VARCHAR(100) NOT NULL UNIQUE,
    description          VARCHAR(500),
    label_width_inches   FLOAT        NOT NULL,
    label_height_inches  FLOAT        NOT NULL,
    show_logo            BOOLEAN      NOT NULL DEFAULT FALSE,
    logo_path            VARCHAR(500),
    show_brand_name      BOOLEAN      NOT NULL DEFAULT TRUE,
    show_product_name    BOOLEAN      NOT NULL DEFAULT TRUE,
    show_variant_details BOOLEAN      NOT NULL DEFAULT TRUE,
    show_net_quantity    BOOLEAN      NOT NULL DEFAULT TRUE,
    show_mfg_date        BOOLEAN      NOT NULL DEFAULT TRUE,
    show_exp_date        BOOLEAN      NOT NULL DEFAULT TRUE,
    show_batch_no        BOOLEAN      NOT NULL DEFAULT TRUE,
    show_mrp             BOOLEAN      NOT NULL DEFAULT TRUE,
    show_barcode         BOOLEAN      NOT NULL DEFAULT TRUE,
    fssai_code           VARCHAR(100)          NULL,
    show_fssai_code      TINYINT(1)   NOT NULL DEFAULT 1,
    is_default           BOOLEAN      NOT NULL DEFAULT FALSE,
    columns_per_row      INT          NOT NULL DEFAULT 1,
    status               VARCHAR(20)  DEFAULT 'ACTIVE',
    created_at           DATETIME,
    created_by           VARCHAR(50),
    updated_at           DATETIME,
    updated_by           VARCHAR(50)
);
-- Migration for existing tables:
-- ALTER TABLE label_config ADD COLUMN columns_per_row INT NOT NULL DEFAULT 1;
-- ALTER TABLE label_config ADD COLUMN fssai_code VARCHAR(100) NULL;
-- ALTER TABLE label_config ADD COLUMN show_fssai_code TINYINT(1) NOT NULL DEFAULT 1;
-- label_print_jobs now includes:
ALTER TABLE label_print_jobs
    ADD COLUMN label_config_id BIGINT NULL;
`
"@
Write-Host "Done"
Set-Content -Path "C:\personal\backend\user\src\main\java\com\user\dto\InventoryUpdateDatesDTO.java" -Value @"
package com.user.dto;
import lombok.Data;
import java.time.LocalDate;
/**
 * Request body for updating MFG / EXP / best-before dates on existing
 * inventory detail rows.
 *
 * Supply EITHER {@code batchNo} (updates every item in that batch) OR
 * {@code barcode} (updates one specific unit).  If both are given,
 * {@code batchNo} wins.
 */
@Data
public class InventoryUpdateDatesDTO {
    /** Batch number � all items in this batch are updated. */
    private String batchNo;
    /** Single-item barcode � only this unit is updated (used when batchNo is absent). */
    private String barcode;
    /** Manufactured / production date. */
    private LocalDate mfd;
    /** Best-before date (optional). */
    private LocalDate bestBefore;
    /** Expiry / expiration date. */
    private LocalDate expiryDate;
}
