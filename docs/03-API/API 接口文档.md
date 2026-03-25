# 美食推荐系统 - API 接口文档

> **版本**: v2.4  
> **最后更新**: 2026-03-19 13:07:08  
> **基准 URL**: `http://localhost:8081/api`  
> **数据格式**: JSON  
> **字符编码**: UTF-8

## 2026-03-19 增量说明

- 本地开发默认后端端口为 `8081`。
- 搜索接口当前支持两套后端实现：
  - `search.engine=mysql`：沿用 MySQL 多字段混合检索
  - `search.engine=elasticsearch`：切换到 Elasticsearch 检索，再回 MySQL 补全展示字段
- 搜索能力当前已升级为 Search V2：
  - `GET /recipes/search` 新增 `sort` 参数，支持 `relevance`、`hot`、`new`
  - Elasticsearch 路径使用 `smartcn` 中文分词
  - 核心召回覆盖标题、作者、食材、分类
  - 意图字段（口味、做法、耗时、难度）仅在核心召回无结果时作为 fallback
  - 新增 `GET /recipes/search/suggestions`
- Elasticsearch 建议词已改为独立 completion 字段，不再复用主搜索字段
- 当前本地运行态已完成 `recipes_search_v2` 全量重建并切到 `elasticsearch`；仓库默认配置仍保留 `mysql` 作为首次部署默认值
- 以下列表型接口返回项已统一补充作者字段：
  - `GET /recipes`
  - `GET /recipes/search`
  - `GET /recipes/recommend`
  - `GET /recipes/{id}/similar`
- 推荐接口当前支持“基于现有分类的个性化筛选”：
  - `GET /recipes/recommend` 新增可选 `categoryId`
  - 推荐页前端已从硬编码场景切换为现有分类筛选
  - 当前前端默认展示的分类包括 `家常菜 / 快手菜 / 减肥瘦身 / 宴客菜 / 夜宵 / 下饭菜 / 儿童 / 早餐`
- 上述返回项新增字段：
  - `author`：作者名称
  - `authorUid`：作者用户 ID
- 本次更新同时对推荐/相似菜谱查询、统计缓存与启动性能做了实现优化，但不改变接口调用方式。

---

## 目录

