#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
else
  echo "[dev-frontend] WARN: root .env not found. Falling back to local defaults."
fi

BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"
VITE_API_BASE_URL="${VITE_API_BASE_URL:-http://localhost:${BACKEND_PORT}/api/v1}"
export VITE_API_BASE_URL

echo "[dev-frontend] Starting Vite dev server. Secret values are not printed."
echo "[dev-frontend] VITE_API_BASE_URL is configured for the backend dev port."

if [[ -n "${VITE_KAKAO_MAP_APP_KEY:-}" ]]; then
  echo "[dev-frontend] VITE_KAKAO_MAP_APP_KEY: present."
else
  echo "[dev-frontend] WARN: VITE_KAKAO_MAP_APP_KEY is missing; Kakao map fallback will be shown."
fi

cd "${ROOT_DIR}/frontend"
exec npm run dev -- --port "${FRONTEND_PORT}" "$@"
