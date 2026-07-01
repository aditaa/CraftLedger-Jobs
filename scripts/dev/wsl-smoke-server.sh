#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG_DIR="$ROOT/tmp/dev"
LOG_FILE="$LOG_DIR/wsl-smoke-server.log"
TIMEOUT_SECONDS="${CRAFTLEDGER_SMOKE_TIMEOUT_SECONDS:-420}"

mkdir -p "$LOG_DIR"
rm -f "$LOG_FILE"

cd "$ROOT"
setsid bash -lc "scripts/dev/wsl-run-server.sh" >"$LOG_FILE" 2>&1 &
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

    if grep -q 'BUILD FAILED' "$LOG_FILE"; then
      echo "Smoke test failed during Gradle build. See $LOG_FILE" >&2
      exit 1
    fi

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