1. [通用说明](#通用说明)
2. [用户模块](#用户模块)
3. [菜谱模块](#菜谱模块)
4. [分类模块](#分类模块)
5. [收藏模块](#收藏模块)
6. [评论模块](#评论模块)
7. [管理员模块](#管理员模块)
8. [错误码说明](#错误码说明)

---

## 通用说明

### 统一响应格式

所有接口返回统一使用以下格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

**响应字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，200 表示成功 |
| message | string | 响应消息 |
| data | object/array | 响应数据 |

### 认证说明

需要登录的接口需要在请求头中携带 Token：

```
Authorization: Bearer <token>
```

Token 通过用户登录接口获取，使用 JWT 格式。

### 分页参数

列表接口统一使用以下分页参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页数量，最大 100 |

### 数据库索引说明

为提高查询效率，以下接口使用了数据库索引：

| 接口 | 使用的索引 | 说明 |
|------|-----------|------|
| 获取菜谱列表 | idx_recipe_taste, idx_recipe_technique, idx_recipe_difficulty | 按口味、工艺、难度筛选 |
| 获取评论列表 | idx_comment_recipe, idx_comment_user | 按菜谱 ID、用户 ID 查询 |
| 获取收藏列表 | idx_interaction_user, idx_interaction_recipe | 按用户 ID、菜谱 ID 查询 |

---

## 用户模块

### 1. 用户登录

**接口地址**: `POST /users/login`

**功能描述**: 用户登录，返回 JWT Token

**请求参数**:

```json
{
  "username": "string",
  "password": "string"
}
```

**请求参数说明**:

| 参数 | 类型 | 必填 | 长度 | 说明 |
|------|------|------|------|------|
| username | string | 是 | 1-100 | 用户名 |
| password | string | 是 | 6-255 | 密码 |

**响应示例**:

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "testuser",
      "nickname": "美食爱好者",
      "avatar": "https://example.com/avatar.jpg",
      "email": "test@example.com"
    }
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| token | string | JWT 令牌，后续请求需携带 |
| user.id | int | 用户 ID |
| user.username | string | 用户名 |
| user.nickname | string | 昵称 |
| user.avatar | string | 头像 URL |
| user.email | string | 邮箱 |

**数据库表**: `users`

**使用的索引**: `uk_old_id` (唯一索引)

---

### 2. 用户注册

**接口地址**: `POST /users/register`

**功能描述**: 新用户注册

**请求参数**:

```json
{
  "username": "string",
  "password": "string",
  "nickname": "string",
  "email": "string"
}
```

**请求参数说明**:

| 参数 | 类型 | 必填 | 长度 | 说明 |
|------|------|------|------|------|
| username | string | 是 | 1-100 | 用户名（唯一） |
| password | string | 是 | 6-255 | 密码 |
| nickname | string | 否 | 0-100 | 昵称 |
| email | string | 否 | 0-100 | 邮箱 |

**响应示例**:

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 1,
    "username": "newuser",
    "createTime": "2026-03-01T10:00:00"
  }
}
```

**数据库表**: `users`

**使用的索引**: `uk_old_id` (唯一索引)

---

### 3. 获取用户信息

**接口地址**: `GET /users/profile`

**功能描述**: 获取当前登录用户的详细信息

**请求头**:

```
Authorization: Bearer <token>
```

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "nickname": "美食爱好者",
    "avatar": "https://example.com/avatar.jpg",
    "email": "test@example.com",
    "createTime": "2026-01-01T00:00:00",
    "favoritesCount": 15,
    "commentsCount": 8
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int | 用户 ID |
| username | string | 用户名 |
| nickname | string | 昵称 |
| avatar | string | 头像 URL |
| email | string | 邮箱 |
| createTime | datetime | 注册时间 |
| favoritesCount | int | 收藏数量 |
| commentsCount | int | 评论数量 |

**数据库表**: `users`, `interactions`, `comments`

**使用的索引**: 
- `users.pk` (主键)
- `idx_interaction_user` (统计收藏数)
- `idx_comment_user` (统计评论数)

---

### 4. 更新用户信息

**接口地址**: `PUT /users/profile`

**功能描述**: 更新当前用户信息

**请求头**:

```
Authorization: Bearer <token>
```

**请求参数**:

```json
{
  "nickname": "string",
  "email": "string",
  "avatar": "string"
}
```

**请求参数说明**:

| 参数 | 类型 | 必填 | 长度 | 说明 |
|------|------|------|------|------|
| nickname | string | 否 | 0-100 | 昵称 |
| email | string | 否 | 0-100 | 邮箱 |
| avatar | string | 否 | 0-255 | 头像 URL |

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

## 菜谱模块

### 1. 获取菜谱列表

**接口地址**: `GET /recipes`

**功能描述**: 获取菜谱列表，支持分页、筛选、排序

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页数量 |
| categoryId | int | 否 | - | 分类 ID |
| tasteId | int | 否 | - | 口味 ID |
| techniqueId | int | 否 | - | 工艺 ID |
| timeCostId | int | 否 | - | 耗时 ID |
| difficultyId | int | 否 | - | 难度 ID |
| sort | string | 否 | new | 排序：new-最新/hot-最热/like-最多点赞 |
| keyword | string | 否 | - | 搜索关键词 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "title": "红烧肉",
        "author": "美食达人",
        "image": "https://example.com/recipe1.jpg",
        "description": "美味的红烧肉",
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
        "createTime": "2026-01-01T00:00:00"
      }
    ],
    "total": 309863,
    "page": 1,
    "pageSize": 10
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| list | array | 菜谱列表 |
| list[].id | int | 菜谱 ID |
| list[].title | string | 菜谱标题 |
| list[].author | string | 作者 |
| list[].image | string | 封面图 URL |
| list[].description | string | 简介 |
| list[].tasteId | int | 口味 ID |
| list[].tasteName | string | 口味名称 |
| list[].techniqueId | int | 工艺 ID |
| list[].techniqueName | string | 工艺名称 |
| list[].timeCostId | int | 耗时 ID |
| list[].timeCostName | string | 耗时名称 |
| list[].difficultyId | int | 难度 ID |
| list[].difficultyName | string | 难度名称 |
| list[].replyCount | int | 评论数 |
| list[].likeCount | int | 点赞数 |
| list[].ratingCount | int | 浏览数 |
| list[].createTime | datetime | 创建时间 |
| total | int | 总记录数 |
| page | int | 当前页码 |
| pageSize | int | 每页数量 |

**数据库表**: `recipes`, `tastes`, `techniques`, `time_costs`, `difficulties`, `recipe_categories`

**使用的索引**:
- `idx_recipe_taste` (taste_id)
- `idx_recipe_technique` (technique_id)
- `idx_recipe_difficulty` (difficulty_id)
- `uk_old_id` (old_id)

**SQL 示例**:

```sql
SELECT r.*, t.name as taste_name, tec.name as technique_name, 
       tc.name as time_cost_name, d.name as difficulty_name
FROM recipes r
LEFT JOIN tastes t ON r.taste_id = t.id
LEFT JOIN techniques tec ON r.technique_id = tec.id
LEFT JOIN time_costs tc ON r.time_cost_id = tc.id
LEFT JOIN difficulties d ON r.difficulty_id = d.id
WHERE r.difficulty_id = ?
ORDER BY r.create_time DESC
LIMIT 0, 10;
```

---

### 2. 获取菜谱详情

**接口地址**: `GET /recipes/{id}`

**功能描述**: 获取菜谱详细信息，包括食材、步骤、分类等

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | int | 是 | 菜谱 ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "oldId": 10,
    "title": "红烧肉",
    "author": "美食达人",
    "authorUid": "12345",
    "image": "https://example.com/recipe1.jpg",
    "description": "美味的红烧肉",
    "tips": "烹饪技巧：火候要适中",
    "cookware": "炒锅、砂锅",
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
    "createTime": "2026-01-01T00:00:00",
    "updateTime": "2026-01-01T12:00:00",
    "ingredients": [
      {
        "id": 1,
        "ingredientId": 100,
        "name": "五花肉",
        "type": "main",
        "typeName": "主料",
        "quantity": "500g"
      },
      {
        "id": 2,
        "ingredientId": 101,
        "name": "酱油",
        "type": "seasoning",
        "typeName": "调料",
        "quantity": "适量"
      }
    ],
    "steps": [
      {
        "id": 1,
        "stepNumber": 1,
        "description": "准备食材，五花肉切块",
        "image": "https://example.com/step1.jpg"
      },
      {
        "id": 2,
        "stepNumber": 2,
        "description": "热锅凉油，放入五花肉翻炒",
        "image": "https://example.com/step2.jpg"
      }
    ],
    "categories": [
      {
        "id": 1,
        "categoryId": 10,
        "name": "家常菜"
      },
      {
        "id": 2,
        "categoryId": 20,
        "name": "肉类"
      }
    ]
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int | 菜谱 ID |
| oldId | int | 原始菜谱 ID |
| title | string | 菜谱标题 |
| author | string | 作者名称 |
| authorUid | string | 作者用户 ID |
| image | string | 封面图 URL |
| description | string | 菜谱描述 |
| tips | string | 烹饪小贴士 |
| cookware | string | 所需厨具 |
| tasteId | int | 口味 ID |
| tasteName | string | 口味名称 |
| techniqueId | int | 工艺 ID |
| techniqueName | string | 工艺名称 |
| timeCostId | int | 耗时 ID |
| timeCostName | string | 耗时名称 |
| difficultyId | int | 难度 ID |
| difficultyName | string | 难度名称 |
| replyCount | int | 评论数 |
| likeCount | int | 点赞数 |
| ratingCount | int | 浏览数 |
| createTime | datetime | 创建时间 |
| updateTime | datetime | 更新时间 |
| ingredients | array | 食材列表 |
| ingredients[].id | int | 关联 ID |
| ingredients[].ingredientId | int | 食材 ID |
| ingredients[].name | string | 食材名称 |
| ingredients[].type | enum | 类型：main/sub/seasoning |
| ingredients[].typeName | string | 类型名称：主料/辅料/调料 |
| ingredients[].quantity | string | 用量 |
| steps | array | 烹饪步骤列表 |
| steps[].id | int | 步骤 ID |
| steps[].stepNumber | int | 步骤序号 |
| steps[].description | string | 步骤描述 |
| steps[].image | string | 步骤图片 URL |
| categories | array | 分类列表 |
| categories[].id | int | 关联 ID |
| categories[].categoryId | int | 分类 ID |
| categories[].name | string | 分类名称 |

**数据库表**: 
- `recipes` (主表)
- `recipe_ingredients` (食材关联)
- `ingredients` (食材表)
- `cooking_steps` (步骤表)
- `recipe_categories` (分类关联)
- `categories` (分类表)
- `tastes`, `techniques`, `time_costs`, `difficulties` (属性表)

**使用的索引**:
- `recipes.pk` (主键)
- `recipe_ingredients.uk_recipe_ingredient` (唯一索引)
- `cooking_steps.fk_recipe` (外键)
- `recipe_categories.uk_recipe_category` (唯一索引)

**查询说明**:

1. 主查询：从 `recipes` 表获取基本信息
2. 关联查询 1：从 `recipe_ingredients` 和 `ingredients` 获取食材信息
3. 关联查询 2：从 `cooking_steps` 获取烹饪步骤
4. 关联查询 3：从 `recipe_categories` 和 `categories` 获取分类信息
5. 关联查询 4：从属性表获取口味、工艺、耗时、难度名称

---

### 3. 搜索菜谱

**接口地址**: `GET /recipes/search`

**功能描述**: 根据关键词搜索菜谱，支持多字段混合检索与排序

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| keyword | string | 否 | - | 搜索关键词 |
| sort | string | 否 | relevance | 排序方式：`relevance`-综合相关、`hot`-热门优先、`new`-最新发布 |
| page | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 12 | 每页数量 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "name": "红烧肉",
        "author": "美食达人",
        "image": "https://example.com/recipe1.jpg",
        "difficulty": "普通",
        "time": "30分钟",
        "taste": "家常",
        "likeCount": 156,
        "favoriteCount": 48,
        "replyCount": 12,
        "categories": ["家常菜", "肉类"],
        "ingredients": ["五花肉", "冰糖"]
      }
    ],
    "total": 156,
    "page": 1,
    "pageSize": 12
  }
}
```

**检索范围**:

- `recipes.title`
- `recipes.author`
- `ingredients.name`
- `categories.name`
- `tastes.name`
- `techniques.name`
- `time_costs.name`
- `difficulties.name`

**数据库表**: `recipes`, `recipe_ingredients`, `ingredients`, `recipe_categories`, `categories`, `tastes`, `techniques`, `time_costs`, `difficulties`

**说明**:

- `sort=relevance` 时，后端会基于标题完全匹配、标题前缀、作者、分类、食材等信号进行轻量评分。
- 空关键词会返回空列表，不报错。
- 当前返回列表项中的菜名字段为 `name`。

**实现说明**:

- 当前实现支持通过 `search.engine` 在 MySQL 与 Elasticsearch 之间切换。
- 默认仓库配置仍为 MyBatis 注解 SQL + `LIKE` / `EXISTS` 子查询的轻量混合搜索。
- 当启用 Elasticsearch 时，搜索接口会先在索引别名 `recipes_search` 上检索，再回 MySQL 按命中顺序补全菜谱详情。
- Search V2 的 ES 查询策略为：
  - `combined_fields(title, ingredients, categories, author)` + `operator=and` 做核心召回
  - `title.keyword / ingredients.keyword` 精确命中和 `match_phrase` 做精排
  - `tasteName / techniqueName / timeCostName / difficultyName` 仅在核心召回无结果时参与 fallback
- 当前这套实现已经规避了“`草鱼` 被 `鱼腥草` 等单字命中污染结果”的主要问题。
- 本轮未引入向量检索、大模型查询改写或语义搜索。

#### 搜索建议词

**接口地址**: `GET /recipes/search/suggestions`

**功能描述**: 根据输入内容返回搜索建议词

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| keyword | string | 否 | - | 当前输入内容 |
| limit | int | 否 | 8 | 返回数量上限 |

**实现说明**:

- Elasticsearch 路径下，建议词来自独立的 completion 字段：
  - `titleSuggest`
  - `ingredientSuggest`
  - `categorySuggest`
  - `authorSuggest`
- 当前服务端会按类型配额合并并去重，避免标题建议词淹没食材/分类/作者建议词。

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "value": "鸡胸肉",
      "type": "ingredient",
      "typeLabel": "食材"
    },
    {
      "value": "家常菜",
      "type": "category",
      "typeLabel": "分类"
    }
  ]
}
```

**返回说明**:

- `type` 可能值：`title`、`ingredient`、`category`、`author`
- 返回结果已按来源优先级与去重规则聚合
- 空关键词返回空数组，不报错
- 当启用 Elasticsearch 时，建议词来源于索引中已写入的标题、食材、分类、作者字段

---

### 4. 获取推荐菜谱

**接口地址**: `GET /recipes/recommend`

**功能描述**: 获取推荐菜谱列表

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | int | 否 | 16 | 返回数量 |
| type | string | 否 | personal | 推荐类型：personal-个性化/hot-热门/new-最新 |
| categoryId | int | 否 | - | 分类 ID，用于在推荐页按现有分类筛选 |
| scene | string | 否 | - | 兼容旧版场景筛选参数，当前推荐页默认不再使用 |

**请求头** (可选):

```
Authorization: Bearer <token>
```

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reason": "根据你的偏好推荐（分类：减肥瘦身）",
    "list": [
      {
        "id": 1,
        "name": "凉拌莴笋丝",
        "author": "厨友小李",
        "authorUid": "usr_1024",
        "image": "https://example.com/recipe1.jpg",
        "difficulty": "简单",
        "time": "十分钟",
        "taste": "咸鲜",
        "likeCount": 567,
        "favoriteCount": 120,
        "replyCount": 16,
        "categories": ["减肥瘦身", "凉菜"],
        "ingredients": ["莴笋", "蒜末"],
        "sceneTags": ["减脂", "快手"],
        "reasons": ["符合你的饮食目标：减脂", "属于你选择的分类：减肥瘦身"]
      }
    ]
  }
}
```

**数据库表**: `recipes`

**使用的索引**: 
- `idx_recipes_status_create_time` (最新推荐)
- `idx_recipes_status_like_count` (热门推荐)
- `idx_recipe_categories_composite` / `uk_recipe_category` (分类筛选)

**推荐算法**:

1. **热门推荐**: 按 `like_count` 降序排列
2. **最新推荐**: 按 `create_time` 降序排列
3. **个性化推荐** (登录用户): 根据用户浏览历史、收藏、行为埋点与偏好重排
4. **分类筛选推荐**: 当传入 `categoryId` 时，会先扩大候选池，再按分类过滤并补齐结果，避免出现有效分类却空列表

---

## 分类模块

### 1. 获取分类列表

**接口地址**: `GET /categories`

**功能描述**: 获取所有菜谱分类

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "家常菜",
      "recipeCount": 1234,
      "createTime": "2026-01-01T00:00:00"
    },
    {
      "id": 2,
      "name": "川菜",
      "recipeCount": 567,
      "createTime": "2026-01-01T00:00:00"
    }
  ]
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int | 分类 ID |
| name | string | 分类名称 |
| recipeCount | int | 该分类下的菜谱数量 |
| createTime | datetime | 创建时间 |

