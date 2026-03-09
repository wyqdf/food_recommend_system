# 美食推荐系统 - 管理员 API 接口文档

> **版本**: v2.0  
> **最后更新**: 2026-03-01  
> **基准 URL**: `http://localhost:8080/api/admin`  
> **数据格式**: JSON  
> **字符编码**: UTF-8

---

## 目录

1. [概述](#概述)
2. [认证模块](#认证模块)
3. [用户管理](#用户管理)
4. [食谱管理](#食谱管理)
5. [分类管理](#分类管理)
6. [属性管理](#属性管理)
7. [统计管理](#统计管理)
8. [日志管理](#日志管理)
9. [错误码说明](#错误码说明)

---

## 概述

### 数据库表结构

#### 管理员表 (admins)

```sql
CREATE TABLE admins (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role ENUM('super_admin', 'admin') DEFAULT 'admin',
    status TINYINT DEFAULT 1 COMMENT '1:启用 0:禁用',
    last_login_time DATETIME,
    last_login_ip VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';
```

**索引**:
- `pk`: id (主键)
- `uk_username`: username (唯一索引)

#### 系统日志表 (system_logs)

```sql
CREATE TABLE system_logs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    admin_id INT,
    operation VARCHAR(100) COMMENT '操作类型',
    module VARCHAR(50) COMMENT '模块',
    content TEXT COMMENT '操作内容',
    ip VARCHAR(50),
    user_agent VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES admins(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';
```

**索引**:
- `pk`: id (主键)
- `idx_admin_id`: admin_id (外键索引)
- `idx_module`: module (模块查询)
- `idx_create_time`: create_time (时间排序)

#### 用户表增强字段

```sql
ALTER TABLE users ADD COLUMN status TINYINT DEFAULT 1 COMMENT '1:正常 0:禁用';
ALTER TABLE users ADD COLUMN email VARCHAR(100);
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
ALTER TABLE users ADD COLUMN last_login_time DATETIME;
```

**新增索引**:
- `idx_users_status`: status (状态筛选)
- `idx_users_email`: email (邮箱查询)
- `idx_users_username`: username (用户名查询)

#### 属性表增强

```sql
ALTER TABLE tastes ADD COLUMN recipe_count INT DEFAULT 0;
ALTER TABLE techniques ADD COLUMN recipe_count INT DEFAULT 0;
ALTER TABLE time_costs ADD COLUMN recipe_count INT DEFAULT 0;
ALTER TABLE difficulties ADD COLUMN recipe_count INT DEFAULT 0;
ALTER TABLE ingredients ADD COLUMN recipe_count INT DEFAULT 0;
```

**说明**: recipe_count 字段用于缓存关联的食谱数量，提高查询性能

---

## 认证模块

### 1. 管理员登录

**接口地址**: `POST /login`

**功能描述**: 管理员登录，获取访问令牌

**请求参数**:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

**请求参数说明**:

| 参数 | 类型 | 必填 | 长度 | 说明 |
|------|------|------|------|------|
| username | string | 是 | 1-100 | 管理员用户名 |
| password | string | 是 | 6-255 | 密码（加密传输） |

**响应示例**:

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhZG1pbklkIjoxLCJyb2xlIjoic3VwZXJfYWRtaW4ifQ...",
    "admin": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "role": "super_admin",
      "status": 1
    }
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| token | string | JWT 令牌，有效期 24 小时 |
| admin.id | int | 管理员 ID |
| admin.username | string | 管理员用户名 |
| admin.email | string | 邮箱 |
| admin.role | enum | 角色：super_admin/admin |
| admin.status | int | 状态：1-启用/0-禁用 |

**数据库表**: `admins`

**使用的索引**: `uk_username` (username 唯一索引)

**业务逻辑**:

1. 根据 username 查询管理员信息
2. 验证密码（BCrypt 加密）
3. 检查管理员状态（status=1）
4. 生成 JWT Token（包含 adminId 和 role）
5. 更新 last_login_time 和 last_login_ip
6. 记录登录日志到 system_logs

**错误响应**:

```json
{
  "code": 1001,
  "message": "用户名或密码错误",
  "data": null
}
```

```json
{
  "code": 1002,
  "message": "管理员账号已被禁用",
  "data": null
}
```

---

### 2. 获取管理员信息

**接口地址**: `GET /profile`

**功能描述**: 获取当前登录管理员的详细信息

**请求头**:

```
Authorization: Bearer <admin_token>
```

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "role": "super_admin",
    "status": 1,
    "lastLoginTime": "2026-03-01T10:00:00",
    "createTime": "2026-01-01T00:00:00"
  }
}
```

**数据库表**: `admins`

**使用的索引**: `pk` (主键，从 Token 中解析 adminId)

---

### 3. 修改密码

**接口地址**: `PUT /password`

**功能描述**: 修改管理员密码

**请求头**:

```
Authorization: Bearer <admin_token>
```

**请求参数**:

```json
{
  "oldPassword": "admin123",
  "newPassword": "newAdmin123"
}
```

**请求参数说明**:

| 参数 | 类型 | 必填 | 长度 | 说明 |
|------|------|------|------|------|
| oldPassword | string | 是 | 6-255 | 旧密码 |
| newPassword | string | 是 | 6-255 | 新密码 |

**响应示例**:

```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": {}
}
```

**数据库表**: `admins`

**业务逻辑**:

1. 验证旧密码
2. 新密码强度校验（至少 6 位，包含字母和数字）
3. 更新密码（BCrypt 加密）
4. 记录操作日志

**错误响应**:

```json
{
  "code": 1003,
  "message": "旧密码错误",
  "data": null
}
```

---

## 用户管理

### 1. 获取用户列表

**接口地址**: `GET /users`

**功能描述**: 获取用户列表，支持分页、筛选、搜索

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页数量，最大 100 |
| keyword | string | 否 | - | 搜索关键词（用户名/昵称/邮箱） |
| status | int | 否 | - | 状态筛选：1-正常/0-禁用 |
| startTime | string | 否 | - | 开始时间（ISO 8601） |
| endTime | string | 否 | - | 结束时间（ISO 8601） |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "username": "user1",
        "nickname": "美食达人 1",
        "email": "user1@example.com",
        "phone": "13800138000",
        "status": 1,
        "lastLoginTime": "2026-03-01T10:30:00",
        "createTime": "2026-01-01T00:00:00",
        "favoritesCount": 15,
        "commentsCount": 8
      }
    ],
    "total": 297479,
    "page": 1,
    "pageSize": 10
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| list | array | 用户列表 |
| list[].id | int | 用户 ID |
| list[].username | string | 用户名 |
| list[].nickname | string | 昵称 |
| list[].email | string | 邮箱 |
| list[].phone | string | 手机号 |
| list[].status | int | 状态：1-正常/0-禁用 |
| list[].lastLoginTime | datetime | 最后登录时间 |
| list[].createTime | datetime | 注册时间 |
| list[].favoritesCount | int | 收藏数 |
| list[].commentsCount | int | 评论数 |
| total | int | 总记录数 |
| page | int | 当前页码 |
| pageSize | int | 每页数量 |

**数据库表**: `users`, `interactions`, `comments`

**使用的索引**:
- `users.pk` (主键)
- `idx_users_status` (status 字段，状态筛选)
- `idx_users_email` (email 字段，邮箱搜索)
- `idx_users_username` (username 字段，用户名搜索)
- `idx_interaction_user` (统计收藏数)
- `idx_comment_user` (统计评论数)

**SQL 示例**:

```sql
SELECT u.id, u.username, u.nickname, u.email, u.phone, u.status,
       u.last_login_time, u.create_time,
       COUNT(DISTINCT i.id) as favorites_count,
       COUNT(DISTINCT c.id) as comments_count
FROM users u
LEFT JOIN interactions i ON u.id = i.user_id AND i.interaction_type = 'favorite'
LEFT JOIN comments c ON u.id = c.user_id
WHERE u.status = ?
  AND (u.username LIKE ? OR u.nickname LIKE ? OR u.email LIKE ?)
  AND u.create_time BETWEEN ? AND ?
GROUP BY u.id
ORDER BY u.create_time DESC
LIMIT 0, 10;
```

---

### 2. 获取用户详情

**接口地址**: `GET /users/{id}`

**功能描述**: 获取指定用户的详细信息

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int | 是 | 用户 ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "user1",
    "nickname": "美食达人 1",
    "email": "user1@example.com",
    "phone": "13800138000",
    "avatar": "https://example.com/avatar.jpg",
    "status": 1,
    "lastLoginTime": "2026-03-01T10:30:00",
    "createTime": "2026-01-01T00:00:00",
    "statistics": {
      "favoritesCount": 15,
      "commentsCount": 8,
      "likedCount": 120,
      "viewCount": 5000
    }
  }
}
```

**数据库表**: `users`, `interactions`, `comments`, `recipes`

**使用的索引**:
- `users.pk` (主键)
- `idx_interaction_user` (统计收藏)
- `idx_comment_user` (统计评论)

---

### 3. 创建用户

**接口地址**: `POST /users`

**功能描述**: 创建新用户

**请求头**:

```
Authorization: Bearer <admin_token>
```

**请求参数**:

```json
{
  "username": "newuser",
  "password": "123456",
  "nickname": "新用户",
  "email": "user@example.com",
  "phone": "13800138000"
}
```

**请求参数说明**:

| 参数 | 类型 | 必填 | 长度 | 说明 |
|------|------|------|------|------|
| username | string | 是 | 1-100 | 用户名（唯一） |
| password | string | 是 | 6-255 | 密码 |
| nickname | string | 否 | 0-100 | 昵称 |
| email | string | 否 | 0-100 | 邮箱 |
| phone | string | 否 | 0-20 | 手机号 |

**响应示例**:

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 297480,
    "username": "newuser",
    "createTime": "2026-03-01T12:00:00"
  }
}
```

**数据库表**: `users`

**使用的索引**: `uk_old_id` (唯一索引检查)

**业务逻辑**:

1. 检查 username 是否已存在
2. 检查 email 是否已存在
3. 密码加密（BCrypt）
4. 插入用户记录
5. 记录操作日志

**错误响应**:

```json
{
  "code": 1004,
  "message": "用户名已存在",
  "data": null
}
```

```json
{
  "code": 1005,
  "message": "邮箱已存在",
  "data": null
}
```

---

### 4. 更新用户

**接口地址**: `PUT /users/{id}`

**功能描述**: 更新用户信息

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int | 是 | 用户 ID |

**请求头**:

```
Authorization: Bearer <admin_token>
```

**请求参数**:

```json
{
  "nickname": "新昵称",
  "email": "new@example.com",
  "phone": "13900139000",
  "status": 1
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {}
}
```

**数据库表**: `users`

---

### 5. 删除用户

**接口地址**: `DELETE /users/{id}`

**功能描述**: 删除指定用户

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int | 是 | 用户 ID |

**请求头**:

```
Authorization: Bearer <admin_token>
```

**响应示例**:

```json
{
  "code": 200,
  "message": "删除成功",
  "data": {}
}
```

**数据库表**: `users`

**业务逻辑**:

1. 检查用户是否存在
2. 检查用户是否有未处理的评论
3. 软删除：更新 status=0
4. 记录操作日志

**注意**: 由于外键约束，删除用户会级联删除相关数据（评论、收藏等）

---

### 6. 批量删除用户

**接口地址**: `DELETE /users/batch`

**功能描述**: 批量删除用户

**请求头**:

```
Authorization: Bearer <admin_token>
```

**请求参数**:

```json
{
  "ids": [1, 2, 3]
}
```

**请求参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ids | array | 是 | 用户 ID 数组 |

**响应示例**:

```json
{
  "code": 200,
  "message": "批量删除成功",
  "data": {
    "successCount": 3,
    "failCount": 0
  }
}
```

**数据库表**: `users`

**业务逻辑**:

1. 遍历 ID 列表
2. 逐个执行删除操作
3. 返回成功和失败数量
4. 记录操作日志

---

### 7. 禁用/启用用户

**接口地址**: `PUT /users/{id}/status`

**功能描述**: 禁用或启用用户

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int | 是 | 用户 ID |

**请求头**:

```
Authorization: Bearer <admin_token>
```

**请求参数**:

```json
{
  "status": 0
}
```

**请求参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | int | 是 | 状态：1-启用/0-禁用 |

**响应示例**:

```json
{
  "code": 200,
  "message": "状态更新成功",
  "data": {}
}
```

**数据库表**: `users`

**业务逻辑**:

1. 检查用户是否存在
2. 更新 status 字段
3. 记录操作日志
4. 如果禁用用户，强制下线（使 Token 失效）

---

### 8. 重置用户密码

**接口地址**: `PUT /users/{id}/password`

**功能描述**: 管理员为用户重置密码

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int | 是 | 用户 ID |

**请求头**:

```
Authorization: Bearer <admin_token>
```

**请求参数**:

```json
{
  "password": "newpassword123"
}
```

**请求参数说明**:

| 参数 | 类型 | 必填 | 长度 | 说明 |
|------|------|------|------|------|
| password | string | 是 | 6-255 | 新密码 |

**响应示例**:

```json
{
  "code": 200,
  "message": "密码重置成功",
  "data": {}
}
```

**数据库表**: `users`

**业务逻辑**:

1. 检查用户是否存在
2. 密码强度校验
3. 密码加密（BCrypt）
4. 更新密码
5. 记录操作日志
6. 可选：发送通知邮件给用户

---

## 食谱管理

### 1. 获取食谱列表

**接口地址**: `GET /recipes`

**功能描述**: 获取食谱列表，支持分页、筛选、搜索

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页数量 |
| keyword | string | 否 | - | 搜索关键词（标题/作者） |
| categoryId | int | 否 | - | 分类 ID |
| tasteId | int | 否 | - | 口味 ID |
| techniqueId | int | 否 | - | 工艺 ID |
| timeCostId | int | 否 | - | 耗时 ID |
| difficultyId | int | 否 | - | 难度 ID |
| status | int | 否 | - | 状态筛选 |
| startTime | string | 否 | - | 开始时间 |
| endTime | string | 否 | - | 结束时间 |

**响应示例**:

```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "title": "红烧肉",
        "author": "美食达人",
        "image": "https://example.com/image1.jpg",
        "tasteId": 1,
        "tasteName": "咸鲜",
        "techniqueId": 5,
        "techniqueName": "炖",
        "timeCostId": 4,
        "timeCostName": "1 小时",
        "difficultyId": 2,
        "difficultyName": "中等",
        "replyCount": 89,
        "likeCount": 567,
        "ratingCount": 1234,
        "status": 1,
        "createTime": "2026-01-01T00:00:00"
      }
    ],
    "total": 309863,
    "page": 1,
    "pageSize": 10
  }
}
```

**数据库表**: `recipes`, `tastes`, `techniques`, `time_costs`, `difficulties`, `recipe_categories`

**使用的索引**:
- `idx_recipe_taste` (taste_id)
- `idx_recipe_technique` (technique_id)
- `idx_recipe_difficulty` (difficulty_id)
- `uk_old_id` (old_id)

---

### 2. 获取食谱详情

**接口地址**: `GET /recipes/{id}`

**功能描述**: 获取食谱详细信息

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int | 是 | 食谱 ID |

**响应示例**:

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "title": "红烧肉",
    "author": "美食达人",
    "authorUid": "12345",
    "image": "https://example.com/image1.jpg",
    "description": "美味的红烧肉",
    "tips": "烹饪技巧",
    "cookware": "炒锅",
    "tasteId": 1,
    "tasteName": "咸鲜",
    "techniqueId": 5,
    "techniqueName": "炖",
    "timeCostId": 4,
    "timeCostName": "1 小时",
    "difficultyId": 2,
    "difficultyName": "中等",
    "categories": [
      {"id": 1, "categoryId": 10, "name": "家常菜"},
      {"id": 2, "categoryId": 20, "name": "肉类"}
    ],
    "ingredients": [
      {
        "id": 1,
        "ingredientId": 100,
        "name": "五花肉",
        "type": "main",
        "typeName": "主料",
        "quantity": "500g"
      }
    ],
    "steps": [
      {
        "id": 1,
        "stepNumber": 1,
        "description": "准备食材",
        "image": "https://example.com/step1.jpg"
      }
    ],
    "replyCount": 89,
    "likeCount": 567,
    "ratingCount": 1234,
    "status": 1,
    "createTime": "2026-01-01T00:00:00",
    "updateTime": "2026-01-01T12:00:00"
  }
}
```

