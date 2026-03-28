-- V7: System config table + loyalty discount on orders

CREATE TABLE IF NOT EXISTS system_config (
    key         VARCHAR(100) PRIMARY KEY,
    value       VARCHAR(500) NOT NULL,
    description VARCHAR(500)
);

INSERT INTO system_config (key, value, description) VALUES
('loyalty.spend_per_point', '10000', 'Số tiền VND cần chi tiêu để nhận 1 điểm'),
('loyalty.vnd_per_point',   '1000',  'Giá trị VND của mỗi điểm khi đổi'),
('loyalty.referral_bonus',  '50',    'Điểm thưởng khi giới thiệu thành công');

-- Add loyalty discount columns to orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS points_discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS points_redeemed        INTEGER       NOT NULL DEFAULT 0;
