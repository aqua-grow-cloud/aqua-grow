#!/bin/bash
# Regenerates frontend/openapi.json from the API and (optionally) the Orval client.
#   scripts/export-openapi.sh           # refresh frontend/openapi.json
#   scripts/export-openapi.sh --client  # ...and run `npm run api:generate`
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew :api:test --tests '*OpenApiExportTest' --rerun-tasks -q
jq -S . api/build/openapi/openapi.json > frontend/openapi.json
if [ "${1:-}" = "--client" ]; then
  (cd frontend && npm run api:generate)
fi
