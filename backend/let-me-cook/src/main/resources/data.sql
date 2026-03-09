USE food_recommend;

-- 插入分类数据
INSERT INTO categories (name) VALUES 
('家常菜'),
('川菜'),
('粤菜'),
('甜点'),
('汤类');

-- 插入难度数据
INSERT INTO difficulties (name) VALUES 
('简单'),
('普通'),
('高级');

-- 插入口味数据
INSERT INTO tastes (name) VALUES 
('原味'),
('咸鲜'),
('甜味'),
('辣味');

-- 插入工艺数据
INSERT INTO techniques (name) VALUES 
('炒'),
('炖'),
('蒸'),
('烤');

-- 插入耗时数据
INSERT INTO time_costs (name) VALUES 
('10分钟'),
('30分钟'),
('1小时');

-- 插入食材数据
INSERT INTO ingredients (name) VALUES 
('五花肉'),
('鸡蛋'),
('西红柿'),
('黄瓜'),
('盐'),
('生抽'),
('料酒'),
('糖');

-- 插入厨具数据
INSERT INTO cookwares (name) VALUES 
('炒锅'),
('蒸锅'),
('砂锅'),
('烤箱'),
('微波炉'),
('电饭煲'),
('平底锅'),
('高压锅');

-- 插入用户数据
INSERT INTO users (username, password, nickname, email) VALUES 
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', 'test@example.com');

-- 插入菜谱数据
INSERT INTO recipes (title, author, description, tips, cookware, image, taste_id, technique_id, time_cost_id, difficulty_id, like_count, reply_count) VALUES 
('红烧肉', '美食达人', '经典家常菜，肥而不腻', '小火慢炖', '炒锅', 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400', 2, 1, 2, 2, 156, 23),
('番茄炒蛋', '厨房新手', '简单又美味的下饭菜', '鸡蛋要炒嫩', '炒锅', 'https://images.unsplash.com/photo-1482049016688-2d3e1b311543?w=400', 2, 1, 1, 1, 234, 45),
('清蒸鱼', '美食达人', '保留鱼肉鲜嫩', '水开后蒸8分钟', '蒸锅', 'https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=400', 1, 3, 2, 2, 89, 12),
('糖醋排骨', '美食达人', '酸甜可口', '糖色要炒好', '炒锅', 'https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?w=400', 3, 1, 2, 2, 178, 34),
('蛋炒饭', '厨房新手', '粒粒分明', '米饭要过夜', '炒锅', 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=400', 2, 1, 1, 1, 145, 28),
('宫保鸡丁', '川菜师傅', '麻辣鲜香', '花生要脆', '炒锅', 'https://images.unsplash.com/photo-1525755662778-989d0524087e?w=400', 4, 1, 2, 2, 267, 56);

-- 关联菜谱和分类
INSERT INTO recipe_categories (recipe_id, category_id) VALUES 
(1, 1), (1, 2),
(2, 1),
(3, 1), (3, 3),
(4, 1),
(5, 1),
(6, 1), (6, 2);

-- 插入菜谱食材
INSERT INTO recipe_ingredients (recipe_id, ingredient_id, ingredient_type, quantity) VALUES 
(1, 1, 'main', '500g'),
(1, 5, 'seasoning', '适量'),
(1, 6, 'seasoning', '2勺'),
(1, 7, 'seasoning', '1勺'),
(2, 2, 'main', '3个'),
(2, 3, 'main', '2个'),
(2, 5, 'seasoning', '适量'),
(3, 5, 'seasoning', '适量'),
(3, 6, 'seasoning', '1勺'),
(4, 1, 'main', '400g'),
(4, 8, 'seasoning', '3勺'),
(4, 6, 'seasoning', '2勺'),
(5, 2, 'main', '2个'),
(5, 5, 'seasoning', '适量'),
(6, 1, 'main', '300g'),
(6, 5, 'seasoning', '适量'),
(6, 6, 'seasoning', '2勺');

-- 插入烹饪步骤
INSERT INTO cooking_steps (recipe_id, step_number, description) VALUES 
(1, 1, '五花肉切块，冷水下锅焯水去血沫'),
(1, 2, '锅中放油，加冰糖小火炒糖色'),
(1, 3, '放入五花肉翻炒上色'),
(1, 4, '加生抽、料酒、水，小火炖1小时'),
(1, 5, '大火收汁即可'),
(2, 1, '鸡蛋打散，西红柿切块'),
(2, 2, '锅中放油，炒鸡蛋至金黄盛出'),
(2, 3, '炒西红柿出汁'),
(2, 4, '加入鸡蛋翻炒，加盐调味'),
(3, 1, '鱼洗净，划几刀，抹盐'),
(3, 2, '放上姜丝、葱段'),
(3, 3, '水开后蒸8分钟'),
(3, 4, '淋上生抽即可'),
(4, 1, '排骨焯水去血沫'),
(4, 2, '锅中放油，炒糖色'),
(4, 3, '放入排骨翻炒上色'),
(4, 4, '加醋、糖、生抽，小火炖40分钟'),
(4, 5, '大火收汁'),
(5, 1, '鸡蛋打散'),
(5, 2, '锅中放油，炒鸡蛋'),
(5, 3, '加入米饭翻炒'),
(5, 4, '加盐调味'),
(6, 1, '鸡胸肉切丁，加料酒、盐腌制'),
(6, 2, '锅中放油，炒鸡丁至变色盛出'),
(6, 3, '炒干辣椒、花椒'),
(6, 4, '加入鸡丁、花生米翻炒'),
(6, 5, '加调味汁翻炒均匀');

-- 插入管理员数据 (密码: 123456)
INSERT INTO admins (username, password, email, role, status) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@example.com', 'super_admin', 1)
ON DUPLICATE KEY UPDATE password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi';
