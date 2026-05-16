CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(15) UNIQUE,
    role VARCHAR(50) NOT NULL DEFAULT 'RETAILER',
    subscription_plan VARCHAR(50) NOT NULL DEFAULT 'FREE',
    failed_login_attempts INT NOT NULL DEFAULT 0,
    account_locked_until TIMESTAMP,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_users_email ON users(email);
