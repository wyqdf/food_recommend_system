-- ========================================
-- 数据库修复脚本
-- 修改时间: 2026-03-03
-- 修改人: AI Assistant
-- 修改原因: 修复两个数据库错误
-- ========================================

-- 问题1: users 表 password 列长度不够
-- BCrypt 加密后的密码长度约 60 字符，需要确保列长度足够
-- 修改: 将 password 列长度改为 VARCHAR(255)

USE food_recommend;

-- 检查并修改 users 表的 password 列长度
ALTER TABLE users MODIFY COLUMN password VARCHAR(255);

-- 检查并修改 admins 表的 password 列长度（如果需要）
ALTER TABLE admins MODIFY COLUMN password VARCHAR(255) NOT NULL;

-- 验证修改
SELECT COLUMN_NAME, COLUMN_TYPE, CHARACTER_MAXIMUM_LENGTH 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'food_recommend' 
AND TABLE_NAME = 'users' 
AND COLUMN_NAME = 'password';

SELECT COLUMN_NAME, COLUMN_TYPE, CHARACTER_MAXIMUM_LENGTH 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'food_recommend' 
AND TABLE_NAME = 'admins' 
AND COLUMN_NAME = 'password';
