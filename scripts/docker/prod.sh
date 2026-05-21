#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

# Verify required secrets are present before touching anything
required_vars=(DB_PASSWORD DB_USERNAME AWS_ACCESS_KEY AWS_SECRET_KEY GOOGLE_GENAI_API_KEY GOOGLE_PROJECT_ID CF_TUNNEL_TOKEN)
for var in "${required_vars[@]}"; do
  if [[ -z "${!var:-}" ]]; then
    echo "ERROR: Required environment variable '$var' is not set." >&2
    exit 1
  fi
done

echo "Shutting down existing prod containers..."
docker compose -f docker-compose.prod.yml down

echo "Building and starting prod environment (app + Cloudflare tunnel)..."
docker compose -f docker-compose.prod.yml up --build -d

echo "Prod environment is up. Cloudflare tunnel is running."
