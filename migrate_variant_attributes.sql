-- ============================================================
-- MIGRATION: Copy product_attributes from existing variant
--            to sibling variants (same product) that have none.
--
-- Tables involved:
--   products            (id, name, slug, ...)
--   product_variants    (id, product_id, sku_code, ...)
--   product_attributes  (id, product_var_id, attribute_name, attribute_value)
--
-- Strategy:
--   For every variant that has 0 attributes, find the sibling
--   variant with the lowest id under the same product that DOES
--   have attributes, and copy all its attributes.
--
-- Run STEP 1 first (dry-run preview), review, then run STEP 2.
-- ============================================================


-- ============================================================
-- STEP 1 — DRY RUN: Preview rows that will be inserted
-- ============================================================
SELECT
    p.id                 AS product_id,
    p.name               AS product_name,
    tgt_var.id           AS target_variant_id,
    tgt_var.sku_code     AS target_sku,
    src.id               AS source_variant_id,
    src_var.sku_code     AS source_sku,
    pa.attribute_name,
    pa.attribute_value
FROM product_variants tgt_var

-- Find the lowest-id sibling variant (same product) that HAS attributes
JOIN LATERAL (
    SELECT sv.id
    FROM   product_variants sv
    WHERE  sv.product_id = tgt_var.product_id
      AND  sv.id <> tgt_var.id
      AND  EXISTS (
               SELECT 1
               FROM   product_attributes pa2
               WHERE  pa2.product_var_id = sv.id
           )
    ORDER  BY sv.id
    LIMIT  1
) src ON TRUE                                          -- LATERAL produces 0 rows when no sibling found → variant skipped

JOIN product_variants    src_var ON src_var.id = src.id
JOIN product_attributes  pa      ON pa.product_var_id = src.id
JOIN products            p       ON p.id = tgt_var.product_id

-- Only show variants that have NO attributes yet
WHERE NOT EXISTS (
    SELECT 1
    FROM   product_attributes
    WHERE  product_var_id = tgt_var.id
)

ORDER BY p.id, tgt_var.id, pa.attribute_name;


-- ============================================================
-- STEP 2 — ACTUAL MIGRATION: Insert the missing attributes
--          (safe to run multiple times — WHERE NOT EXISTS guard)
-- ============================================================
INSERT INTO product_attributes (product_var_id, attribute_name, attribute_value)
SELECT
    tgt_var.id,
    pa.attribute_name,
    pa.attribute_value
FROM product_variants tgt_var

-- Find the lowest-id sibling variant (same product) that HAS attributes
JOIN LATERAL (
    SELECT sv.id
    FROM   product_variants sv
    WHERE  sv.product_id = tgt_var.product_id
      AND  sv.id <> tgt_var.id
      AND  EXISTS (
               SELECT 1
               FROM   product_attributes pa2
               WHERE  pa2.product_var_id = sv.id
           )
    ORDER  BY sv.id
    LIMIT  1
) src ON TRUE

JOIN product_attributes pa ON pa.product_var_id = src.id

-- Only process variants that have NO attributes yet
WHERE NOT EXISTS (
    SELECT 1
    FROM   product_attributes
    WHERE  product_var_id = tgt_var.id
);


-- ============================================================
-- STEP 3 — VERIFY: Count attributes per variant after migration
-- ============================================================
SELECT
    p.id                        AS product_id,
    p.name                      AS product_name,
    pv.id                       AS variant_id,
    pv.sku_code                 AS sku,
    COUNT(pa.id)                AS attribute_count,
    STRING_AGG(
        pa.attribute_name || ' = ' || pa.attribute_value,
        ' | '
        ORDER BY pa.attribute_name
    )                           AS attributes
FROM products p
JOIN product_variants  pv ON pv.product_id   = p.id
LEFT JOIN product_attributes pa ON pa.product_var_id = pv.id
GROUP BY p.id, p.name, pv.id, pv.sku_code
ORDER BY p.id, pv.id;


-- ============================================================
-- STEP 4 — REPORT: Products where some variants still have
--          0 attributes (i.e. none of the siblings had attributes)
-- ============================================================
SELECT
    p.id          AS product_id,
    p.name        AS product_name,
    pv.id         AS variant_id,
    pv.sku_code   AS sku
FROM products p
JOIN product_variants pv ON pv.product_id = p.id
WHERE NOT EXISTS (
    SELECT 1 FROM product_attributes WHERE product_var_id = pv.id
)
ORDER BY p.id, pv.id;