**数据库表**: 
- `recipes` (主表)
- `recipe_ingredients`, `ingredients` (食材)
- `cooking_steps` (步骤)
- `recipe_categories`, `categories` (分类)
- `tastes`, `techniques`, `time_costs`, `difficulties` (属性)

**使用的索引**:
- `recipes.pk` (主键)
- `recipe_ingredients.uk_recipe_ingredient` (唯一索引)
- `cooking_steps.fk_recipe` (外键)
- `recipe_categories.uk_recipe_category` (唯一索引)

---

### 3. 创建食谱

**接口地址**: `POST /recipes`

**功能描述**: 创建新食谱

**请求头**:

```
Authorization: Bearer <admin_token>
```

**请求参数**:

```json
{
  "title": "新菜品",
  "author": "作者名",
  "authorUid": "12345",
  "image": "https://example.com/image.jpg",
  "description": "菜品描述",
  "tips": "烹饪技巧",
  "cookware": "炒锅",
  "tasteId": 1,
  "techniqueId": 5,
  "timeCostId": 4,
  "difficultyId": 2,
  "categoryIds": [1, 2],
  "ingredients": [
    {
      "ingredientId": 100,
      "type": "main",
      "quantity": "500g"
    }
  ],
  "steps": [
    {
      "stepNumber": 1,
      "description": "第一步：准备食材",
      "image": "https://example.com/step1.jpg"
    }
  ]
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 309864,
    "title": "新菜品",
    "createTime": "2026-03-01T12:00:00"
  }
}
```

