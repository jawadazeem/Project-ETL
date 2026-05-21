#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

SPRING_PROFILES_ACTIVE=dev java -jar target/blueprint.jar
