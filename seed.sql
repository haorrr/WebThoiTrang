-- Clean up existing data to avoid duplicates if run multiple times
DELETE FROM order_items;
DELETE FROM cart_items;
DELETE FROM reviews;
DELETE FROM product_images;
DELETE FROM products;
DELETE FROM categories;

-- 1. Insert Categories
INSERT INTO categories (id, name, slug, description, image_url, status, created_at, updated_at) VALUES 
(1, 'Outerwear', 'outerwear', 'Premium coats, jackets, and blazers designed for elegance and warmth.', '/images/product-jacket.png', 'ACTIVE', NOW(), NOW()),
(2, 'Dresses', 'dresses', 'Timeless dresses crafted from refined silk, linen, and cotton.', '/images/product-dress.png', 'ACTIVE', NOW(), NOW()),
(3, 'Bags', 'bags', 'Handcrafted leather bags offering both function and luxury.', '/images/product-handbag.png', 'ACTIVE', NOW(), NOW()),
(4, 'Accessories', 'accessories', 'Luxury details that complete your look.', '/images/product-sunglasses.png', 'ACTIVE', NOW(), NOW());

-- Reset sequence
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));

-- 2. Insert Products
INSERT INTO products (id, name, slug, description, ai_description, price, sale_price, stock, status, category_id, created_at, updated_at) VALUES 
-- Outerwear
(1, 'Milano Leather Jacket', 'milano-leather-jacket', '<p>Áo khoác da thật 100% nhập khẩu từ Ý. Thiết kế form dáng classic, tôn lên vẻ nam tính, mạnh mẽ nhưng cũng không kém phần thanh lịch.</p>', 'Premium black leather jacket with minimalist zippers and a tailored fit.', 12500000.00, 11500000.00, 50, 'ACTIVE', 1, NOW(), NOW()),
(2, 'Classic Trench Coat', 'classic-trench-coat', '<p>Áo măng tô dáng dài màu beige kinh điển. Chất liệu chống nước nhẹ, thích hợp cho thời tiết giao mùa.</p>', 'A beige double-breasted trench coat made of water-resistant gabardine cotton.', 8500000.00, NULL, 30, 'ACTIVE', 1, NOW() - INTERVAL '1 day', NOW()),
(3, 'Noir Wool Blazer', 'noir-wool-blazer', '<p>Áo blazer len cừu mỏng, cấu trúc không đệm vai, mang lại cảm giác thoải mái tuyệt đối nhưng vẫn giữ form chuẩn.</p>', 'Unstructured black wool blazer offering modern effortless tailoring.', 9200000.00, 8000000.00, 25, 'ACTIVE', 1, NOW() - INTERVAL '2 days', NOW()),

-- Dresses
(4, 'Sienna Silk Dress', 'sienna-silk-dress', '<p>Váy lụa cao cấp với thiết kế trễ vai quyến rũ. Màu đỏ tía trầm ấm sang trọng, hoàn hảo cho những đêm tiệc.</p>', 'Elegant deep crimson silk slip dress featuring a cowl neckline and an open back design.', 7800000.00, 7100000.00, 40, 'ACTIVE', 2, NOW(), NOW()),
(5, 'Ivory Pleated Gown', 'ivory-pleated-gown', '<p>Váy dạ hội xếp ly màu trắng ngà. Điểm nhấn là thắt lưng voan mềm mại, tôn vóc dáng thanh mảnh.</p>', 'A flowing ivory pleated maxi dress cinched at the waist with ethereal tulle detailing.', 15000000.00, NULL, 10, 'ACTIVE', 2, NOW() - INTERVAL '5 days', NOW()),
(6, 'Midnight Velvet Midi', 'midnight-velvet-midi', '<p>Váy nhung màu xanh màn đêm. Chiều dài qua gối sang trọng, lý tưởng cho dạ tiệc mùa thu đông.</p>', 'Midnight blue velvet midi dress with long sleeves and a sophisticated boat neckline.', 6500000.00, NULL, 15, 'ACTIVE', 2, NOW(), NOW()),

