-- ============================================================
-- REPORT: Category → Product → Product Variants Details
--
-- Tables:
--   product_categories  (id, name, href, status, ...)
--   products            (id, category_id, name, slug, status, ...)
--   product_variants    (id, product_id, sku_code, pack_size, uom,
--                        container_type, mrp, selling_price, status,
--                        length, breadth, height, weight, ...)
--   product_attributes  (id, product_var_id, attribute_name, attribute_value)
--   inventory           (id, product_variant_id, available_qty,
--                        total_qty, reserved_qty, status, ...)
-- ============================================================


-- ============================================================
-- QUERY 1 — Full flat report: one row per variant
--           (best for export to Excel / CSV)
-- ============================================================
SELECT
    -- Category
    pc.id                                   AS category_id,
    pc.name                                 AS category_name,
    pc.status                               AS category_status,

    -- Product
    p.id                                    AS product_id,
    p.name                                  AS product_name,
    p.slug                                  AS product_slug,
    p.status                                AS product_status,

    -- Variant
    pv.id                                   AS variant_id,
    pv.sku_code,
    pv.pack_size,
    pv.uom,
    pv.container_type,
    pv.mrp,
    pv.selling_price,
    pv.currency,
    pv.status                               AS variant_status,
    pv.weight,
    pv.length,
    pv.breadth,
    pv.height,

    -- Inventory
    COALESCE(inv.available_qty, 0)          AS available_qty,
    COALESCE(inv.total_qty, 0)              AS total_qty,
    COALESCE(inv.reserved_qty, 0)           AS reserved_qty,
    CASE
        WHEN COALESCE(inv.available_qty, 0) > 0 THEN 'In Stock'
        ELSE 'Out of Stock'
    END                                     AS stock_status,

    -- Attributes (aggregated as key=value pairs)
    STRING_AGG(
        pa.attribute_name || ' = ' || pa.attribute_value,
        ' | '
        ORDER BY pa.attribute_name
    )                                       AS attributes

FROM product_categories pc
JOIN products          p   ON p.category_id      = pc.id
JOIN product_variants  pv  ON pv.product_id      = p.id
LEFT JOIN inventory    inv ON inv.product_variant_id = pv.id
LEFT JOIN product_attributes pa ON pa.product_var_id = pv.id

GROUP BY
    pc.id, pc.name, pc.status,
    p.id, p.name, p.slug, p.status,
    pv.id, pv.sku_code, pv.pack_size, pv.uom, pv.container_type,
    pv.mrp, pv.selling_price, pv.currency, pv.status,
    pv.weight, pv.length, pv.breadth, pv.height,
    inv.available_qty, inv.total_qty, inv.reserved_qty

ORDER BY pc.name, p.name, pv.selling_price;


-- ============================================================
-- QUERY 2 — Category summary: product count & variant count
-- ============================================================
SELECT
    pc.id                       AS category_id,
    pc.name                     AS category_name,
    pc.status                   AS category_status,
    COUNT(DISTINCT p.id)        AS total_products,
    COUNT(DISTINCT pv.id)       AS total_variants,
    COUNT(DISTINCT CASE WHEN COALESCE(inv.available_qty, 0) > 0 THEN pv.id END)
                                AS in_stock_variants,
    COUNT(DISTINCT CASE WHEN COALESCE(inv.available_qty, 0) = 0 THEN pv.id END)
                                AS out_of_stock_variants,
    MIN(pv.selling_price)       AS min_price,
    MAX(pv.selling_price)       AS max_price
FROM product_categories pc
LEFT JOIN products         p   ON p.category_id         = pc.id
LEFT JOIN product_variants pv  ON pv.product_id         = p.id
LEFT JOIN inventory        inv ON inv.product_variant_id = pv.id
GROUP BY pc.id, pc.name, pc.status
ORDER BY pc.name;


