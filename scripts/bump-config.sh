#!/bin/bash
# Point the cloudpe manifests at new image tags and check that the affected overlays still build.
#   scripts/bump-config.sh <aqua-grow-config checkout> <tag> <service>...
# Services: api, mcp, resource-monitor, frontend.
set -euo pipefail

CONFIG="${1:?config repo checkout}"
TAG="${2:?image tag}"
shift 2
[ "$#" -gt 0 ] || { echo "no services given" >&2; exit 2; }

REGISTRY="ghcr.io/aqua-grow-cloud"

overlay_for() {
  case "$1" in
    api)              echo "apps/api/overlays/cloudpe/api" ;;
    resource-monitor) echo "apps/api/overlays/cloudpe/resource-monitor" ;;
    mcp)              echo "apps/mcp/overlays/cloudpe" ;;
    frontend)         echo "apps/frontend/overlays/cloudpe" ;;
    *) echo "unknown service: $1" >&2; return 1 ;;
  esac
}

for svc in "$@"; do
  overlay="$(overlay_for "$svc")"
  image="$REGISTRY/aqua-grow-$svc"
  files="$(grep -rl --include='*.yaml' "$image:" "$CONFIG/apps" || true)"
  [ -n "$files" ] || { echo "no manifest references $image" >&2; exit 1; }
  for f in $files; do
    sed -E -i.bak "s|(${image}:)[0-9A-Za-z._-]+|\1${TAG}|g" "$f"
    rm -f "$f.bak"
  done
  echo "$svc -> $TAG"
  kustomize build "$CONFIG/$overlay" > /dev/null
  echo "  $overlay builds"
done
