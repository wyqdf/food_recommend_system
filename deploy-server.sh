#!/usr/bin/env bash
set -euo pipefail

COMPOSE_ARGS=(--env-file .env.server -f docker-compose.server.yml)

if ! command -v docker >/dev/null 2>&1; then
  echo "docker command not found. Install Docker first."
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "docker compose plugin not found. Install docker-compose-plugin first."
  exit 1
fi

if [[ ! -f ".env.server" ]]; then
  echo ".env.server not found. Copy .env.server.example to .env.server first."
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env.server
set +a

required_vars=(
  MYSQL_ROOT_PASSWORD
  MYSQL_PASSWORD
  JWT_SECRET
  ALIYUN_OSS_ACCESS_KEY_ID
  ALIYUN_OSS_ACCESS_KEY_SECRET
)

for var in "${required_vars[@]}"; do
  value="${!var:-}"
  if [[ -z "${value}" ]]; then
    echo "Missing required value: ${var}"
    exit 1
  fi
  if [[ "${value}" == CHANGE_ME_* || "${value}" == REPLACE_WITH_* ]]; then
    echo "Placeholder detected for ${var}. Please set a real value in .env.server."
    exit 1
  fi
done

backend_port="${BACKEND_PORT:-8081}"
frontend_port="${FRONTEND_PORT:-3000}"

echo "Starting containers..."
docker compose "${COMPOSE_ARGS[@]}" up -d --build
docker compose "${COMPOSE_ARGS[@]}" ps

if ! command -v curl >/dev/null 2>&1; then
  echo "curl not found. Skipping HTTP health checks."
  exit 0
fi

wait_http_ok() {
  local url="$1"
  local name="$2"
  local retries=30
  local i=1
  while (( i <= retries )); do
    if curl -fsS -m 5 "${url}" >/dev/null; then
      echo "${name} is ready: ${url}"
      return 0
    fi
    sleep 2
    ((i++))
  done
  return 1
}

if ! wait_http_ok "http://127.0.0.1:${backend_port}/api/categories" "backend"; then
  echo "Backend health check failed. Recent backend logs:"
  docker compose "${COMPOSE_ARGS[@]}" logs --tail=120 backend
  exit 1
fi

if ! wait_http_ok "http://127.0.0.1:${frontend_port}" "frontend"; then
  echo "Frontend health check failed. Recent frontend logs:"
  docker compose "${COMPOSE_ARGS[@]}" logs --tail=120 frontend
  exit 1
fi

echo "Deployment completed."