-- Bags
(7, 'Firenze Leather Tote', 'firenze-leather-tote', '<p>Túi xách da bò thượng hạng size lớn, phù hợp đựng laptop và các vật dụng công sở hàng ngày.</p>', 'Spacious cognac full-grain leather tote bag with sturdy structured top handles.', 13500000.00, 12000000.00, 20, 'ACTIVE', 3, NOW() - INTERVAL '10 days', NOW()),
(8, 'Monochrome Mini Bag', 'monochrome-mini-bag', '<p>Túi xách mini màu đen nhám. Hoàn thiện bởi logo kim loại mạ tĩnh điện minimalist.</p>', 'Sleek matte black structured mini crossbody bag featuring a hidden magnetic closure.', 6200000.00, NULL, 35, 'ACTIVE', 3, NOW(), NOW()),
(9, 'Canvas Weekend Duffle', 'canvas-weekend-duffle', '<p>Túi du lịch phối da thật và vải canvas chống thấm nước. Thể tích 40L, lý tưởng cho những chuyến đi ngắn ngày.</p>', 'Durable cream canvas duffle bag accented with thick dark brown leather trims.', 9800000.00, 8500000.00, 18, 'ACTIVE', 3, NOW() - INTERVAL '3 days', NOW()),

-- Accessories
(10, 'Noir Aviator Shades', 'noir-aviator-shades', '<p>Mắt kính phi công cổ điển với gọng vàng thanh mảnh. Mắt kính polarized chống tia UV 100%.</p>', 'Gold-rimmed aviator sunglasses featuring premium gradient dark tinted polarized lenses.', 4500000.00, NULL, 100, 'ACTIVE', 4, NOW(), NOW()),
(11, 'Minimalist Mesh Watch', 'minimalist-mesh-watch', '<p>Đồng hồ tinh giản với dây lưới thép không gỉ. Mặt kính sapphire chống trầy xước xuất sắc.</p>', 'Ultra-thin silver stainless steel watch with a mesh strap and a stark white dial.', 5500000.00, 4900000.00, 45, 'ACTIVE', 4, NOW() - INTERVAL '4 days', NOW()),
(12, 'Cashmere Scarf', 'cashmere-scarf', '<p>Khăn choàng cổ len cashmere 100% dệt tay. Cực kỳ mềm mại và giữ ấm tuyệt đối cho mùa đông.</p>', 'Ultra-soft charcoal grey 100% pure cashmere scarf with fringed edges.', 3200000.00, NULL, 60, 'ACTIVE', 4, NOW(), NOW());

-- Reset sequence
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));

-- 3. Insert Product Images (Map to frontend images where available, or Unsplash placeholders)
INSERT INTO product_images (product_id, image_url, is_primary, sort_order, created_at, updated_at) VALUES 
(1, '/images/product-jacket.png', true, 1, NOW(), NOW()),
(1, 'https://images.unsplash.com/photo-1520975954732-57dd22299614?auto=format&fit=crop&q=80&w=800', false, 2, NOW(), NOW()),

(2, 'https://images.unsplash.com/photo-1591047139829-d91aecb6caea?auto=format&fit=crop&q=80&w=800', true, 1, NOW(), NOW()),

(3, 'https://images.unsplash.com/photo-1592878904946-b3cd8ae243d0?auto=format&fit=crop&q=80&w=800', true, 1, NOW(), NOW()),

(4, '/images/product-dress.png', true, 1, NOW(), NOW()),
(4, 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?auto=format&fit=crop&q=80&w=800', false, 2, NOW(), NOW()),

(5, 'https://images.unsplash.com/photo-1566150905458-1bf1fc113f0d?auto=format&fit=crop&q=80&w=800', true, 1, NOW(), NOW()),

(6, 'https://images.unsplash.com/photo-1539008835657-9e8e9680c956?auto=format&fit=crop&q=80&w=800', true, 1, NOW(), NOW()),

(7, '/images/product-handbag.png', true, 1, NOW(), NOW()),
(7, 'https://images.unsplash.com/photo-1591561954557-26941169b49e?auto=format&fit=crop&q=80&w=800', false, 2, NOW(), NOW()),

(8, 'https://images.unsplash.com/photo-1584916201218-f4242ceb4809?auto=format&fit=crop&q=80&w=800', true, 1, NOW(), NOW()),

(9, 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&q=80&w=800', true, 1, NOW(), NOW()),

(10, '/images/product-sunglasses.png', true, 1, NOW(), NOW()),

(11, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&q=80&w=800', true, 1, NOW(), NOW()),

(12, 'https://images.unsplash.com/photo-1620794341491-9f5e8c1878d6?auto=format&fit=crop&q=80&w=800', true, 1, NOW(), NOW());