**数据库表**: `categories`, `recipe_categories`

**使用的索引**: 
- `categories.pk` (主键)
- `categories.name` (唯一索引)
- `recipe_categories.fk_category` (统计数量时使用)

**SQL 示例**:

```sql
SELECT c.id, c.name, c.create_time, COUNT(rc.recipe_id) as recipe_count
FROM categories c
LEFT JOIN recipe_categories rc ON c.id = rc.category_id
GROUP BY c.id, c.name, c.create_time
ORDER BY c.name;
```

---

## 收藏模块

### 1. 获取收藏列表

**接口地址**: `GET /favorites`

**功能描述**: 获取当前用户的收藏列表

**请求头**:

```
Authorization: Bearer <token>
```

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页数量 |

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "recipeId": 10,
        "title": "红烧肉",
        "image": "https://example.com/recipe1.jpg",
        "author": "美食达人",
        "createTime": "2026-02-01T10:00:00"
      }
    ],
    "total": 15,
    "page": 1,
    "pageSize": 10
  }
}
```

**数据库表**: `interactions`, `recipes`

**使用的索引**: 
- `idx_interaction_user` (user_id)
- `idx_interaction_recipe` (recipe_id)

**SQL 示例**:

```sql
SELECT i.id, i.recipe_id, r.title, r.image, r.author, i.create_time
FROM interactions i
JOIN recipes r ON i.recipe_id = r.id
WHERE i.user_id = ? AND i.interaction_type = 'favorite'
ORDER BY i.create_time DESC
LIMIT 0, 10;
```

---

### 2. 添加收藏

**接口地址**: `POST /favorites`

**功能描述**: 收藏指定菜谱

**请求头**:

```
Authorization: Bearer <token>
```

**请求参数**:

```json
{
  "recipeId": 10
}
```

**请求参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| recipeId | int | 是 | 菜谱 ID |

**响应示例**:

```json
{
  "code": 200,
  "message": "收藏成功",
  "data": {}
}
```

**数据库表**: `interactions`

**使用的索引**: 
- `idx_interaction_user` (user_id)
- `idx_interaction_recipe` (recipe_id)

**事务说明**: 

1. 检查是否已收藏（避免重复）
2. 插入收藏记录到 `interactions` 表
3. 更新 `recipes.like_count`

---

### 3. 取消收藏

**接口地址**: `DELETE /favorites/{recipeId}`

**功能描述**: 取消收藏指定菜谱

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| recipeId | int | 是 | 菜谱 ID |

**请求头**:

```
Authorization: Bearer <token>
```

**响应示例**:

```json
{
  "code": 200,
  "message": "取消收藏成功",
  "data": {}
}
```

**数据库表**: `interactions`

**使用的索引**: `idx_interaction_user`, `idx_interaction_recipe`

---

### 4. 检查是否收藏

**接口地址**: `GET /favorites/check/{recipeId}`

**功能描述**: 检查当前用户是否收藏了指定菜谱

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| recipeId | int | 是 | 菜谱 ID |

**请求头**:

```
Authorization: Bearer <token>
```

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "isFavorite": true
  }
}
```

