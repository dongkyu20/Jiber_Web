#!/bin/sh
set -eu

MYSQL_HOST="${DB_HOST:-mysql}"
MYSQL_PORT="${DB_PORT:-3306}"
MYSQL_DATABASE="${DB_NAME:-jiber}"
MYSQL_ROOT_PASSWORD="${DB_ROOT_PASSWORD:?Set DB_ROOT_PASSWORD for database dump import}"
SOURCE_DATABASE="${DB_DUMP_SOURCE_DATABASE:-Jiber}"
DUMP_DIR="${DB_DUMP_EXTRACT_DIR:-/dumps/extracted}"
FORCE_IMPORT="${DB_DUMP_FORCE_IMPORT:-false}"
DB_SCRIPT_DIR="${DB_SCRIPT_DIR:-/db-scripts}"

mysql_cmd="mysql --default-character-set=utf8mb4 -h${MYSQL_HOST} -P${MYSQL_PORT} -uroot -p${MYSQL_ROOT_PASSWORD}"

apply_project_migrations() {
  if [ ! -d "$DB_SCRIPT_DIR" ]; then
    echo "DB script directory not found at ${DB_SCRIPT_DIR}; skipping project migrations."
    return
  fi

  dependency_count="$($mysql_cmd -N -B "$MYSQL_DATABASE" -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('users', 'properties');")"
  if [ "$dependency_count" != "2" ]; then
    echo "Project migration dependencies are not ready; skipping."
    return
  fi

  for script in "$DB_SCRIPT_DIR"/007_community.sql "$DB_SCRIPT_DIR"/008_community_notice_category.sql; do
    if [ -f "$script" ]; then
      echo "Applying project migration ${script}."
      $mysql_cmd "$MYSQL_DATABASE" < "$script"
    fi
  done
}

echo "Preparing database ${MYSQL_DATABASE} for dump import."
$mysql_cmd -e "CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
$mysql_cmd "$MYSQL_DATABASE" -e "CREATE TABLE IF NOT EXISTS jiber_import_history (import_name VARCHAR(255) PRIMARY KEY, imported_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;"

if [ "${DB_DUMP_SKIP_IMPORT:-false}" = "true" ]; then
  echo "DB_DUMP_SKIP_IMPORT=true; skipping database dump import."
  apply_project_migrations
  exit 0
fi

sql_file="$(find "$DUMP_DIR" -type f -name '*.sql' | sort | tail -n 1)"
if [ -z "$sql_file" ]; then
  echo "No .sql dump file found under $DUMP_DIR" >&2
  exit 1
fi

dump_name="$(basename "$sql_file")"
already_imported="$($mysql_cmd -N -B "$MYSQL_DATABASE" -e "SELECT COUNT(*) FROM jiber_import_history WHERE import_name = '${dump_name}';")"
if [ "$already_imported" != "0" ] && [ "$FORCE_IMPORT" != "true" ]; then
  echo "Database dump ${dump_name} was already imported; skipping."
  apply_project_migrations
  exit 0
fi

first_bytes="$(head -c 2 "$sql_file" | od -An -tx1 | tr -d ' \n')"
echo "Importing database dump ${dump_name} into ${MYSQL_DATABASE}."

if [ "$first_bytes" = "fffe" ] || [ "$first_bytes" = "feff" ]; then
  iconv -f UTF-16 -t UTF-8 "$sql_file" \
    | sed "s/\`${SOURCE_DATABASE}\`/\`${MYSQL_DATABASE}\`/g" \
    | $mysql_cmd "$MYSQL_DATABASE"
else
  sed "s/\`${SOURCE_DATABASE}\`/\`${MYSQL_DATABASE}\`/g" "$sql_file" \
    | $mysql_cmd "$MYSQL_DATABASE"
fi

apply_project_migrations
$mysql_cmd "$MYSQL_DATABASE" -e "CREATE TABLE IF NOT EXISTS jiber_import_history (import_name VARCHAR(255) PRIMARY KEY, imported_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_0900_ai_ci; REPLACE INTO jiber_import_history (import_name) VALUES ('${dump_name}');"
echo "Database dump import completed: ${dump_name}"