-- ============================================================
-- QUERY 3 — Product summary: variant count & stock per product
-- ============================================================
SELECT
    pc.id                       AS category_id,
    pc.name                     AS category_name,
    p.id                        AS product_id,
    p.name                      AS product_name,
    p.slug,
    p.status                    AS product_status,
    COUNT(DISTINCT pv.id)       AS total_variants,
    COUNT(DISTINCT CASE WHEN COALESCE(inv.available_qty, 0) > 0 THEN pv.id END)
                                AS in_stock_variants,
    SUM(COALESCE(inv.available_qty, 0))
                                AS total_available_qty,
    MIN(pv.selling_price)       AS min_selling_price,
    MAX(pv.selling_price)       AS max_selling_price
FROM product_categories pc
JOIN products          p   ON p.category_id         = pc.id
LEFT JOIN product_variants pv  ON pv.product_id     = p.id
LEFT JOIN inventory    inv ON inv.product_variant_id = pv.id
GROUP BY pc.id, pc.name, p.id, p.name, p.slug, p.status
ORDER BY pc.name, p.name;


-- ============================================================
-- QUERY 4 — Only ACTIVE categories, ACTIVE products,
--           ACTIVE variants (production view)
-- ============================================================
SELECT
    pc.id                                   AS category_id,
    pc.name                                 AS category_name,
    p.id                                    AS product_id,
    p.name                                  AS product_name,
    p.slug,
    pv.id                                   AS variant_id,
    pv.sku_code,
    pv.pack_size,
    pv.uom,
    pv.mrp,
    pv.selling_price,
    COALESCE(inv.available_qty, 0)          AS available_qty,
    CASE
        WHEN COALESCE(inv.available_qty, 0) > 0 THEN 'In Stock'
        ELSE 'Out of Stock'
    END                                     AS stock_status,
    STRING_AGG(
        pa.attribute_name || ' = ' || pa.attribute_value,
        ' | '
        ORDER BY pa.attribute_name
    )                                       AS attributes
FROM product_categories pc
JOIN products          p   ON p.category_id         = pc.id  AND p.status  = 'A'
JOIN product_variants  pv  ON pv.product_id         = p.id   AND pv.status = 'A'
LEFT JOIN inventory    inv ON inv.product_variant_id = pv.id
LEFT JOIN product_attributes pa ON pa.product_var_id = pv.id
WHERE pc.status = 'A'
GROUP BY
    pc.id, pc.name,
    p.id, p.name, p.slug,
    pv.id, pv.sku_code, pv.pack_size, pv.uom, pv.mrp, pv.selling_price,
    inv.available_qty
ORDER BY pc.name, p.name, pv.selling_price;


-- ============================================================
-- QUERY 5 — Filter by a specific category name
--           (change 'Cow Ghee' to desired category)
-- ============================================================
SELECT
    pc.name                                 AS category_name,
    p.name                                  AS product_name,
    p.slug,
    pv.sku_code,
    pv.pack_size,
    pv.uom,
    pv.mrp,
    pv.selling_price,
    COALESCE(inv.available_qty, 0)          AS available_qty,
    CASE
        WHEN COALESCE(inv.available_qty, 0) > 0 THEN 'In Stock'
        ELSE 'Out of Stock'
    END                                     AS stock_status,
    STRING_AGG(
        pa.attribute_name || ' = ' || pa.attribute_value,
        ' | '
        ORDER BY pa.attribute_name
    )                                       AS attributes
FROM product_categories pc
JOIN products          p   ON p.category_id         = pc.id
JOIN product_variants  pv  ON pv.product_id         = p.id
LEFT JOIN inventory    inv ON inv.product_variant_id = pv.id
LEFT JOIN product_attributes pa ON pa.product_var_id = pv.id
WHERE pc.name = 'Cow Ghee'          -- ← change category name here
GROUP BY
    pc.name, p.name, p.slug,
    pv.sku_code, pv.pack_size, pv.uom, pv.mrp, pv.selling_price,
    inv.available_qty
ORDER BY p.name, pv.selling_price;

