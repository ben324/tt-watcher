#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out \
  src/com/riftbound/events/*.java \
  src/com/riftbound/events/json/*.java \
  src/com/riftbound/events/uvs/*.java \
  src/com/riftbound/events/playriftbound/*.java
java -cp out com.riftbound.events.ListEvents "$@"
