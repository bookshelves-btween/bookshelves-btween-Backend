#!/usr/bin/env bash
set -Eeuo pipefail

RUNTIME_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_ROOT="${PROJECT_ROOT:-/opt/bookshelf}"
cd "$RUNTIME_DIR"

if [[ ! -f .env ]]; then
  echo "Missing $RUNTIME_DIR/.env"
  echo "Copy .env.example to .env, replace every CHANGE_* value, and try again."
  exit 1
fi

if grep -Eq 'CHANGE_TO_|example\.com' .env; then
  echo ".env still contains example values. Deployment stopped."
  exit 1
fi

docker compose --env-file .env config >/dev/null

app_image="$(grep -E '^APP_IMAGE=' .env | tail -n 1 | cut -d= -f2-)"
if [[ ! "$app_image" =~ @sha256:[0-9a-f]{64}$ ]]; then
  echo "APP_IMAGE must be pinned to an immutable sha256 digest."
  echo "Example: ghcr.io/org/app@sha256:<64 hexadecimal characters>"
  exit 1
fi

for tls_file in \
  "$(grep -E '^TLS_CERT_PATH=' .env | tail -n 1 | cut -d= -f2-)" \
  "$(grep -E '^TLS_PRIVATE_KEY_PATH=' .env | tail -n 1 | cut -d= -f2-)"; do
  if [[ -z "$tls_file" || ! -f "$tls_file" ]]; then
    echo "TLS file not found: ${tls_file:-<empty>}"
    exit 1
  fi
done

# Pull everything before touching the currently-serving bootstrap or application stack.
docker compose --env-file .env pull

# Stop the infrastructure placeholder before binding the production Nginx to port 80.
if [[ -f "$PROJECT_ROOT/bootstrap/compose.yml" ]]; then
  docker compose -f "$PROJECT_ROOT/bootstrap/compose.yml" down || true
fi

docker compose --env-file .env up -d --remove-orphans

echo "Waiting for the Spring Boot health endpoint..."
for attempt in $(seq 1 60); do
  if curl --insecure -fsS https://127.0.0.1/health >/dev/null; then
    echo "Deployment healthy."
    docker compose --env-file .env ps
    exit 0
  fi

  if (( attempt % 6 == 0 )); then
    echo "Still waiting ($attempt/60)..."
  fi
  sleep 5
done

echo "Health check failed. Recent application logs:"
docker compose --env-file .env logs --tail=150 app
exit 1
