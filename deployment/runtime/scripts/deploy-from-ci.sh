#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <immutable-image-reference>" >&2
  exit 64
fi

image_ref="$1"
if [[ ! "$image_ref" =~ ^ghcr\.io/[a-z0-9._/-]+@sha256:[0-9a-f]{64}$ ]]; then
  echo "Image must be a lowercase GHCR reference pinned by sha256 digest." >&2
  exit 65
fi

runtime_dir="/opt/bookshelf/runtime"
env_file="$runtime_dir/.env"

if [[ ! -f "$env_file" ]]; then
  echo "Missing $env_file. Complete the one-time runtime setup before CI deployment." >&2
  exit 66
fi

sed -i "s|^APP_IMAGE=.*|APP_IMAGE=$image_ref|" "$env_file"
exec bash "$runtime_dir/scripts/deploy.sh"