**数据库表**: `interactions`

**使用的索引**: `idx_interaction_user`, `idx_interaction_recipe`

---

## 评论模块

### 1. 获取评论列表

**接口地址**: `GET /comments/recipe/{recipeId}`

**功能描述**: 获取指定菜谱的评论列表

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| recipeId | int | 是 | 菜谱 ID |

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页数量 |
| type | string | 否 | all | 评论类型：all-全部/hot-热门 |

**请求头** (可选):

```
Authorization: Bearer <token>
```

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "userId": 100,
        "username": "美食爱好者",
        "avatar": "https://example.com/avatar.jpg",
        "content": "这道菜真的太好吃了！",
        "publishTime": "2026-02-01T10:30:00",
        "likes": 23,
        "isLiked": false,
        "replyCount": 2
      }
    ],
    "total": 89,
    "page": 1,
    "pageSize": 10
  }
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| list | array | 评论列表 |
| list[].id | int | 评论 ID |
| list[].userId | int | 用户 ID |
| list[].username | string | 用户名 |
| list[].avatar | string | 头像 URL |
| list[].content | string | 评论内容 |
| list[].publishTime | datetime | 发布时间 |
| list[].likes | int | 点赞数 |
| list[].isLiked | boolean | 当前用户是否点赞 |
| list[].replyCount | int | 回复数 |
| total | int | 总评论数 |
| page | int | 当前页码 |
| pageSize | int | 每页数量 |