**数据库表**: 多个表（recipes、recipe_ingredients、cooking_steps、recipe_categories）

**事务说明**:

1. 开启事务
2. 插入 recipes 表，获取生成的 ID
3. 插入 recipe_ingredients 表
4. 插入 cooking_steps 表
5. 插入 recipe_categories 表
6. 更新属性表的 recipe_count
7. 提交事务
8. 记录操作日志

---

### 4. 更新食谱

**接口地址**: `PUT /recipes/{id}`

**功能描述**: 更新食谱信息

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int | 是 | 食谱 ID |

**请求参数**: 与创建食谱相同，所有字段都可更新

**响应示例**:

```json
{
  "code": 200,
  "message": "更新成功",
  "data": {}
}
```

**事务说明**:

1. 开启事务
2. 更新 recipes 表
3. 删除旧的 ingredients、steps、categories 关联
4. 插入新的关联数据
5. 更新属性表的 recipe_count
6. 提交事务
7. 记录操作日志

---

### 5. 删除食谱

**接口地址**: `DELETE /recipes/{id}`

**功能描述**: 删除指定食谱

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int | 是 | 食谱 ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "删除成功",
  "data": {}
}
```

**数据库表**: `recipes`

**级联删除**:
- `recipe_ingredients` (外键 ON DELETE CASCADE)
- `cooking_steps` (外键 ON DELETE CASCADE)
- `recipe_categories` (外键 ON DELETE CASCADE)
- `comments` (外键 ON DELETE CASCADE)
- `interactions` (外键 ON DELETE CASCADE)

---

### 6. 批量删除食谱

**接口地址**: `DELETE /recipes/batch`

**请求参数**:

```json
{
  "ids": [1, 2, 3]
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "批量删除成功",
  "data": {
    "successCount": 3,
    "failCount": 0
  }
}
```

---

### 7. 审核食谱

**接口地址**: `PUT /recipes/{id}/audit`

**功能描述**: 审核食谱（通过/拒绝）

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int | 是 | 食谱 ID |

**请求参数**:

```json
{
  "status": 1,
  "reason": "审核通过"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "审核成功",
  "data": {}
}
```

---

## 分类管理

### 1. 获取分类列表

**接口地址**: `GET /categories`

**响应示例**:

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "家常菜",
      "recipeCount": 1234,
      "createTime": "2026-01-01T00:00:00"
    }
  ]
}
```

