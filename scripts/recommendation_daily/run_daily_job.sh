#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
VENV_PYTHON="${REPO_ROOT}/scripts/recommendation_daily/.venv/bin/python"

if [[ ! -x "${VENV_PYTHON}" ]]; then
  echo "missing python runtime: ${VENV_PYTHON}" >&2
  exit 1
fi

cd "${REPO_ROOT}"
"${VENV_PYTHON}" -m scripts.recommendation_daily.main run-daily
