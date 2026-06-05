-- Initial schema for Booking & Loyalty Service (PostgreSQL).

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE customer_profiles (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL UNIQUE REFERENCES users (id),
    first_name VARCHAR(100),
    last_name  VARCHAR(100),
    phone      VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE loyalty_accounts (
    id             BIGSERIAL PRIMARY KEY,
    customer_id    BIGINT NOT NULL UNIQUE REFERENCES customer_profiles (id),
    points_balance BIGINT NOT NULL DEFAULT 0,
    tier           VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE bookings (
    id             BIGSERIAL PRIMARY KEY,
    customer_id    BIGINT NOT NULL REFERENCES customer_profiles (id),
    room_type      VARCHAR(255) NOT NULL,
    check_in_date  DATE NOT NULL,
    check_out_date DATE NOT NULL,
    status         VARCHAR(255) NOT NULL,
    total_amount   NUMERIC(12, 2) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE loyalty_transactions (
    id                 BIGSERIAL PRIMARY KEY,
    loyalty_account_id BIGINT NOT NULL REFERENCES loyalty_accounts (id),
    booking_id         BIGINT REFERENCES bookings (id),
    points             BIGINT NOT NULL,
    transaction_type   VARCHAR(255) NOT NULL,
    description        VARCHAR(255),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_bookings_customer_id ON bookings (customer_id);
CREATE INDEX idx_loyalty_tx_account_id ON loyalty_transactions (loyalty_account_id);
CREATE INDEX idx_loyalty_tx_booking_id ON loyalty_transactions (booking_id);
