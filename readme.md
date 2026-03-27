# 美食推荐系统（foodrec）

一个面向本地开发与课程/项目演示的美食推荐系统，包含：

- Vue 3 用户端与管理后台
- Spring Boot 后端
- MySQL 业务库
- Elasticsearch Search V2
- 仓库内离线 `Top100` 推荐源码入口与模型清单

## 当前快照

> 最后整理：`2026-03-28 03:41:15`

- 推荐开发端口：前端 `3000`，后端 `8081`
- Windows 已提供源码直启脚本：
  - `run-latest-dev.ps1`
  - `stop-latest-dev.ps1`
- 推荐接口当前真实口径：
  - 登录用户命中场景且存在离线 `Top100` 时，`type=personal` 改为“离线与实时分开筛选，再交错返回”
  - 目标配额按 `ceil(limit * 0.7)` 保证至少 `70%` 优先来自命中当前场景的离线 `Top100`
  - 若命中场景的 `Top100` 不足，不会补入非场景 `Top100`，而是用命中当前场景的实时结果补齐
  - 实时补位来自更大的热门源与非热门源候选池，带随机性，避免剩余 `30%` 全被大热门占满
  - `Top100` 卡片优先保留离线模型理由，仅在理由缺失时补场景文案
- 评论与登录态当前口径：
  - `GET /api/comments/recipe/**` 支持匿名读取
  - 若请求携带有效用户 token，后端会识别登录态并正确返回 `isLiked`
- 首页当前行为：
  - `为你推荐` 与推荐页 `type=personal` 使用同一条推荐链路
  - `热门菜谱` 改成从更大的热门候选池中随机抽样展示
  - `热门分类` 来自 `GET /api/categories/recommend`
- 最近一轮接口性能优化：
  - `recipes` 随机候选查询已改成基于 `status + id` 索引的随机种子范围扫描，避免在 30 万菜谱表上全量扫 `status=1`
  - 登录态 `type=personal` 合并了重复的偏好/历史种子查询与候选装配
  - 个性化推荐收藏种子改为数据库直接取最近公开收藏 `LIMIT 20`，避免全量装载收藏记录
  - 收藏列表分页改为数据库分页；收藏统计统一回到 `recipes.favorite_count`
  - 管理端与统计页的浏览量统一基于 `behavior_events` 中的 `recipe_view` 事件
  - 列表页命中 `mode` 时不再默认粗拉 `5000` 条候选做场景重排，冷请求耗时明显下降
- 最近一轮验证状态：
  - 已验证 `run-latest-dev.ps1` / `stop-latest-dev.ps1` 可拉起并停止最新源码前后端
  - 后端 `./mvnw.cmd test` 已通过，`50` 项测试绿色通过
  - 前端 `npm run build` 已通过
  - 已完成登录、首页、搜索、详情、收藏、收藏页、烹饪模式、投稿页、7 日报告、管理端首页与菜谱管理页的浏览器流程回归
- 最新本地测速样例：
  - `GET /api/recipes/recommend?type=personal&limit=12&mode=family`：约 `211.26ms`
  - `GET /api/favorites?page=1&pageSize=12`：约 `8.18ms`
  - `GET /api/users/profile`：约 `7.52ms`
  - `GET /api/recipes/search?keyword=鸡&page=1&pageSize=12`：约 `63.17ms`
  - `GET /api/admin/recipes?page=1&pageSize=5`：约 `66.82ms`
- 前端 `Home.vue`、`Recommend.vue`、`Recipes.vue` 已接入 `latestRequest` 防护，避免切模式/切筛选时旧请求覆盖新结果
- 登录页只展示 5 个测试账号名称用于演示，不再暗示自动登录或展示用户偏好

## 先看哪份文档

- [快速启动指南.md](./快速启动指南.md)
  - 第一次把项目跑起来、导库、自检、排障时看这里
- [docs/README.md](./docs/README.md)
  - 想快速找到“该看哪一份专题文档”时看这里
- [agent.md](./agent.md)
  - 接手开发、排查架构与数据库细节时看这里

## 最短启动路径

### 1. 准备环境

