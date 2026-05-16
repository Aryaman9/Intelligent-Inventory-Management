CREATE TABLE inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id VARCHAR(255) NOT NULL,
    quantity DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    unit VARCHAR(50) NOT NULL DEFAULT 'piece',
    low_stock_threshold DECIMAL(10, 2) NOT NULL DEFAULT 10,
    reorder_quantity DECIMAL(10, 2),
    cost_price DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (cost_price >= 0),
    selling_price DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (selling_price >= 0),
    mrp DECIMAL(12, 2),
    batch_number VARCHAR(100),
    expiry_date DATE,
    last_restocked_at TIMESTAMP,
    last_sold_at TIMESTAMP,
    location VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(store_id, product_id)
);
CREATE INDEX idx_inventory_store_active ON inventory(store_id) WHERE is_active = true;
CREATE INDEX idx_inventory_low_stock ON inventory(store_id) WHERE is_active = true AND quantity <= low_stock_threshold;
CREATE INDEX idx_inventory_expiry ON inventory(expiry_date) WHERE is_active = true AND expiry_date IS NOT NULL;
