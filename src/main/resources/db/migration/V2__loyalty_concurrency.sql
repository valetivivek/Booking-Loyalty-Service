-- Concurrency hardening for the loyalty domain.

-- Optimistic-locking version column on loyalty accounts.
ALTER TABLE loyalty_accounts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Enforce at most one transaction of a given type per booking (e.g. one EARNED
-- award per completed booking). REDEEMED rows have a NULL booking_id, and in
-- PostgreSQL NULLs are treated as distinct, so redemptions are unaffected.
ALTER TABLE loyalty_transactions
    ADD CONSTRAINT uq_loyalty_tx_booking_type UNIQUE (booking_id, transaction_type);