**数据库表**: `comments`, `users`

**使用的索引**: 
- `idx_comment_recipe` (recipe_id)
- `idx_comment_user` (user_id)

**SQL 示例**:

```sql
SELECT c.id, c.user_id, u.username, u.avatar, c.content, 
       c.publish_time, c.likes, COUNT(c2.id) as reply_count
FROM comments c
LEFT JOIN users u ON c.user_id = u.id
LEFT JOIN comments c2 ON c2.reply_to_user = u.username
WHERE c.recipe_id = ? AND c.is_reply = 0
GROUP BY c.id
ORDER BY c.publish_time DESC
LIMIT 0, 10;
```

---

### 2. 发表评论

**接口地址**: `POST /comments`

**功能描述**: 发表评论或回复评论

**请求头**:

```
Authorization: Bearer <token>
```

**请求参数**:

```json
{
  "recipeId": 10,
  "content": "这道菜非常好吃！",
  "replyToUserId": null,
  "replyToUsername": null
}
```

**请求参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| recipeId | int | 是 | 菜谱 ID |
| content | string | 是 | 评论内容 |
| replyToUserId | int | 否 | 回复的用户 ID（回复评论时填写） |
| replyToUsername | string | 否 | 回复的用户名（回复评论时填写） |

**响应示例**:

