# 美食推荐系统（foodrec）

一个可本地直接跑起来的美食推荐系统，包含：

- 用户端
- 管理后台
- Spring Boot 后端
- MySQL 数据库
- 可选的 Elasticsearch 搜索

## 先看这两个文档

- [快速启动指南.md](./快速启动指南.md)
  适合第一次从 GitHub 拉取项目后，按步骤把本地服务跑起来。
- [agent.md](./agent.md)
  适合接手开发、排障、看系统结构和数据库细节。

## 当前技术栈

- 前端：Vue 3 + Vite 5 + Element Plus + Pinia + Axios + ECharts
- 后端：Spring Boot 3.2.2 + MyBatis + MySQL 8 + Elasticsearch
- Java：21
- Node.js：建议 18+
- 构建工具：Maven 3.9+

## 仓库结构

```text
foodrec/
├── backend/let-me-cook/                 # Spring Boot 后端
├── frontend/                            # 用户端 + 管理后台同一个前端工程
├── docs/                                # 专题文档
├── infra/elasticsearch/                 # ES smartcn 镜像
├── scripts/                             # 辅助脚本
├── agent.md                             # 项目交接总文档
├── 快速启动指南.md                       # 本地启动文档
└── docker-compose.yml                   # ES / 前后端容器编排
```

## 5 分钟本地跑起来

### 1. 准备环境

- MySQL 8
- Java 21
- Maven 3.9+
- Node.js 18+
- Docker Desktop（可选，仅在你想启用 Elasticsearch 时需要）

### 2. 初始化数据库

项目默认连接：

- 数据库：`food_recommend`
- 用户名：`root`
- 密码：`123456`

如果你的本地 MySQL 密码不是 `123456`，启动后端前请自行设置环境变量 `DB_PASSWORD`。

首次启动时，优先使用**完整的 `food_recommend` 数据库备份**。

最推荐：

- 直接导入你们分享的完整数据库导出文件，例如 `food_recommend.sql`

备用方案才是手动执行 `schema.sql + data.sql`：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS food_recommend DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p food_recommend < backend/let-me-cook/src/main/resources/schema.sql
mysql -uroot -p food_recommend < backend/let-me-cook/src/main/resources/data.sql
```

说明：

- 如果已经拿到完整数据库备份，**不要重复导入** `schema.sql` / `data.sql`
- `schema.sql` 负责建表
- `data.sql` 负责写入基础数据和默认账号
- `schema.sql + data.sql` 只作为没有完整数据库备份时的兜底初始化方式

### 3. 启动后端

```bash
cd backend/let-me-cook
mvn spring-boot:run
```

启动后默认监听：

- 后端：`http://127.0.0.1:8081`

可用接口自检：

```bash
curl http://127.0.0.1:8081/api/categories
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

启动后默认监听：

- 前端：`http://127.0.0.1:3000`

常用页面：

- 首页：`http://127.0.0.1:3000`
- 管理员登录：`http://127.0.0.1:3000/admin/login`
- 推荐页：`http://127.0.0.1:3000/recommend`

### 5. 默认账号

如果你已经导入了完整数据库备份，或手动导入了 `data.sql`，可以直接使用：

- 管理员：`admin / 123456`
- 测试用户：`testuser / 123456`

## 当前推荐的本地开发口径

第一次接手仓库，最推荐先按下面这条链路跑：

1. 本地 MySQL
2. 后端本地 `8081`
3. 前端 Vite 本地 `3000`

这是最容易排障、最不容易被 Docker 旧容器干扰的方式。

## 可选：开启 Elasticsearch 完整体验搜索

如果你想体验当前的 Search V2，而不只是最小可运行状态：

### 1. 启动 ES

```bash
docker compose up -d elasticsearch
```

ES 默认端口：

- `http://127.0.0.1:9200`

### 2. 用 ES 模式启动后端

macOS / Linux:

```bash
export SEARCH_ENGINE=elasticsearch
mvn -f backend/let-me-cook/pom.xml spring-boot:run
```

PowerShell:

```powershell
$env:SEARCH_ENGINE='elasticsearch'
cd backend/let-me-cook
mvn spring-boot:run
```

### 3. 首次重建搜索索引

启动后登录管理后台：

- `http://127.0.0.1:3000/admin/login`

然后在数据看板里触发一次“搜索索引重建”。不做这一步，ES 能连上，但搜索索引里还没有数据。

## 当前默认端口一览

- 前端 Vite：`3000`
- 后端：`8081`
- Elasticsearch：`9200`

说明：

- 团队当前也经常使用 Docker 前端 `5173` 作为演示端口
- 但对第一次从 GitHub 拉项目的人来说，**最推荐先用 `3000 + 8081` 跑通**

## 最容易卡住的 4 个点

1. `spring.sql.init.mode=never`
   这意味着后端**不会自动帮你导入数据库结构或种子数据**。

2. 根目录不是独立的管理端项目
   用户端和管理端都在 `frontend/`。

3. 默认搜索引擎仍是 `mysql`
   这能保证你不启动 ES 也能先跑起来。

4. 前端默认端口不是 `5173`
   `npm run dev` 默认是 `3000`；`5173` 是当前团队常用的 Docker 前端演示端口。

## 常用文档

- [快速启动指南.md](./快速启动指南.md)
- [agent.md](./agent.md)
- [docs/README.md](./docs/README.md)
- [项目全景开发手册.md](./docs/02-开发文档/项目全景开发手册.md)
- [API 接口文档.md](./docs/03-API/API%20接口文档.md)
- [数据库设计文档.md](./docs/05-数据库文档/数据库设计文档.md)
