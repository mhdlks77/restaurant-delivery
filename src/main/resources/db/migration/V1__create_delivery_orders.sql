CREATE TABLE IF NOT EXISTS delivery_orders (
    id BIGSERIAL PRIMARY KEY,
    woocommerce_order_id VARCHAR(255) UNIQUE,
    uber_delivery_id VARCHAR(255),
    status VARCHAR(50),
    customer_name VARCHAR(255),
    customer_address VARCHAR(255),
    customer_phone VARCHAR(255),
    order_type VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);