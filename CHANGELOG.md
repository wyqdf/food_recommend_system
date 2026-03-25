# 开发日志 (CHANGELOG)

## [2026-03-01] API 文档同步更新

### 新增
- API 文档添加数据结构定义章节
- 添加 Recipe、User、Comment 对象字段说明
- 添加菜谱详情接口字段说明表

### 修改
- 菜谱详情接口响应结构更新为嵌套格式
  - `recipe` 对象包含基本信息
  - `steps` 数组包含烹饪步骤
  - `ingredients` 数组包含食材信息
  - `nutrition` 对象包含营养信息
- 评论列表接口响应结构更新
  - `user.nickname` 替代 `username`
  - `publishTime` 替代 `time`
- 推荐菜谱接口响应结构更新
  - 返回 `title` 字段替代 `name`

### 修复
- 前端 API 数据结构与组件期望字段对齐
- 详情页步骤字段从 `content` 改为 `description`
- 详情页食材添加 `type` 字段区分主料/辅料/调料

---

## [2026-03-01] 前端 API Mock 数据修复

### 修复
- Home.vue 推荐列表数据取值错误（`recRes.data` → `recRes.data.list`）
- 图片占位服务从 `via.placeholder.com` 改为 `dummyimage.com`
- 修复 JS 语法错误（多余的 `};`）

---

## [2026-03-01] 前端项目初始化

### 新增
- Vue 3 + Element Plus 项目结构
- 路由配置（首页、菜谱详情、搜索、登录注册、个人中心）
- API 接口封装（recipeApi、userApi、favoriteApi、commentApi）
- Pinia 状态管理（userStore）
- 公共组件（RecipeCard、RecipeGrid）

---

## [2026-03-01] 后端 API 开发

### 新增
- 用户模块接口（登录、注册、获取信息、更新信息）
- 菜谱模块接口（列表、详情、搜索、分类、推荐）
- 收藏模块接口（列表、添加、删除、检查）
- 评论模块接口（列表、发表、点赞）
- JWT 认证机制
- 单元测试

---

## [2026-03-01] 数据库设计与导入

### 新增
- 数据库表结构设计（13 张表）
- 数据导入脚本（Python + pymysql）
- 批量插入优化
- 进度条显示

### 数据统计
- 菜谱数据：309,863 条
- 评论数据：2,878,150 条
- 互动数据：2,228,594 条
- 用户数据：297,479 条
