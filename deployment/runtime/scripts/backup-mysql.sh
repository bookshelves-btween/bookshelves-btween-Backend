#!/usr/bin/env bash
set -Eeuo pipefail

RUNTIME_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/opt/bookshelf/backups/mysql}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"

cd "$RUNTIME_DIR"
set -a
source .env
set +a

install -d -m 0700 "$BACKUP_DIR"
timestamp="$(date +%Y%m%d-%H%M%S)"
target="$BACKUP_DIR/${MYSQL_DATABASE}-${timestamp}.sql.gz"

docker compose --env-file .env exec -T mysql \
  mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --routines --triggers "$MYSQL_DATABASE" \
  | gzip > "$target"

find "$BACKUP_DIR" -type f -name '*.sql.gz' -mtime "+$RETENTION_DAYS" -delete
echo "Backup created: $target"
