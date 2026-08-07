# Search API Fix: Include Out-of-Stock Products

## Issue
The `/search` API endpoint was excluding out-of-stock products from search results even when the `inStock` parameter was not explicitly set to `true`.

### Example
URL: `https://www.kuchimittai.com/search?categoryId=0&price=10000&minPrice=0`

**Before Fix**: Only returned products with inventory (in-stock products)
**After Fix**: Returns all products, including out-of-stock products with `stock=0` and `inStock=0`

## Changes Made

### File: `src/main/java/com/user/service/ProductServiceImpl.java`

#### Step 6: Variant Selection Logic (Lines 740-765)
**Before**: Only selected variants that had inventory records
```java
for (ProductVariantEO v : variants) {
    if (inventoryByVariantId.containsKey(v.getId().longValue())) {
        chosenVariantByProductId.put(product.getId(), v);
        break;
    }
}
```

**After**: Conditional variant selection based on `inStock` parameter
```java
if (Boolean.TRUE.equals(inStock)) {
    // inStock=true: only select variants with inventory
    for (ProductVariantEO v : variants) {
        if (inventoryByVariantId.containsKey(v.getId().longValue())) {
            chosenVariantByProductId.put(product.getId(), v);
            break;
        }
    }
} else {
    // inStock is null/false: select cheapest variant regardless of stock
    if (!variants.isEmpty()) {
        chosenVariantByProductId.put(product.getId(), variants.get(0));
    }
}
```

#### Step 8: DTO Building Logic (Lines 787-823)
**Before**: Only handled non-null inventory objects
```java
InventoryEO inventory = inventoryByVariantId.get(chosenVariant.getId().longValue());
productDTO.setStock(inventory.getAvailableQty());
productDTO.setInStock(inventory.getAvailableQty() != null && inventory.getAvailableQty() > 0 ? 1 : 0);
```

**After**: Handles both null and non-null inventory
```java
InventoryEO inventory = inventoryByVariantId.get(chosenVariant.getId().longValue());

if (inventory != null) {
    productDTO.setStock(inventory.getAvailableQty());
    productDTO.setInStock(
        inventory.getAvailableQty() != null && inventory.getAvailableQty() > 0 ? 1 : 0);
} else {
    // No inventory record: out-of-stock product
    productDTO.setStock(0);
    productDTO.setInStock(0);
}
```

#### Method: `getProductsByCategoryId` (Lines 1171-1185)
Updated for consistency to explicitly set `stock=0` and `inStock=0` for out-of-stock products.

## Behavior Comparison

| Scenario | Before | After |
|----------|--------|-------|
| `/search?categoryId=0` | Only in-stock products | All products |
| `/search?categoryId=0&inStock=true` | Only in-stock products | Only in-stock products |
| `/search?categoryId=0&inStock=false` | Empty or only in-stock | All products |
| Out-of-stock product attributes | N/A (excluded) | `stock=0, inStock=0` |

## API Endpoint Impact

### GET `/api/search`
**Parameters:**
- `categoryId` (optional): Category IDs to filter by, or `0` for all
- `inStock` (optional): `true` for in-stock only, `false`/null for all products
- `minPrice`, `price` (optional): Price range filter
- `query`/`search` (optional): Text search
- `sort`, `page`, `limit`: Sorting and pagination

**Response:**
Products now include:
- In-stock items with actual stock quantities
- Out-of-stock items with `stock=0` and `inStock=0`

## Testing

### Test Case 1: Search without inStock filter (new behavior)
```
GET /api/search?categoryId=0&price=10000&minPrice=0
Expected: Returns all products including out-of-stock with inStock=0
```

### Test Case 2: Search with inStock=true (existing behavior preserved)
```
GET /api/search?categoryId=0&inStock=true&price=10000&minPrice=0
Expected: Returns only in-stock products
```

### Test Case 3: Product with no inventory
```
GET /api/search?categoryId=1
Product object when out-of-stock:
{
  "id": 123,
  "name": "Product Name",
  "stock": 0,
  "inStock": 0,
  "price": 500,
  ...
}
```

## Database Queries

No new database queries were added. The fix reuses existing query patterns:
- `inventoryRepository.findAllByProductVariantIdIn()` - Still fetches all inventory
- `inventoryRepository.findAllByProductVariantIdInAndInStock()` - Fetches only in-stock when `inStock=true`

## Backward Compatibility

✅ **Fully backward compatible** for:
- Clients explicitly using `inStock=true` (existing behavior unchanged)
- Existing product data structures
- Database schema

⚠️ **Behavior Change** for:
- Clients that expected the API to return only in-stock products by default
- Now returns out-of-stock products with `inStock=0` field set

## Notes

- Variants are sorted by price ASC in database queries, so the cheapest variant is always selected
- When a product has no variants with price info, the first variant is selected
- Images are only loaded for the selected variant (no performance regression)

