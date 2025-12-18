#!/usr/bin/env bash

cd "$(dirname "$0")/.."
export SPRING_PROFILES_ACTIVE=test
java -jar target/billing-0.0.1-SNAPSHOT.jar

# Run the script with:
#chmod +x scripts/run-test.sh
#./scripts/run-test.sh