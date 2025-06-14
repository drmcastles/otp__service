CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(10) NOT NULL CHECK (role IN ('ADMIN','USER'))
);

CREATE TABLE IF NOT EXISTS otp_config (
    id          BIGSERIAL PRIMARY KEY,
    length      INT NOT NULL CHECK (length > 0),
    ttl_seconds INT NOT NULL CHECK (ttl_seconds > 0)
);

CREATE TABLE IF NOT EXISTS otp_codes (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation_id VARCHAR(100),
    code         VARCHAR(20) NOT NULL,
    status       VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE','USED','EXPIRED')),
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_otp_codes_code ON otp_codes(code);