- Java 21
- Maven 3.9+
- Node.js 18+
- MySQL 8
- Docker Desktop
  - 可选，但 `run-latest-dev.ps1` 会检查 Docker 中是否有旧容器；如果本机没有 `docker` 命令，建议改用手动启动

### 2. 初始化数据库

优先导入完整 `food_recommend` 数据库备份；如果备份偏旧，再补执行：

```bash
mysql -uroot -p food_recommend < backend/let-me-cook/src/main/resources/db/migration/V6__daily_recommendation_comment_patch.sql
```

### 3. Windows 一键拉起最新前后端

首次启动前，请先在 [快速启动指南.md](./快速启动指南.md) 中完成数据库导入，并至少执行一次前端依赖安装：

```bash
cd frontend
npm install
```

随后可在仓库根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\run-latest-dev.ps1
```

脚本会：

- 停掉本仓库占用 `3000/8081` 的旧前后端进程
- 停掉名为 `food-frontend`、`food-backend` 的旧容器
- 若前端依赖缺失，会先自动执行一次 `npm install`
- 以后端源码方式启动最新代码，并显式使用：
  - `ALIYUN_OSS_ENABLED=false`
  - `SEARCH_ENGINE=auto`
- 以前端 Vite 开发服务方式启动最新代码
- 把日志写到 `temp/` 目录

停止脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-latest-dev.ps1
```

### 4. 手动启动

手动启动步骤、数据库导入细节、ES 启用方式、常见报错与自检项统一放在 [快速启动指南.md](./快速启动指南.md)。

### 5. 本地 Docker 启动

如果你想直接验证当前 Docker 启动链路，可以在仓库根目录执行：

```powershell
Copy-Item .env.docker.example .env.docker
docker compose --env-file .env.docker -f .\docker-compose.yml up -d --build
```

当前本地 Docker 口径：

- 前端默认对外：`3000`
- 后端默认对外：`8081`
- Elasticsearch 默认对外：`9200`
- 前端容器内部直接反向代理到 `backend:8081`
- 本地默认 `SEARCH_ENGINE=auto`
- 本地默认 `ALIYUN_OSS_ENABLED=false`

## 当前技术栈

- 前端：Vue 3 + Vite 5 + Element Plus + Pinia + Axios + ECharts
- 后端：Spring Boot 3.2.2 + MyBatis + MySQL 8
- 搜索：Elasticsearch Search V2
- 推荐：离线 `Top100` + 实时个性化推荐混合链路
- Java：21
- Node.js：18+

## 仓库结构

```text
foodrec-merged-run/
├── backend/let-me-cook/                 # Spring Boot 后端
├── frontend/                            # 用户端 + 管理后台
├── docs/                                # 专题文档
├── infra/elasticsearch/                 # ES smartcn 镜像
├── scripts/                             # 辅助脚本
├── local_top100_cke_full/               # 离线 Top100 说明、源码入口、模型清单
├── run-latest-dev.ps1                   # Windows 源码直启脚本
├── stop-latest-dev.ps1                  # Windows 停止脚本
├── temp/                                # 本地运行日志与 PID
├── agent.md                             # 项目交接总文档
├── 快速启动指南.md                       # 启动、自检、常见问题
└── docker-compose.yml                   # 开发期容器编排
```

## 离线推荐资产

仓库内当前保留：

- `local_top100_cke_full/README.md`
- `local_top100_cke_full/cke_full_source/`
- `local_top100_cke_full/knowledge_graph_source/`
- `local_top100_cke_full/selected_model_manifest.json`

普通 Git 不随仓库同步的大文件仍包括：

- 模型权重
- 大型缓存
- 运行工件
- 大体量离线结果文件

## 文档维护约定

- 启动步骤只保留在 [快速启动指南.md](./快速启动指南.md)
- 架构与运行链路只保留在 [项目全景开发手册.md](./docs/02-开发文档/项目全景开发手册.md)
- 对外接口语义只保留在 [API 接口文档.md](./docs/03-API/API%20接口文档.md)
- 迭代时间线只保留在 [升级迭代记录.md](./docs/02-开发文档/升级迭代记录.md)

如果发现同一说明在多个文档重复出现，优先删掉副本，保留一份主文档。
