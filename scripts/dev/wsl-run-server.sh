#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JDK_ROOT="${CRAFTLEDGER_JDK_ROOT:-$HOME/.local/share/craftledger/jdk-17}"

if [[ ! -x "$JDK_ROOT/bin/java" ]]; then
  "$ROOT/scripts/dev/wsl-setup-jdk.sh"
fi

export JAVA_HOME="$JDK_ROOT"
export PATH="$JAVA_HOME/bin:$PATH"

mkdir -p "$ROOT/run"
if [[ ! -f "$ROOT/run/eula.txt" ]]; then
  cat > "$ROOT/run/eula.txt" <<'EULA'
# By changing the setting below to TRUE you are indicating your agreement to the Minecraft EULA.
# https://aka.ms/MinecraftEULA
eula=true
EULA
fi

cd "$ROOT"
./gradlew runServer --console=plain --no-daemon
