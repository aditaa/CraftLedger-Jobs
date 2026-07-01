#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JDK_ROOT="${CRAFTLEDGER_JDK_ROOT:-$HOME/.local/share/craftledger/jdk-17}"

if [[ ! -x "$JDK_ROOT/bin/java" ]]; then
  "$ROOT/scripts/dev/wsl-setup-jdk.sh"
fi

export JAVA_HOME="$JDK_ROOT"
export PATH="$JAVA_HOME/bin:$PATH"
ASSET_RETRIES="${CRAFTLEDGER_ASSET_RETRIES:-5}"

mkdir -p "$ROOT/run"
if [[ ! -f "$ROOT/run/eula.txt" ]]; then
  cat > "$ROOT/run/eula.txt" <<'EULA'
# By changing the setting below to TRUE you are indicating your agreement to the Minecraft EULA.
# https://aka.ms/MinecraftEULA
eula=true
EULA
fi

cd "$ROOT"
for attempt in $(seq 1 "$ASSET_RETRIES"); do
  if ./gradlew downloadAssets --console=plain --no-daemon; then
    break
  fi

  if [[ "$attempt" == "$ASSET_RETRIES" ]]; then
    echo "Minecraft assets failed to download after $ASSET_RETRIES attempts." >&2
    exit 1
  fi

  echo "Minecraft asset download failed; retrying ($attempt/$ASSET_RETRIES)..." >&2
  sleep 5
done

./gradlew runServer --console=plain --no-daemon
