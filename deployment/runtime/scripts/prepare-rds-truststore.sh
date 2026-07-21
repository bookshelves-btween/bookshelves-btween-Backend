#!/usr/bin/env bash
set -Eeuo pipefail

RUNTIME_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RDS_CA_URL="${RDS_CA_URL:-https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem}"
JDK_IMAGE="eclipse-temurin:21-jdk-jammy@sha256:9d8dcf999b0bce2453e913823595a5ff2a4e8e9e5d5241b45280d0ff069818ec"

cd "$RUNTIME_DIR"
set -a
source .env
set +a

: "${RDS_CA_BUNDLE_PATH:?RDS_CA_BUNDLE_PATH is required}"
: "${RDS_TRUSTSTORE_PATH:?RDS_TRUSTSTORE_PATH is required}"
: "${RDS_TRUSTSTORE_PASSWORD:?RDS_TRUSTSTORE_PASSWORD is required}"

install -d -m 0700 "$(dirname "$RDS_CA_BUNDLE_PATH")"
install -d -m 0700 "$(dirname "$RDS_TRUSTSTORE_PATH")"

work_dir="$(mktemp -d)"
temporary_ca="$(mktemp "$(dirname "$RDS_CA_BUNDLE_PATH")/.global-bundle.XXXXXX.pem")"
temporary_truststore="$(mktemp "$(dirname "$RDS_TRUSTSTORE_PATH")/.rds-truststore.XXXXXX.jks")"
cleanup() {
  rm -rf -- "$work_dir"
  rm -f -- "$temporary_ca" "$temporary_truststore"
}
trap cleanup EXIT

curl --proto '=https' --tlsv1.2 -fsSL "$RDS_CA_URL" -o "$work_dir/global-bundle.pem"
if ! grep -q -- '-----BEGIN CERTIFICATE-----' "$work_dir/global-bundle.pem"; then
  echo 'Downloaded RDS CA bundle does not contain a certificate.' >&2
  exit 1
fi

(
  cd "$work_dir"
  awk 'split_after == 1 {n++; split_after=0} /-----END CERTIFICATE-----/ {split_after=1} {print > "rds-ca-" n+1 ".pem"}' global-bundle.pem
)

rm -f -- "$temporary_truststore"
docker run --rm \
  --user "$(id -u):$(id -g)" \
  -e STORE_PASSWORD="$RDS_TRUSTSTORE_PASSWORD" \
  -v "$work_dir:/work" \
  "$JDK_IMAGE" \
  bash -c '
    set -Eeuo pipefail
    for certificate in /work/rds-ca-*.pem; do
      alias="$(basename "$certificate" .pem)"
      keytool -importcert -noprompt \
        -alias "$alias" \
        -file "$certificate" \
        -keystore /work/rds-truststore.jks \
        -storepass:env STORE_PASSWORD
    done
    keytool -list \
      -keystore /work/rds-truststore.jks \
      -storepass:env STORE_PASSWORD >/dev/null
  '

cp "$work_dir/global-bundle.pem" "$temporary_ca"
cp "$work_dir/rds-truststore.jks" "$temporary_truststore"
chmod 0644 "$temporary_ca" "$temporary_truststore"
mv -f -- "$temporary_ca" "$RDS_CA_BUNDLE_PATH"
mv -f -- "$temporary_truststore" "$RDS_TRUSTSTORE_PATH"

echo "RDS CA bundle ready: $RDS_CA_BUNDLE_PATH"
echo "RDS Java truststore ready: $RDS_TRUSTSTORE_PATH"
