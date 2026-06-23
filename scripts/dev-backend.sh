#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
else
  echo "[dev-backend] WARN: root .env not found. Falling back to local defaults."
fi

BACKEND_PORT="${BACKEND_PORT:-8080}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"

echo "[dev-backend] Starting Spring Boot API. Secret values are not printed."
echo "[dev-backend] BACKEND_PORT is configured."

check_docker_mysql_port() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "[dev-backend] WARN: docker command not found; skipping Docker published port check."
    return 0
  fi

  local ports
  ports="$(docker ps --filter name=jiber-mysql --format '{{.Ports}}' 2>/dev/null || true)"
  if [[ -z "${ports}" ]]; then
    echo "[dev-backend] WARN: jiber-mysql container is not running; DB preflight may fail."
    return 0
  fi

  local published_port=""
  if [[ "${ports}" =~ 0\.0\.0\.0:([0-9]+)-\>3306 ]]; then
    published_port="${BASH_REMATCH[1]}"
  elif [[ "${ports}" =~ 127\.0\.0\.1:([0-9]+)-\>3306 ]]; then
    published_port="${BASH_REMATCH[1]}"
  elif [[ "${ports}" =~ \[::\]:([0-9]+)-\>3306 ]]; then
    published_port="${BASH_REMATCH[1]}"
  fi

  if [[ -z "${published_port}" ]]; then
    echo "[dev-backend] WARN: could not detect jiber-mysql published port; check docker ps if DB connection fails."
    return 0
  fi

  if [[ "${DB_HOST}" == "localhost" || "${DB_HOST}" == "127.0.0.1" ]]; then
    if [[ "${published_port}" != "${DB_PORT}" ]]; then
      if [[ "${ALLOW_DB_PORT_MISMATCH:-false}" == "true" ]]; then
        echo "[dev-backend] WARN: Docker MySQL published port and DB_PORT differ; continuing because ALLOW_DB_PORT_MISMATCH=true."
        return 0
      fi
      echo "[dev-backend] ERROR: Docker MySQL published port and DB_PORT differ."
      echo "[dev-backend]        Set DB_PORT to the Docker published port, or set ALLOW_DB_PORT_MISMATCH=true to continue."
      exit 2
    fi
    echo "[dev-backend] Docker MySQL published port matches DB_PORT."
  fi
}

check_docker_mysql_port

if [[ "${SKIP_DB_PREFLIGHT:-false}" == "true" ]]; then
  echo "[dev-backend] SKIP_DB_PREFLIGHT=true; skipping auth schema preflight."
else
  echo "[dev-backend] Running auth schema preflight."
  "${ROOT_DIR}/scripts/check-auth-schema.sh"
fi

cd "${ROOT_DIR}/backend"
exec mvn spring-boot:run "$@"
