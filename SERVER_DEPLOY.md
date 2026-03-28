# Server Deployment (Ubuntu 24.04 + Docker)

## 1. Requirements

- Ubuntu 24.04 server
- Ports opened in firewall/security group:
  - `1234` (frontend)
  - `8081` (backend API)
  - `9200` (optional, only if you want to debug Elasticsearch from outside the server)
- MySQL (`3306`) is bound to `127.0.0.1` by default for security
  - For external DB access, edit `docker-compose.server.yml` and change
    `127.0.0.1:${MYSQL_PORT:-3306}:3306` to `${MYSQL_PORT:-3306}:3306`
- Elasticsearch (`9200`) is also bound to `127.0.0.1` by default in server compose for security

## 2. Install Docker (Ubuntu 24.04)

Run on server:

```bash
cd /path/to/food-recommendation-system
sudo bash install-docker-ubuntu.sh
newgrp docker
```

## 3. Prepare Environment

```bash
cd /path/to/food-recommendation-system
cp .env.server.example .env.server
```

Edit `.env.server` and set real values:

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_PASSWORD`
- `JWT_SECRET`
- if you need upload on the server:
  - set `ALIYUN_OSS_ENABLED=true`
  - set `ALIYUN_OSS_ACCESS_KEY_ID`
  - set `ALIYUN_OSS_ACCESS_KEY_SECRET`
- if upload is not needed yet:
  - keep `ALIYUN_OSS_ENABLED=false`
- keep `SEARCH_ENGINE=auto`
- verify `SPRING_ELASTICSEARCH_URIS=http://elasticsearch:9200`
- keep `SEARCH_ES_INDEX_ALIAS=recipes_search`
- keep `SEARCH_ES_INDEX_NAME=recipes_search_v2`
- keep `NGINX_API_UPSTREAM=http://backend:8081`
- keep `FRONTEND_PORT=1234`
- keep `SERVER_DATA_ROOT=/opt/food-data`
- note: the compose file builds a custom Elasticsearch image with the official `analysis-smartcn` plugin
  - for weak-network servers, prefer the `prebuilt` deployment path and preload `food-elasticsearch:smartcn`

## 4. Deploy

```bash
cd /path/to/food-recommendation-system
bash deploy-server.sh
```

`deploy-server.sh` will:

- check Docker / Compose availability
- validate `.env.server` required values
- build and start all containers
- run HTTP health checks for frontend and backend

The compose stack now includes:

- `frontend`
- `backend`
- `mysql`
- `elasticsearch`

Elasticsearch is built from the repo Dockerfile and includes:

- official `analysis-smartcn` plugin
- persistent data volume `elasticsearch_data`
- host-mounted persistence root `${SERVER_DATA_ROOT}` for MySQL and Elasticsearch in prebuilt mode

The current startup files also include:

- frontend container now proxies `/api` to `backend:8081` inside the compose network
- datasource URLs now include prepared-statement cache and batch-write optimizations
- OSS credentials are only mandatory when `ALIYUN_OSS_ENABLED=true`

## 5. Verify

```bash
docker compose --env-file .env.server -f docker-compose.server.yml ps
curl -I http://127.0.0.1:1234
curl -I http://127.0.0.1:8081/api/categories
curl -I http://127.0.0.1:9200
```

Then complete the first Elasticsearch rollout:

1. Log in to the admin dashboard.
2. Open the new `搜索索引` card on Dashboard.
3. Trigger one full rebuild and wait until status finishes.
4. Confirm `failed=0` and there is no `lastError`.
5. Confirm `targetIndex=recipes_search_v2` and `phase=completed`.
6. Keep `.env.server` `SEARCH_ENGINE=auto`.
7. Restart backend:

```bash
docker compose --env-file .env.server -f docker-compose.server.yml restart backend
```

This strategy keeps MySQL as the primary source of truth while allowing the backend to auto-select Elasticsearch whenever the alias/index is ready.

## 6. First-Start DB Init and Persistence

- On first startup only, MySQL initializes from:
  - `backend/let-me-cook/src/main/resources/schema.sql`
  - `backend/let-me-cook/src/main/resources/data.sql`
- Data is persisted in Docker volume `mysql_data`.
- Recreate containers without data loss:

```bash
docker compose --env-file .env.server -f docker-compose.server.yml down
docker compose --env-file .env.server -f docker-compose.server.yml up -d
```

- Full DB re-init (this deletes all DB data):

```bash
docker compose --env-file .env.server -f docker-compose.server.yml down -v
docker compose --env-file .env.server -f docker-compose.server.yml up -d --build
```

## 7. Common Operations

```bash
# restart
docker compose --env-file .env.server -f docker-compose.server.yml restart

# logs
docker compose --env-file .env.server -f docker-compose.server.yml logs -f backend
docker compose --env-file .env.server -f docker-compose.server.yml logs -f frontend
docker compose --env-file .env.server -f docker-compose.server.yml logs -f mysql
docker compose --env-file .env.server -f docker-compose.server.yml logs -f elasticsearch

# check smartcn plugin
curl http://127.0.0.1:9200/_cat/plugins?v

# stop
docker compose --env-file .env.server -f docker-compose.server.yml down
```

