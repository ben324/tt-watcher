# tt-watcher

Riftbound event alerts: watch a search (new listings) or an event id (seats).

```
riftbound-events/     Java library (UVS Hydra + PlayRiftbound stub)
riftbound-api/        User API + serves tt-watcher at /
riftbound-internal/   Job-only event lookup (port 8081)
riftbound-schema/     Document schema
tt-watcher/           Frontend
```

## Run with Docker

Install Docker Desktop, then from the repo root:

```sh
docker compose up --build
```

Open http://127.0.0.1:8080/

One SEARCH job pass:

```sh
docker compose --profile job run --rm job
```

Stop with Ctrl+C, then `docker compose down`.

## Run without Docker

Need JDK 17+.

Windows: `run-local.bat` or the two `run-*.bat` scripts.
