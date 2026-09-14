# tt-watcher

Live site: [https://ttwatcher.com](https://ttwatcher.com)

Email alerts for Riftbound listings on the official UVS locator.

## What it does

**Watch a search** — pick an area, distance, event type, and dates. The job emails you when a *new* matching event appears, or when a matching event that was full gets an opening. Existing open listings are recorded when you save the watch so they are not mailed as new.

**Watch one event** — paste an event id. The job emails you when that listing has an opening, then removes the watch.

SMS notifications are in progress (expensive)

## Repo

| Path | Role |
|---|---|
| `tt-watcher/` | Public UI |
| `riftbound-api/` | Signed-in user API and static site |
| `riftbound-internal/` | Event lookup used by the API and the job (not public) |
| `riftbound-events/` | Java mapping over UVS Hydra (PlayRiftbound stub) |

Accounts and watches persist in `data/store.json` on the server.
