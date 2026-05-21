#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

echo "Shutting down existing containers..."
docker compose -f docker-compose.dev.yml down

echo "Building and starting dev environment..."
docker compose -f docker-compose.dev.yml up --build -d

echo "Dev environment is up."