**数据库表**: `categories`, `recipe_categories`

**使用的索引**: 
- `categories.pk` (主键)
- `categories.name` (唯一索引)
- `recipe_categories.fk_category` (统计数量)

---

### 2. 创建分类

**接口地址**: `POST /categories`

**请求参数**:

```json
{
  "name": "新分类"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 345,
    "name": "新分类",
    "createTime": "2026-03-01T12:00:00"
  }
}
```

**数据库表**: `categories`

**使用的索引**: `categories.name` (唯一索引检查)

---

### 3. 更新分类

**接口地址**: `PUT /categories/{id}`

**请求参数**:

```json
{
  "name": "新分类名"
}
```

---

### 4. 删除分类

**接口地址**: `DELETE /categories/{id}`

**业务逻辑**:

1. 检查分类是否存在
2. 检查分类下是否有食谱（通过 recipe_categories）
3. 如果有食谱关联，返回错误
4. 删除分类
5. 记录操作日志

**错误响应**:

```json
{
  "code": 2002,
  "message": "无法删除：该分类下还有 1234 个食谱",
  "data": null
}
```

---

## 属性管理

### 1. 获取属性列表

**接口地址**: 

- `GET /attributes/tastes` - 口味列表
- `GET /attributes/techniques` - 工艺列表
- `GET /attributes/time-costs` - 耗时列表
- `GET /attributes/difficulties` - 难度列表
- `GET /attributes/ingredients` - 食材列表

