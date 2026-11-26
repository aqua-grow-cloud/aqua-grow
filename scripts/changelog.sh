#!/bin/bash
# Prints a Markdown changelog: commit subjects since the previous release tag (or the last 50 commits).
set -euo pipefail
last="$(git describe --tags --abbrev=0 2>/dev/null || true)"
range="${last:+$last..}HEAD"
echo "## Changes${last:+ since $last}"
echo
git log --no-merges --max-count=50 --pretty='* %s (%an)' "$range"
