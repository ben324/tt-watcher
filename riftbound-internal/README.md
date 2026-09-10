# riftbound-internal

Job-only HTTP over `riftbound-events`. Separate process from `riftbound-api`.

Users never call this. The scanner will.

Default port **8081**. Auth: `X-Internal-Key` or `Authorization: Bearer`
(`INTERNAL_API_KEY`, default `dev-internal`).

## Routes

| Method | Path | Meaning |
|---|---|---|
| `GET` | `/internal/health` | liveness |
| `GET` | `/internal/sources` | vendor backends |
| `GET` | `/internal/events` | list + optional filters |
| `GET` | `/internal/events/{id}` | one listing for EVENT watch validation |
| `POST` | `/internal/find` | same, watch-shaped JSON body |

```
GET /internal/events?lat=40.7128&lng=-74.0060&radiusMiles=25&categoryContains=Nexus
```

```json
POST /internal/find
{
  "latitude": 40.7128,
  "longitude": -74.006,
  "radiusMiles": 25,
  "source": "uvs-hydra",
  "eventType": "LOCALS"
}
```

## Run

Windows:

```bat
cd riftbound-internal
run-internal.bat
```

Then start `riftbound-api` in another terminal.