```json
{
  "code": 200,
  "message": "评论成功",
  "data": {
    "id": 1,
    "recipeId": 10,
    "content": "这道菜非常好吃！",
    "publishTime": "2026-03-01T10:30:00"
  }
}
```

**数据库表**: `comments`

**使用的索引**: `idx_comment_recipe`, `idx_comment_user`

**事务说明**:

1. 插入评论记录到 `comments` 表
2. 更新 `recipes.reply_count`

---

### 3. 点赞评论

**接口地址**: `POST /comments/{commentId}/like`

**功能描述**: 点赞指定评论

**路径参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| commentId | int | 是 | 评论 ID |

**请求头**:

```
Authorization: Bearer <token>
```

**响应示例**:

```json
{
  "code": 200,
  "message": "点赞成功",
  "data": {}
}
```

**数据库表**: `comments`

**使用的索引**: `comments.pk` (主键)

**SQL 示例**:

```sql
UPDATE comments SET likes = likes + 1 WHERE id = ?;
```

---

## 管理员模块

### 1. 管理员登录

**接口地址**: `POST /admin/login`

**功能描述**: 管理员登录

**请求参数**:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "admin_jwt_token",
    "admin": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "role": "super_admin"
    }
  }
}
```

**数据库表**: `admins`

**使用的索引**: `admins.username` (唯一索引)

---

### 2. 获取管理员信息

**接口地址**: `GET /admin/profile`

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
    "lastLoginTime": "2026-03-01T10:00:00"
  }
}
```

