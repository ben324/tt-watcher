# Lightsail deploy

Public site with no port: `https://your-domain.com` via Caddy on this box.

## 1. Create the instance

1. AWS console → **Lightsail**.
2. Create instance:
   - Platform: **Linux**
   - OS: **Ubuntu 24.04**
   - Size: **$5 or $7** (1 GB RAM is tight; 2 GB is safer for two Java containers)
   - Name: `tt-watcher`
3. Create a **static IP** and attach it to the instance. Use that IP in DNS.

## 2. Firewall

Instance → Networking → IPv4 firewall:

| App | Port | Protocol |
|---|---|---|
| SSH | 22 | TCP |
| HTTP | 80 | TCP |
| HTTPS | 443 | TCP |

Do **not** open 8080 or 8081.

## 3. Domain

At your registrar, A record:

- `@` or `watcher` → the Lightsail **static IP**

Example: `watcher.example.com` → `3.x.x.x`

## 4. Install and start

SSH in (Lightsail “Connect” or `ssh ubuntu@STATIC_IP`), then:

```sh
curl -fsSL https://raw.githubusercontent.com/ben324/tt-watcher/main/lightsail-setup.sh | bash
```

Edit env:

```sh
nano ~/tt-watcher/.env
```

```
SITE_ADDRESS=watcher.example.com
CADDY_EMAIL=you@example.com
```

```sh
cd ~/tt-watcher
docker compose up --build -d
```

Site: `https://watcher.example.com`
