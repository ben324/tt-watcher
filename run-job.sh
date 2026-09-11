#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
docker compose --profile job run --rm --no-deps job