**数据库表**: `admins`

---

### 3. 获取用户列表

**接口地址**: `GET /admin/users`

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页数量 |
| keyword | string | 否 | - | 搜索关键词 |
| status | int | 否 | - | 状态：1-正常/0-禁用 |

**响应示例**:

```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "username": "user1",
        "nickname": "美食达人 1",
        "email": "user1@example.com",
        "status": 1,
        "createTime": "2026-01-01T00:00:00"
      }
    ],
    "total": 297479,
    "page": 1,
    "pageSize": 10
  }
}
```

**数据库表**: `users`

**使用的索引**: 
- `users.pk` (主键)
- `users.username` (唯一索引)
- 建议添加：`idx_users_status` (status)

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

### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 1001 | 用户名或密码错误 |
| 1002 | 用户已被禁用 |
| 1003 | 旧密码错误 |
| 1004 | 用户名已存在 |
| 1005 | 邮箱已存在 |
| 2001 | 菜谱不存在 |
| 2002 | 分类不存在 |
| 3001 | 评论不存在 |
| 4001 | 未收藏该菜谱 |

---

## 附录

### A. 数据库表索引汇总

| 表名 | 索引名 | 字段 | 类型 | 说明 |
|------|--------|------|------|------|
| users | uk_old_id | old_id | 唯一索引 | 原始用户 ID 映射 |
| users | username | username | 唯一索引 | 用户名唯一 |
| recipes | uk_old_id | old_id | 唯一索引 | 原始菜谱 ID 映射 |
| recipes | idx_recipe_taste | taste_id | 普通索引 | 口味筛选 |
| recipes | idx_recipe_technique | technique_id | 普通索引 | 工艺筛选 |
| recipes | idx_recipe_difficulty | difficulty_id | 普通索引 | 难度筛选 |
| comments | idx_comment_recipe | recipe_id | 普通索引 | 按菜谱查询评论 |
| comments | idx_comment_user | user_id | 普通索引 | 按用户查询评论 |
| interactions | idx_interaction_user | user_id | 普通索引 | 按用户查询互动 |
| interactions | idx_interaction_recipe | recipe_id | 普通索引 | 按菜谱查询互动 |
| recipe_categories | uk_recipe_category | recipe_id, category_id | 唯一索引 | 防止重复关联 |
| categories | name | name | 唯一索引 | 分类名唯一 |
| tastes | name | name | 唯一索引 | 口味名唯一 |
| techniques | name | name | 唯一索引 | 工艺名唯一 |
| time_costs | name | name | 唯一索引 | 耗时名唯一 |
| difficulties | name | name | 唯一索引 | 难度名唯一 |
| ingredients | name | name | 唯一索引 | 食材名唯一 |
| admins | username | username | 唯一索引 | 管理员名唯一 |

