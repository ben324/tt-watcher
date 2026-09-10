#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
LIB_SRC="../riftbound-events/riftbound-events/src"
if [[ ! -d "$LIB_SRC/com/riftbound/events" ]]; then
  echo "Expected library sources at $LIB_SRC" >&2
  exit 1
fi
mkdir -p out
find out -name '*.class' -delete 2>/dev/null || true
javac -encoding UTF-8 -d out \
  "$LIB_SRC"/com/riftbound/events/*.java \
  "$LIB_SRC"/com/riftbound/events/json/*.java \
  "$LIB_SRC"/com/riftbound/events/uvs/*.java \
  "$LIB_SRC"/com/riftbound/events/playriftbound/*.java \
  src/com/riftbound/internal/*.java
exec java -cp out com.riftbound.internal.InternalMain "$@"
