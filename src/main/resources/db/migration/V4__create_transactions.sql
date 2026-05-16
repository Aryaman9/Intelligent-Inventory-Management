CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    inventory_id UUID NOT NULL REFERENCES inventory(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('SALE', 'PURCHASE', 'RETURN', 'ADJUSTMENT')),
    quantity DECIMAL(12, 2) NOT NULL CHECK (quantity > 0),
    price_per_unit DECIMAL(12, 2) NOT NULL CHECK (price_per_unit >= 0),
    total_amount DECIMAL(14, 2) NOT NULL,
    payment_method VARCHAR(50),
    customer_name VARCHAR(255),
    customer_phone VARCHAR(15),
    invoice_number VARCHAR(100) UNIQUE,
    idempotency_key VARCHAR(255) UNIQUE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_transactions_store_created ON transactions(store_id, created_at DESC);
CREATE INDEX idx_transactions_type_store ON transactions(type, store_id);
CREATE INDEX idx_transactions_idempotency ON transactions(idempotency_key) WHERE idempotency_key IS NOT NULL;
