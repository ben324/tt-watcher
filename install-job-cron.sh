#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
chmod +x run-job.sh
LINE="*/15 * * * * cd $HOME/tt-watcher && ./run-job.sh >> $HOME/tt-watcher/job.log 2>&1"
(crontab -l 2>/dev/null | grep -v run-job.sh; echo "$LINE") | crontab -
echo "Installed:"
crontab -l | grep run-job
