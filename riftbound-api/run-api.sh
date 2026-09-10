#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
LIB_SRC="../riftbound-events/riftbound-events/src"
mkdir -p out
javac -encoding UTF-8 -d out \
  "$LIB_SRC"/com/riftbound/events/*.java \
  "$LIB_SRC"/com/riftbound/events/json/*.java \
  "$LIB_SRC"/com/riftbound/events/uvs/*.java \
  "$LIB_SRC"/com/riftbound/events/playriftbound/*.java \
  src/com/riftbound/api/*.java
exec java -cp out com.riftbound.api.ApiMain "$@"
