#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
git pull --ff-only || true
if [ ! -f .env ]; then
  cat > .env <<'EOF'
SITE_ADDRESS=ttwatcher.com
CADDY_EMAIL=you@ttwatcher.com
INTERNAL_API_KEY=dev-internal
EOF
else
  grep -q '^SITE_ADDRESS=' .env || echo 'SITE_ADDRESS=ttwatcher.com' >> .env
  grep -q '^CADDY_EMAIL=' .env || echo 'CADDY_EMAIL=you@ttwatcher.com' >> .env
fi
mkdir -p data
docker compose up -d --remove-orphans
docker compose ps
echo
echo "HTTP:  http://$(curl -s --max-time 2 ifconfig.me || echo STATIC_IP)/"
echo "HTTPS: https://ttwatcher.com  (only after DNS A record points here)"
echo "Check DNS: dig +short ttwatcher.com"