**响应示例** (所有属性接口返回格式相同):

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "咸鲜",
      "recipeCount": 120,
      "createTime": "2026-01-01T00:00:00"
    },
    {
      "id": 2,
      "name": "麻辣",
      "recipeCount": 95,
      "createTime": "2026-01-01T00:00:00"
    }
  ]
}
```

**数据库表**: `tastes`, `techniques`, `time_costs`, `difficulties`, `ingredients`

**使用的索引**: 
- 各表的 pk (主键)
- 各表的 name (唯一索引)

---

### 2. 创建属性

**接口地址**: 

- `POST /attributes/tastes`
- `POST /attributes/techniques`
- `POST /attributes/time-costs`
- `POST /attributes/difficulties`
- `POST /attributes/ingredients`

**请求参数**:

```json
{
  "name": "属性名称"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 6,
    "name": "新属性",
    "recipeCount": 0,
    "createTime": "2026-03-01T12:00:00"
  }
}
```

---

### 3. 更新属性

**接口地址**: 

- `PUT /attributes/tastes/{id}`
- `PUT /attributes/techniques/{id}`
- `PUT /attributes/time-costs/{id}`
- `PUT /attributes/difficulties/{id}`
- `PUT /attributes/ingredients/{id}`

**请求参数**:

```json
{
  "name": "新属性名称"
}
```

---

### 4. 删除属性

**接口地址**: 

- `DELETE /attributes/tastes/{id}`
- `DELETE /attributes/techniques/{id}`
- `DELETE /attributes/time-costs/{id}`
- `DELETE /attributes/difficulties/{id}`
- `DELETE /attributes/ingredients/{id}`

**前置检查**: 

后端检查该属性是否被食谱使用（通过 recipe_count 字段）

**成功响应**:

```json
{
  "code": 200,
  "message": "删除成功",
  "data": {}
}
```

**失败响应**:

```json
{
  "code": 400,
  "message": "无法删除：该属性已被 120 个食谱使用",
  "data": null
}
```

---

## 统计管理

### 1. 获取统计数据概览

**接口地址**: `GET /statistics/overview`

**功能描述**: 获取平台整体统计数据概览

**请求头**:
```
Authorization: Bearer <admin_token>
```

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalUsers": 297479,
    "totalRecipes": 309863,
    "totalCategories": 344,
    "totalComments": 2048720,
    "todayViews": 12345,
    "todayNewUsers": 56,
    "todayNewRecipes": 12
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| totalUsers | int | 总用户数 |
| totalRecipes | int | 总食谱数 |
| totalCategories | int | 总分类数 |
| totalComments | int | 总评论数 |
| todayViews | int | 今日浏览量（基于 interactions 表今日数据） |
| todayNewUsers | int | 今日新增用户数 |
| todayNewRecipes | int | 今日新增食谱数 |

**数据库表**: `users`, `recipes`, `categories`, `comments`, `interactions`

**使用的索引**:
- 各表的 pk (主键)
- `interactions.idx_create_time` (统计今日浏览量)
- `users.idx_create_time` (统计今日新增用户)
- `recipes.idx_create_time` (统计今日新增食谱)

**SQL 示例**:

```sql
-- 总用户数
SELECT COUNT(*) FROM users;

