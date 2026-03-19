# Server Deployment (Ubuntu 24.04 + Docker)

## 1. Requirements

- Ubuntu 24.04 server
- Ports opened in firewall/security group:
  - `3000` (frontend)
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
- `ALIYUN_OSS_ACCESS_KEY_ID`
- `ALIYUN_OSS_ACCESS_KEY_SECRET`
- keep `ALIYUN_OSS_ENABLED=true`
- keep `SEARCH_ENGINE=mysql` for the first deployment
- verify `SPRING_ELASTICSEARCH_URIS=http://elasticsearch:9200`
- keep `SEARCH_ES_INDEX_ALIAS=recipes_search`
- keep `SEARCH_ES_INDEX_NAME=recipes_search_v2`
- note: the compose file builds a custom Elasticsearch image with the official `analysis-smartcn` plugin

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

## 5. Verify

```bash
docker compose --env-file .env.server -f docker-compose.server.yml ps
curl -I http://127.0.0.1:3000
curl -I http://127.0.0.1:8081/api/categories
curl -I http://127.0.0.1:9200
```

Then complete the first Elasticsearch rollout:

1. Log in to the admin dashboard.
2. Open the new `搜索索引` card on Dashboard.
3. Trigger one full rebuild and wait until status finishes.
4. Confirm `failed=0` and there is no `lastError`.
5. Confirm `targetIndex=recipes_search_v2` and `phase=completed`.
6. Change `.env.server` `SEARCH_ENGINE` from `mysql` to `elasticsearch`.
7. Restart backend:

```bash
docker compose --env-file .env.server -f docker-compose.server.yml restart backend
```

This switch strategy keeps MySQL as the primary source of truth while allowing Elasticsearch to take over query serving only after the first full index is ready.

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
