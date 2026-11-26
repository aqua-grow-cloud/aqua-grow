#!/bin/bash
# Fails when frontend/openapi.json no longer matches what the API generates.
# Run after `./gradlew :api:test` (OpenApiExportTest writes api/build/openapi/openapi.json).
set -euo pipefail
cd "$(dirname "$0")/.."
if ! diff <(jq -S . api/build/openapi/openapi.json) <(jq -S . frontend/openapi.json) > /tmp/openapi.diff; then
  head -60 /tmp/openapi.diff
  echo "frontend/openapi.json is out of date. Run scripts/export-openapi.sh --client and commit the result." >&2
  exit 1
fi
echo "OpenAPI contract is up to date"