### B. 数据类型说明

| 类型 | 说明 | 示例 |
|------|------|------|
| int | 整数 | 1, 100, 297479 |
| string | 字符串 | "红烧肉", "user1" |
| datetime | 日期时间 | "2026-03-01T10:00:00" |
| enum | 枚举 | "main", "sub", "seasoning" |
| boolean | 布尔值 | true, false |

### C. 枚举值说明

**ingredient_type (食材类型)**:
- `main`: 主料
- `sub`: 辅料
- `seasoning`: 调料

**interaction_type (互动类型)**:
- `like`: 点赞
- `favorite`: 收藏
- `view`: 浏览

**role (管理员角色)**:
- `super_admin`: 超级管理员
- `admin`: 普通管理员

**status (状态)**:
- `1`: 启用/正常
- `0`: 禁用/异常

---

## 增量接口（2026-03-18）

### 用户烹饪会话

1. `POST /users/cooking-sessions/start`
- 说明：开始烹饪会话；若存在未完成会话则返回上次进度（断点恢复）。
- 请求体：`{ "recipeId": 123 }`

2. `PUT /users/cooking-sessions/{sessionId}/progress`
- 说明：保存步骤进度与累计时长。
- 请求体：`{ "currentStep": 3, "durationMs": 125000 }`

3. `POST /users/cooking-sessions/{sessionId}/finish`
- 说明：完成本次烹饪会话。
- 请求体：`{ "durationMs": 260000 }`

### 用户 7 日报告

1. `GET /users/reports/7d`
- 说明：返回用户近 7 日烹饪次数、完成率、场景偏好、口味偏好、活跃时段、总结与建议。

### 新增数据表

- `cooking_sessions`
  - 用途：记录烹饪进度、会话状态、累计时长，支撑断点恢复与报告统计。

---

**文档维护**: 开发团队  
**最后更新**: 2026-03-19 10:35:23  
**版本**: v2.4
