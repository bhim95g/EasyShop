-- Run AFTER database/create_database.sql against the easyshop database.
-- Existing orders and order_line_items are retained; this migration adds checkout state.
-- Apply exactly once. Do not re-run the destructive seed database script over real data.
USE easyshop;

ALTER TABLE orders
  ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'LEGACY',
  ADD COLUMN payment_provider VARCHAR(30) NOT NULL DEFAULT 'SIMULATED',
  ADD COLUMN payment_reference VARCHAR(255) NULL,
  ADD COLUMN total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN currency CHAR(3) NOT NULL DEFAULT 'USD';

ALTER TABLE order_line_items
  ADD COLUMN product_name VARCHAR(100) NOT NULL DEFAULT '';

CREATE INDEX idx_orders_user_date ON orders (user_id, date);
CREATE UNIQUE INDEX idx_orders_payment_reference ON orders (payment_reference);
