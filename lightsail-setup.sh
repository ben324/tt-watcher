#!/bin/bash
set -euo pipefail
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker ubuntu || sudo usermod -aG docker "$USER"
APP_DIR="$HOME/tt-watcher"
if [ ! -d "$APP_DIR" ]; then git clone https://github.com/ben324/tt-watcher.git "$APP_DIR"; fi
cd "$APP_DIR"
git pull --ff-only || true
if [ ! -f .env ]; then
  cat > .env <<'EOF'
SITE_ADDRESS=ttwatcher.com
CADDY_EMAIL=you@ttwatcher.com
INTERNAL_API_KEY=dev-internal
EOF
fi
bash "$APP_DIR/start-live.sh"