-- 总食谱数
SELECT COUNT(*) FROM recipes;

-- 总分类数
SELECT COUNT(*) FROM categories;

-- 总评论数
SELECT COUNT(*) FROM comments;

-- 今日浏览量
SELECT COUNT(*) FROM interactions WHERE DATE(create_time) = CURDATE();

-- 今日新增用户
SELECT COUNT(*) FROM users WHERE DATE(create_time) = CURDATE();

-- 今日新增食谱
SELECT COUNT(*) FROM recipes WHERE DATE(create_time) = CURDATE();
```

---

### 2. 用户统计数据

**接口地址**: `GET /statistics/users`

**功能描述**: 获取用户相关统计数据，包括新增趋势等

**请求头**:
```
Authorization: Bearer <admin_token>
```

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 否 | 统计类型：daily/weekly/monthly（当前默认返回近 7 天数据） |
| startTime | string | 否 | 开始时间（ISO 8601 格式） |
| endTime | string | 否 | 结束时间（ISO 8601 格式） |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "trend": [
      {"date": "2026-02-24", "count": 45},
      {"date": "2026-02-25", "count": 52},
      {"date": "2026-02-26", "count": 48},
      {"date": "2026-02-27", "count": 55},
      {"date": "2026-02-28", "count": 60},
      {"date": "2026-03-01", "count": 50},
      {"date": "2026-03-02", "count": 65}
    ]
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| trend | array | 用户趋势数据 |
| trend[].date | string | 日期（YYYY-MM-DD 格式） |
| trend[].count | int | 新增用户数 |

**数据库表**: `users`

**使用的索引**:
- `users.pk` (主键)
- `users.idx_create_time` (按时间分组统计)

**SQL 示例**:

```sql
-- 近 7 天新增用户趋势
SELECT DATE_FORMAT(create_time, '%Y-%m-%d') as date, COUNT(*) as count
FROM users
WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
ORDER BY date;
```

---

### 3. 食谱统计数据

**接口地址**: `GET /statistics/recipes`

**功能描述**: 获取食谱相关统计数据，包括趋势、分类分布、难度分布、热门食谱等

**请求头**:
```
Authorization: Bearer <admin_token>
```

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 否 | 统计类型 |
| startTime | string | 否 | 开始时间 |
| endTime | string | 否 | 结束时间 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "trend": [
      {"date": "2026-02-24", "count": 8},
      {"date": "2026-02-25", "count": 12},
      {"date": "2026-02-26", "count": 10},
      {"date": "2026-02-27", "count": 15},
      {"date": "2026-02-28", "count": 11},
      {"date": "2026-03-01", "count": 9},
      {"date": "2026-03-02", "count": 14}
    ],
    "categoryDistribution": [
      {"name": "家常菜", "value": 45678},
      {"name": "川菜", "value": 32456},
      {"name": "粤菜", "value": 28934},
      {"name": "面食", "value": 25678},
      {"name": "汤粥", "value": 22345}
    ],
    "difficultyDistribution": [
      {"name": "简单", "value": 150000},
      {"name": "中等", "value": 120000},
      {"name": "困难", "value": 39863}
    ],
    "topRecipes": [
      {
        "id": 1,
        "title": "红烧肉",
        "viewCount": 125678,
        "likeCount": 45678
      },
      {
        "id": 2,
        "title": "宫保鸡丁",
        "viewCount": 98765,
        "likeCount": 38765
      }
    ]
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| trend | array | 食谱新增趋势 |
| trend[].date | string | 日期 |
| trend[].count | int | 新增食谱数 |
| categoryDistribution | array | 分类分布（Top 10） |
| categoryDistribution[].name | string | 分类名称 |
| categoryDistribution[].value | int | 食谱数量 |
| difficultyDistribution | array | 难度分布 |
| difficultyDistribution[].name | string | 难度名称 |
| difficultyDistribution[].value | int | 食谱数量 |
| topRecipes | array | 热门食谱 Top 10 |
| topRecipes[].id | int | 食谱 ID |
| topRecipes[].title | string | 食谱标题 |
| topRecipes[].viewCount | int | 浏览量 |
| topRecipes[].likeCount | int | 点赞数 |

**数据库表**: `recipes`, `categories`, `recipe_categories`, `difficulties`

**使用的索引**:
- `recipes.pk` (主键)
- `recipes.idx_create_time` (趋势统计)
- `recipe_categories.fk_category` (分类分布)
- `recipes.idx_recipe_difficulty` (难度分布)
- `recipes.idx_like_count` (热门食谱排序)

**SQL 示例**:

```sql
-- 近 7 天新增食谱趋势
SELECT DATE_FORMAT(create_time, '%Y-%m-%d') as date, COUNT(*) as count
FROM recipes
WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
ORDER BY date;

