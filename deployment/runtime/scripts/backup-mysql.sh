#!/usr/bin/env bash
set -Eeuo pipefail

RUNTIME_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/opt/bookshelf/backups/mysql}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"

cd "$RUNTIME_DIR"
set -a
source .env
set +a

if [[ ! -f "$RDS_CA_BUNDLE_PATH" ]]; then
  echo "RDS CA bundle not found: $RDS_CA_BUNDLE_PATH"
  echo "Run scripts/prepare-rds-truststore.sh before creating a backup."
  exit 1
fi

install -d -m 0700 "$BACKUP_DIR"
exec 9>"$BACKUP_DIR/.backup.lock"
if ! flock -n 9; then
  echo "Another MySQL backup is already running."
  exit 1
fi

timestamp="$(date +%Y%m%d-%H%M%S-%N)"
target="$BACKUP_DIR/${MYSQL_DATABASE}-${timestamp}.sql.gz"
temporary_target="$(mktemp "$BACKUP_DIR/.${MYSQL_DATABASE}-${timestamp}.XXXXXX.sql.gz")"
credentials_file="$(mktemp "$BACKUP_DIR/.mysql-client.XXXXXX.cnf")"
cutoff_file="$(mktemp "$BACKUP_DIR/.retention-cutoff.XXXXXX")"
cleanup() {
  rm -f -- "$temporary_target" "$credentials_file" "$cutoff_file"
}
trap cleanup EXIT

chmod 0600 "$credentials_file"
escaped_password="${MYSQL_PASSWORD//\\/\\\\}"
escaped_password="${escaped_password//\"/\\\"}"
cat > "$credentials_file" <<EOF
[client]
user=$MYSQL_USER
password="$escaped_password"
host=$MYSQL_HOST
port=${MYSQL_PORT:-3306}
ssl-mode=VERIFY_IDENTITY
ssl-ca=/run/secrets/rds-ca.pem
EOF

docker run --rm --network host \
  -v "$credentials_file:/run/secrets/mysql-client.cnf:ro" \
  -v "$RDS_CA_BUNDLE_PATH:/run/secrets/rds-ca.pem:ro" \
  mysql:8.4@sha256:c592c15aaf4a1961e15d82eb31ea5987dda862d1c4b1e93424438c0e91dc1f8d \
  mysqldump \
  --defaults-extra-file=/run/secrets/mysql-client.cnf \
  --single-transaction --routines --triggers "$MYSQL_DATABASE" \
  | gzip > "$temporary_target"

mv -- "$temporary_target" "$target"

touch --date="$RETENTION_DAYS days ago" "$cutoff_file"
find "$BACKUP_DIR" -type f -name '*.sql.gz' ! -newer "$cutoff_file" -delete
echo "Backup created: $target"