## 8. Copy Project to Server (example)

From local machine:

```bash
scp -r ./food-recommendation-system user@your_server_ip:/opt/
ssh user@your_server_ip
cd /opt/food-recommendation-system
```

## 9. Sync Existing Database Data (optional)

By default, deployment does not auto-sync your local database to server.
It only initializes DB on first start using `schema.sql` and `data.sql`.

If you want to migrate existing data, use dump + import:

On source MySQL:

```bash
mysqldump -h <source_host> -P <source_port> -u <source_user> -p \
  --databases food_recommend > food_recommend.sql
```

Copy to server and import into container:

```bash
scp food_recommend.sql user@your_server_ip:/opt/food-recommendation-system/
ssh user@your_server_ip
cd /opt/food-recommendation-system
set -a
source .env.server
set +a
docker exec -i food-mysql mysql -u root -p"${MYSQL_ROOT_PASSWORD}" food_recommend < food_recommend.sql
```

If the imported dump is older than the current code line, apply the incremental patch immediately after import:

```bash
docker exec -i food-mysql mysql -u root -p"${MYSQL_ROOT_PASSWORD}" food_recommend < backend/let-me-cook/src/main/resources/db/migration/V6__daily_recommendation_comment_patch.sql
```

This patch adds:

- `daily_recipe_recommendations`
- `daily_recommend_job_runs`
- `recipe_count` columns and indexes
- comment performance indexes
- incremental `trg_comment_insert`

## 10. Daily Recommendation Delivery

`type=daily` 和命中离线推荐的 `type=personal` 都不在 Spring Boot 容器里训练。业务栈只读取：

- `daily_recipe_recommendations`
- `daily_recommend_job_runs`

当前日推口径已经固定为：

- **本地离线机器**完成：
  - 筛选用户和食谱
  - 使用仓库内 `foodrec/local_top100_cke_full/knowledge_graph_source/2-13-知识图谱构建.py` 生成筛选后的知识图谱
  - 使用仓库内 `foodrec/local_top100_cke_full/` 提供的 `CKEFull` 源码、数据集映射与模型清单，在本地离线机器上加载历史最优模型
  - 为每个目标用户生成 `Top100`
  - 直接把 `Top100` 写入目标库
- **业务服务器**只负责读取结果，不参与训练

### 10.1 In-repo Top100 bundle

Git 仓库内保留：

- `local_top100_cke_full/README.md`
- `local_top100_cke_full/selected_model_manifest.json`
- `scripts/recommendation_daily/`

大型模型权重、缓存数据和运行工件不随普通 Git 推送，需要额外同步到本地离线机器。

当前默认使用的历史最优模型是本地离线 bundle 中的 `CKEFull model_epoch0030.pt`。

### 10.2 Local baseline and daily generation

在本地离线机器上运行：

```bash
python -m scripts.recommendation_daily.main bootstrap-baseline
python -m scripts.recommendation_daily.main run-daily
```

如果只是使用仓库内已经训练好的历史最优 `CKEFull`，直接执行：

```bash
python -m scripts.recommendation_daily.main use-existing-model
```

如果本地离线机器可以直接访问目标 MySQL，则会直接写入服务器数据库。

如果不能直连目标 MySQL，则导出 `daily_recipe_recommendations` 和 `daily_recommend_job_runs` 的当日增量结果，再在服务器导入。

### 10.3 Business server responsibilities

业务服务器不需要：

- GPU
- Python 训练环境
- 知识图谱构建环境
- `CKEFull` 训练脚本

业务服务器只需要：

- `frontend`
- `backend`
- `mysql`
- `elasticsearch`

以及已经写入库中的离线推荐结果。

### 10.4 Validation

确认结果已进入服务器数据库后，验证：

```bash
docker exec -it food-mysql mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -D food_recommend -e "SELECT job_date, phase, status, affected_users, affected_recipes FROM daily_recommend_job_runs ORDER BY id DESC LIMIT 5;"
docker exec -it food-mysql mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -D food_recommend -e "SELECT user_id, COUNT(*) AS total_count, SUM(selected_for_delivery) AS selected_count FROM daily_recipe_recommendations WHERE biz_date = CURDATE() GROUP BY user_id ORDER BY total_count DESC LIMIT 10;"
curl "http://127.0.0.1:8081/api/recipes/recommend?type=daily&limit=8"
```

当前接口口径：

- 每用户每天保存 `Top100`
- 其中前 `16` 条是主推池，`selected_for_delivery=1`
- 若用户当天已有离线 `Top100`
  - `type=daily` 优先读取这批结果
  - `type=personal` 也优先读取这批结果
- 若用户当天没有离线结果，后端回退实时推荐
