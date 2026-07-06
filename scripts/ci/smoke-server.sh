#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG_DIR="$ROOT/tmp/ci"
LOG_FILE="$LOG_DIR/smoke-server.log"
TIMEOUT_SECONDS="${CRAFTLEDGER_SMOKE_TIMEOUT_SECONDS:-600}"
FORGE_INSTALL_RETRIES="${CRAFTLEDGER_FORGE_INSTALL_RETRIES:-3}"
MC_TARGET="${CRAFTLEDGER_MC_TARGET:-}"
GRADLE_TARGET_ARGS=()
TARGET_PROPERTIES="$ROOT/gradle.properties"

if [[ -n "$MC_TARGET" ]]; then
  GRADLE_TARGET_ARGS+=("-Pcraftledger_mc_target=${MC_TARGET}")
  TARGET_PROPERTIES="$ROOT/gradle/minecraft-targets/${MC_TARGET}.properties"
fi

mkdir -p "$LOG_DIR"
rm -f "$LOG_FILE"

if [[ ! -f "$TARGET_PROPERTIES" ]]; then
  echo "Missing target properties file: $TARGET_PROPERTIES" >&2
  exit 1
fi

read_property() {
  local key="$1"
  grep -E "^${key}=" "$TARGET_PROPERTIES" | tail -n 1 | cut -d '=' -f 2-
}

MINECRAFT_VERSION="$(read_property minecraft_version)"
FORGE_VERSION="$(read_property forge_version)"
SERVER_DIR="$LOG_DIR/server-${MC_TARGET:-default}"
INSTALLER="$LOG_DIR/forge-${MINECRAFT_VERSION}-${FORGE_VERSION}-installer.jar"
INSTALLER_URL="https://maven.minecraftforge.net/net/minecraftforge/forge/${MINECRAFT_VERSION}-${FORGE_VERSION}/forge-${MINECRAFT_VERSION}-${FORGE_VERSION}-installer.jar"

cd "$ROOT"

./gradlew build "${GRADLE_TARGET_ARGS[@]}" --console=plain --no-daemon

JAR_PATH="$(find "$ROOT/build/libs" -maxdepth 1 -name "craftledger_jobs-mc${MINECRAFT_VERSION}-forge${FORGE_VERSION}-*.jar" ! -name '*-plain.jar' | head -n 1)"
if [[ -z "$JAR_PATH" && -z "$MC_TARGET" ]]; then
  JAR_PATH="$(find "$ROOT/build/libs" -maxdepth 1 -name 'craftledger_jobs-*.jar' ! -name '*-plain.jar' | head -n 1)"
fi

if [[ -z "$JAR_PATH" ]]; then
  echo "Could not locate built CraftLedger Jobs jar for Minecraft ${MINECRAFT_VERSION} / Forge ${FORGE_VERSION}." >&2
  exit 1
fi

rm -rf "$SERVER_DIR"
mkdir -p "$SERVER_DIR/mods"
printf "eula=true\n" > "$SERVER_DIR/eula.txt"
cp "$JAR_PATH" "$SERVER_DIR/mods/"

for attempt in $(seq 1 "$FORGE_INSTALL_RETRIES"); do
  if curl --fail --location --silent --show-error --output "$INSTALLER" "$INSTALLER_URL" \
    && (cd "$SERVER_DIR" && java -jar "$INSTALLER" --installServer >>"$LOG_FILE" 2>&1); then
    break
  fi

  if [[ "$attempt" == "$FORGE_INSTALL_RETRIES" ]]; then
    echo "Forge server install failed after ${FORGE_INSTALL_RETRIES} attempts. See $LOG_FILE" >&2
    exit 1
  fi

  echo "Forge server install failed on attempt ${attempt}; retrying..." >&2
  sleep 5
done

if [[ -f "$SERVER_DIR/libraries/net/minecraftforge/forge/${MINECRAFT_VERSION}-${FORGE_VERSION}/unix_args.txt" ]]; then
  touch "$SERVER_DIR/user_jvm_args.txt"
  SERVER_COMMAND="java @user_jvm_args.txt @libraries/net/minecraftforge/forge/${MINECRAFT_VERSION}-${FORGE_VERSION}/unix_args.txt nogui"
else
  SERVER_COMMAND="java -jar forge-${MINECRAFT_VERSION}-${FORGE_VERSION}.jar nogui"
fi

setsid bash -lc "cd '$SERVER_DIR' && $SERVER_COMMAND" >>"$LOG_FILE" 2>&1 &
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
