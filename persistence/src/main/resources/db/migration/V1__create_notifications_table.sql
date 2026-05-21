-- V1__create_notifications_table.sql
--
-- This file runs exactly once, on the first startup against any database.
-- It creates everything the Notification entity maps to.
--
-- Rule: this file is IMMUTABLE once committed. Never edit a Flyway
-- migration file after it's been run anywhere. If you need to change
-- the schema, create a new file: V2__whatever.sql
-- Flyway stores a checksum of each file — if you edit V1 after it ran,
-- Flyway will refuse to start (checksum mismatch). This protects you
-- from accidentally breaking a production database.

-- Enable UUID generation function (Postgres built-in)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE notifications (
                               id               VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,

                               tenant_id        VARCHAR(255) NOT NULL,

                               idempotency_key  VARCHAR(512) UNIQUE,

                               channel          VARCHAR(20)  NOT NULL
                                   CHECK (channel IN ('EMAIL', 'SMS', 'PUSH')),

                               status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                   CHECK (status IN ('PENDING','QUEUED','SENT','FAILED','SKIPPED')),

                               recipient        VARCHAR(512) NOT NULL,
                               subject          VARCHAR(998),  -- 998 is the RFC 2822 max email subject length
                               body             TEXT         NOT NULL,

                               retry_count      INT          NOT NULL DEFAULT 0,
                               scheduled_at     TIMESTAMPTZ,          -- TZ = with timezone (stored as UTC)
                               sent_at          TIMESTAMPTZ,
                               failure_reason   TEXT,
                               created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                               updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Indexes: tell Postgres to build a lookup structure on these columns.
-- Without an index, every query scans the entire table row by row.
-- With an index, lookup is near-instant even with millions of rows.
--
-- Rule of thumb: add an index on any column you filter by (WHERE clause)
-- or sort by (ORDER BY) frequently.

-- We filter by tenant_id constantly: "show me all notifications for tenant X"
CREATE INDEX idx_notifications_tenant_id
    ON notifications (tenant_id);

-- We filter by status constantly: retry job looks for PENDING, dashboard shows SENT
CREATE INDEX idx_notifications_status
    ON notifications (status);

-- Combined index: when you filter by BOTH tenant AND status together,
-- this is faster than using two separate indexes.
CREATE INDEX idx_notifications_tenant_status
    ON notifications (tenant_id, status);

-- We filter by created_at for time-range queries and the retry job
CREATE INDEX idx_notifications_created_at
    ON notifications (created_at DESC);