-- 分类分布（Top 10）
SELECT c.name, COUNT(rc.recipe_id) as value
FROM categories c
LEFT JOIN recipe_categories rc ON c.id = rc.category_id
GROUP BY c.id, c.name
ORDER BY value DESC
LIMIT 10;

-- 难度分布
SELECT d.name, COUNT(r.id) as value
FROM difficulties d
LEFT JOIN recipes r ON d.id = r.difficulty_id
GROUP BY d.id, d.name
ORDER BY d.id;

-- 热门食谱 Top 10
SELECT r.id, r.title, r.view_count, r.like_count
FROM recipes r
ORDER BY r.like_count DESC
LIMIT 10;
```

---

### 4. 评论统计数据

**接口地址**: `GET /statistics/comments`

**功能描述**: 获取评论相关统计数据，包括评论趋势、热门评论食谱等

**请求头**:
```
Authorization: Bearer <admin_token>
```

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 否 | 统计类型 |
| startTime | string | 否 | 开始时间 |
| endTime | string | 否 | 结束时间 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "commentsTrend": [
      {"date": "2026-02-24", "count": 85},
      {"date": "2026-02-25", "count": 92},
      {"date": "2026-02-26", "count": 88},
      {"date": "2026-02-27", "count": 105},
      {"date": "2026-02-28", "count": 110},
      {"date": "2026-03-01", "count": 95},
      {"date": "2026-03-02", "count": 120}
    ],
    "topCommentedRecipes": [
      {
        "id": 1,
        "title": "红烧肉",
        "commentCount": 2345
      },
      {
        "id": 2,
        "title": "宫保鸡丁",
        "commentCount": 1876
      }
    ]
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| commentsTrend | array | 评论趋势数据 |
| commentsTrend[].date | string | 日期 |
| commentsTrend[].count | int | 评论数 |
| topCommentedRecipes | array | 评论最多的食谱 Top 10 |
| topCommentedRecipes[].id | int | 食谱 ID |
| topCommentedRecipes[].title | string | 食谱标题 |
| topCommentedRecipes[].commentCount | int | 评论数 |

**数据库表**: `comments`, `recipes`

**使用的索引**:
- `comments.pk` (主键)
- `comments.idx_create_time` (趋势统计)
- `comments.idx_comment_recipe` (按食谱统计评论)
- `recipes.pk` (关联查询)

**SQL 示例**:

```sql
-- 近 7 天评论趋势
SELECT DATE_FORMAT(create_time, '%Y-%m-%d') as date, COUNT(*) as count
FROM comments
WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
ORDER BY date;

-- 评论最多的食谱 Top 10
SELECT r.id, r.title, COUNT(c.id) as commentCount
FROM recipes r
LEFT JOIN comments c ON r.id = c.recipe_id
GROUP BY r.id, r.title
ORDER BY commentCount DESC
LIMIT 10;
```

---

## 日志管理

### 1. 获取系统日志列表

**接口地址**: `GET /logs`

**功能描述**: 获取管理员操作日志

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页数量 |
| adminId | int | 否 | - | 管理员 ID |
| module | string | 否 | - | 模块 |
| operation | string | 否 | - | 操作类型 |
| startTime | string | 否 | - | 开始时间 |
| endTime | string | 否 | - | 结束时间 |

**响应示例**:

```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "adminId": 1,
        "adminName": "admin",
        "operation": "DELETE_USER",
        "module": "用户管理",
        "content": "删除用户 user1 (ID: 1)",
        "ip": "192.168.1.1",
        "userAgent": "Mozilla/5.0...",
        "createTime": "2026-03-01T10:30:00"
      }
    ],
    "total": 1000,
    "page": 1,
    "pageSize": 10
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| list | array | 日志列表 |
| list[].id | int | 日志 ID |
| list[].adminId | int | 管理员 ID |
| list[].adminName | string | 管理员名称 |
| list[].operation | string | 操作类型 |
| list[].module | string | 模块名称 |
| list[].content | string | 操作内容 |
| list[].ip | string | 操作 IP |
| list[].userAgent | string | 浏览器信息 |
| list[].createTime | datetime | 操作时间 |
| total | int | 总记录数 |
| page | int | 当前页码 |
| pageSize | int | 每页数量 |

