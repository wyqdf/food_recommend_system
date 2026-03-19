# 美食推荐系统 - 文档中心

> **版本**: v3.5
> **最后更新**: 2026-03-19 18:05:00
> **维护原则**: 以仓库代码、`schema.sql`、运行日志与当前 API 返回为准

## 项目概况

- 后端：Spring Boot 3.2.2 + MyBatis + MySQL 8.0
- 前端：Vue 3 + Vite 5 + Element Plus + Pinia + Axios
- 认证：JWT
- 当前已落地能力：
  - 菜谱浏览、搜索、推荐、收藏、评论、投稿
  - 多字段搜索、搜索建议词、最近搜索、搜索排序
  - Elasticsearch Search V2：`smartcn` 中文分词、`combined_fields` 召回、独立 completion 建议词、应用双写、索引重建管理
  - 冷启动问卷、场景标签、可解释推荐
  - 烹饪模式、断点续做、7 日用户报告
  - 管理端用户/菜谱/分类/属性/日志/统计看板

## 文档目录

```text
docs/
├── README.md                      # 文档中心（本文档）
├── 02-开发文档/
│   ├── 项目全景开发手册.md        # 项目全景、启动链路、模块职责
│   └── 升级迭代记录.md            # 功能迭代、修复与性能优化时间线
├── 03-API/
│   ├── API 接口文档.md            # 用户端 API
│   └── 管理员 API 接口文档.md     # 管理端 API
└── 05-数据库文档/
    └── 数据库设计文档.md          # 数据库结构、索引与增量说明
```

## 快速导航

| 场景 | 文档 |
|------|------|
| 第一次从 GitHub 拉项目，先跑起来 | [../readme.md](../readme.md) |
| 想按步骤把本地服务启动到默认端口 | [../快速启动指南.md](../快速启动指南.md) |
| 接手项目 / 喂给大模型建立上下文 | [../agent.md](../agent.md) |
| 了解项目全景 | [02-开发文档/项目全景开发手册.md](./02-开发文档/项目全景开发手册.md) |
| 查看最近开发进度 | [02-开发文档/升级迭代记录.md](./02-开发文档/升级迭代记录.md) |
| 查用户端接口 | [03-API/API 接口文档.md](./03-API/API%20接口文档.md) |
| 查管理端接口 | [03-API/管理员 API 接口文档.md](./03-API/管理员%20API%20接口文档.md) |
| 查数据库与表结构 | [05-数据库文档/数据库设计文档.md](./05-数据库文档/数据库设计文档.md) |

## 当前数据规模

> 统计时间：2026-03-18

| 表 | 数量 |
|----|------|
| `recipes` | 309,461 |
| `comments` | 2,878,150 |
| `users` | 297,490 |
| `cooking_steps` | 2,903,596 |
| `recipe_ingredients` | 4,408,256 |
| `recipe_categories` | 1,438,126 |
| `categories` | 344 |
| `ingredients` | 26,547 |
| `behavior_events` | 136 |
| `user_preference_profiles` | 1 |
| `cooking_sessions` | 2 |

## 当前注意事项

- 前端默认开发端口来自 [vite.config.js](../frontend/vite.config.js) 的 `3000`；若本地用命令行显式指定 `5173`，以实际启动端口为准。
- 后端默认端口为 `8081`，见 [application.properties](../backend/let-me-cook/src/main/resources/application.properties)。
- 本地默认关闭 OSS：`aliyun.oss.enabled=false`。
- 搜索引擎当前仓库默认值仍为 `search.engine=mysql`，用于保证首次部署安全；完成首轮 ES 建索引与重建后再切到 `elasticsearch`。
- 本地已于 `2026-03-19 10:30` 完成 Search V2 验收：
  - `analysis-smartcn 8.12.2` 插件可用
  - 别名 `recipes_search` 当前指向 `recipes_search_v2`
  - `recipes_search_v2` 当前精确文档数为 `309462`
  - 宿主机后端运行中已切到 `SEARCH_ENGINE=elasticsearch`
- 推荐页已于 `2026-03-19 13:07` 从“硬编码场景标签”切换为“现有分类筛选”：
  - 当前展示 `家常菜 / 快手菜 / 减肥瘦身 / 宴客菜 / 夜宵 / 下饭菜 / 儿童 / 早餐`
  - 对外接口为 `GET /api/recipes/recommend?type=personal&categoryId=<分类ID>`
- `recipe_ingredients` 存在历史导入重复数据，统计层已做规避，但数据层清洗仍是后续重点任务。

## 文档同步约定

每次代码完成后，至少检查以下同步项：

1. 功能/流程变化：
   更新 [升级迭代记录.md](./02-开发文档/升级迭代记录.md)
2. 对外接口变化：
   更新 [API 接口文档.md](./03-API/API%20接口文档.md) 或 [管理员 API 接口文档.md](./03-API/管理员%20API%20接口文档.md)
3. 表结构/索引/迁移变化：
   更新 [数据库设计文档.md](./05-数据库文档/数据库设计文档.md)
4. 启动方式、模块职责、端口或架构变化：
   更新 [项目全景开发手册.md](./02-开发文档/项目全景开发手册.md)

## 推荐维护顺序

1. 先改代码并完成验证。
2. 记录迭代时间线与验证结果。
3. 再补 API / 数据库 / 全景手册中的静态说明。
4. 最后回到本文档更新“最后更新”时间与新增索引说明。
