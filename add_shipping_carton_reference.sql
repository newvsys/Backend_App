-- Adds a direct foreign-key reference from `shipping` to `carton`, storing the
-- carton actually used to pack each shipment. Column name matches the JPA
-- @JoinColumn("carton_no") on ShippingEO.carton (a ManyToOne to CartonEO).
--
-- Run this once against the existing database (idempotent-ish: uses IF NOT EXISTS
-- where supported by the DB engine).

ALTER TABLE shipping
    ADD COLUMN IF NOT EXISTS carton_no BIGINT NULL;

ALTER TABLE shipping
    ADD CONSTRAINT fk_shipping_carton
        FOREIGN KEY (carton_no) REFERENCES carton (id);

CREATE INDEX IF NOT EXISTS idx_shipping_carton_no ON shipping (carton_no);

