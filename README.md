# tt-watcher

Riftbound event alerts: watch a search (new listings) or an event id (seats).

```
riftbound-events/     Java library (UVS Hydra + PlayRiftbound stub)
riftbound-api/        User API + serves tt-watcher at /
riftbound-internal/   Job-only event lookup (port 8081)
riftbound-schema/     Document schema
tt-watcher/           Frontend
```

## Run

Need JDK 17+.

**Windows** (two terminals):

```bat
cd riftbound-internal
run-internal.bat

cd riftbound-api
run-api.bat
```

**macOS / Linux:**

```sh
cd riftbound-internal && ./run-internal.sh
cd riftbound-api && ./run-api.sh
```

Open http://127.0.0.1:8080/

Internal service must be running to pin an event id.
