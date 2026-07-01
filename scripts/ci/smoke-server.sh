#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG_DIR="$ROOT/tmp/ci"
LOG_FILE="$LOG_DIR/smoke-server.log"
TIMEOUT_SECONDS="${CRAFTLEDGER_SMOKE_TIMEOUT_SECONDS:-600}"
ASSET_RETRIES="${CRAFTLEDGER_ASSET_RETRIES:-5}"

mkdir -p "$LOG_DIR"
rm -f "$LOG_FILE"

cd "$ROOT"
mkdir -p run
printf "eula=true\n" > run/eula.txt

for attempt in $(seq 1 "$ASSET_RETRIES"); do
  if ./gradlew downloadAssets --console=plain --no-daemon; then
    break
  fi

  if [[ "$attempt" == "$ASSET_RETRIES" ]]; then
    echo "downloadAssets failed after ${ASSET_RETRIES} attempts." >&2
    exit 1
  fi

  echo "downloadAssets failed on attempt ${attempt}; retrying..." >&2
  sleep 5
done

setsid bash -lc "./gradlew runServer --console=plain --no-daemon" >"$LOG_FILE" 2>&1 &
SERVER_PID="$!"

cleanup() {
  if kill -0 "$SERVER_PID" >/dev/null 2>&1; then
    kill -TERM "-$SERVER_PID" >/dev/null 2>&1 || true
    sleep 3
    kill -KILL "-$SERVER_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

deadline=$((SECONDS + TIMEOUT_SECONDS))
server_ready=0
mod_loaded=0

while (( SECONDS < deadline )); do
  if [[ -f "$LOG_FILE" ]]; then
    grep -q 'Done (.*)! For help, type "help"' "$LOG_FILE" && server_ready=1
    grep -q 'CraftLedger Jobs loaded' "$LOG_FILE" && mod_loaded=1

    if (( server_ready == 1 && mod_loaded == 1 )); then
      echo "Smoke test passed: Forge dev server booted and CraftLedger Jobs loaded."
      echo "Log: $LOG_FILE"
      exit 0
    fi
  fi

  if ! kill -0 "$SERVER_PID" >/dev/null 2>&1; then
    echo "Smoke test failed: server process exited before ready. See $LOG_FILE" >&2
    exit 1
  fi

  sleep 2
done

echo "Smoke test timed out after ${TIMEOUT_SECONDS}s. See $LOG_FILE" >&2
exit 1