**数据库表**: `system_logs`, `admins`

**使用的索引**:
- `system_logs.pk` (主键)
- `idx_admin_id` (admin_id，按管理员筛选)
- `idx_module` (module，按模块筛选)
- `idx_create_time` (create_time，时间排序)

**SQL 示例**:

```sql
SELECT sl.id, sl.admin_id, a.username as admin_name,
       sl.operation, sl.module, sl.content, sl.ip,
       sl.user_agent, sl.create_time
FROM system_logs sl
LEFT JOIN admins a ON sl.admin_id = a.id
WHERE sl.module = ?
  AND sl.create_time BETWEEN ? AND ?
ORDER BY sl.create_time DESC
LIMIT 0, 10;
```

---

## 错误码说明

### 通用错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权，登录已过期 |
| 403 | 禁止访问，无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 认证错误码

| 错误码 | 说明 |
|--------|------|
| 1001 | 用户名或密码错误 |
| 1002 | 管理员账号已被禁用 |
| 1003 | 旧密码错误 |

### 用户管理错误码

| 错误码 | 说明 |
|--------|------|
| 1004 | 用户名已存在 |
| 1005 | 邮箱已存在 |
| 1006 | 用户不存在 |

### 食谱管理错误码

| 错误码 | 说明 |
|--------|------|
| 2001 | 食谱不存在 |
| 2002 | 分类不存在 |
| 2003 | 属性不存在 |
| 2004 | 无法删除：有关联数据 |

### 分类管理错误码

| 错误码 | 说明 |
|--------|------|
| 2002 | 分类下还有食谱，无法删除 |

### 属性管理错误码

| 错误码 | 说明 |
|--------|------|
| 2005 | 属性已被使用，无法删除 |

---

## 附录

### A. 数据库索引汇总

| 表名 | 索引名 | 字段 | 类型 | 说明 |
|------|--------|------|------|------|
| admins | pk | id | 主键 | 管理员 ID |
| admins | uk_username | username | 唯一索引 | 管理员名唯一 |
| system_logs | pk | id | 主键 | 日志 ID |
| system_logs | idx_admin_id | admin_id | 外键索引 | 按管理员查询 |
| system_logs | idx_module | module | 普通索引 | 按模块查询 |
| system_logs | idx_create_time | create_time | 普通索引 | 时间排序 |
| users | pk | id | 主键 | 用户 ID |
| users | uk_old_id | old_id | 唯一索引 | 原始用户 ID 映射 |
| users | idx_users_status | status | 普通索引 | 状态筛选 |
| users | idx_users_email | email | 普通索引 | 邮箱查询 |
| users | idx_users_username | username | 普通索引 | 用户名查询 |
| recipes | pk | id | 主键 | 食谱 ID |
| recipes | uk_old_id | old_id | 唯一索引 | 原始食谱 ID 映射 |
| recipes | idx_recipe_taste | taste_id | 普通索引 | 口味筛选 |
| recipes | idx_recipe_technique | technique_id | 普通索引 | 工艺筛选 |
| recipes | idx_recipe_difficulty | difficulty_id | 普通索引 | 难度筛选 |
| categories | pk | id | 主键 | 分类 ID |
| categories | uk_name | name | 唯一索引 | 分类名唯一 |

### B. 操作类型枚举

| 操作代码 | 说明 | 模块 |
|---------|------|------|
| LOGIN | 登录 | 认证 |
| LOGOUT | 登出 | 认证 |
| UPDATE_PASSWORD | 修改密码 | 认证 |
| CREATE_USER | 创建用户 | 用户管理 |
| UPDATE_USER | 更新用户 | 用户管理 |
| DELETE_USER | 删除用户 | 用户管理 |
| RESET_PASSWORD | 重置密码 | 用户管理 |
| CREATE_RECIPE | 创建食谱 | 食谱管理 |
| UPDATE_RECIPE | 更新食谱 | 食谱管理 |
| DELETE_RECIPE | 删除食谱 | 食谱管理 |
| AUDIT_RECIPE | 审核食谱 | 食谱管理 |
| CREATE_CATEGORY | 创建分类 | 分类管理 |
| UPDATE_CATEGORY | 更新分类 | 分类管理 |
| DELETE_CATEGORY | 删除分类 | 分类管理 |
| CREATE_ATTRIBUTE | 创建属性 | 属性管理 |
| UPDATE_ATTRIBUTE | 更新属性 | 属性管理 |
| DELETE_ATTRIBUTE | 删除属性 | 属性管理 |

---

**文档维护**: 开发团队  
**最后更新**: 2026-03-01  
**版本**: v2.0
