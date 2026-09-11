-- Adds per-step status columns to `shipping` plus a dedicated history table for
-- each step so that every status transition (CREATE_ORDER, GENERATE_AWB,
-- REQUEST_PICKUP, GENERATE_LABEL, TRACK_SHIPMENT, and estimated/expected
-- delivery-date resolution) can be individually audited over time.
--
-- Run manually against the target database (no migration framework such as
-- Flyway/Liquibase is wired into this project - see other root-level *.sql
-- scripts for the same convention). Hibernate (spring.jpa.hibernate.ddl-auto=update)
-- will also create/alter these automatically on app startup, but this script is
-- kept for explicit/manual DB provisioning and review.

ALTER TABLE shipping
    ADD COLUMN IF NOT EXISTS shiprocket_order_status VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS generate_awb_status VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS request_pickup_status VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS generate_label_status VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS track_shipment_status VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS estimate_status VARCHAR(50) NULL;

CREATE TABLE IF NOT EXISTS shiprocket_order_status_history (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL REFERENCES shipping(shipment_id),
    status VARCHAR(50),
    remarks VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_shiprocket_order_status_history_shipment_id
    ON shiprocket_order_status_history(shipment_id);

CREATE TABLE IF NOT EXISTS generate_awb_status_history (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL REFERENCES shipping(shipment_id),
    status VARCHAR(50),
    remarks VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_generate_awb_status_history_shipment_id
    ON generate_awb_status_history(shipment_id);

CREATE TABLE IF NOT EXISTS request_pickup_status_history (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL REFERENCES shipping(shipment_id),
    status VARCHAR(50),
    remarks VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_request_pickup_status_history_shipment_id
    ON request_pickup_status_history(shipment_id);

CREATE TABLE IF NOT EXISTS generate_label_status_history (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL REFERENCES shipping(shipment_id),
    status VARCHAR(50),
    remarks VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_generate_label_status_history_shipment_id
    ON generate_label_status_history(shipment_id);

CREATE TABLE IF NOT EXISTS track_shipment_status_history (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL REFERENCES shipping(shipment_id),
    status VARCHAR(50),
    remarks VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_track_shipment_status_history_shipment_id
    ON track_shipment_status_history(shipment_id);

CREATE TABLE IF NOT EXISTS estimate_status_history (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL REFERENCES shipping(shipment_id),
    status VARCHAR(50),
    remarks VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_estimate_status_history_shipment_id
    ON estimate_status_history(shipment_id);

