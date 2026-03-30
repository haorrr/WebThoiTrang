-- V9: Security settings in system_config

INSERT INTO system_config (key, value, description) VALUES
('security.disable_right_click',  'true',  'Chặn chuột phải trên trang shop (không áp dụng với admin)'),
('security.disable_devtools_key', 'true',  'Chặn phím tắt mở DevTools F12/Ctrl+Shift+I (không áp dụng với admin)'),
('security.auto_ban_devtools',    'false', 'Tự động vô hiệu hóa tài khoản nếu phát hiện mở DevTools')
ON CONFLICT (key) DO NOTHING;
