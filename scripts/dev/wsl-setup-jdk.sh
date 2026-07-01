#!/usr/bin/env bash
set -euo pipefail

JDK_ROOT="${CRAFTLEDGER_JDK_ROOT:-$HOME/.local/share/craftledger/jdk-17}"
DOWNLOAD_DIR="$(dirname "$JDK_ROOT")"
ARCHIVE="$DOWNLOAD_DIR/temurin-jdk17.tar.gz"

if [[ -x "$JDK_ROOT/bin/java" ]]; then
  "$JDK_ROOT/bin/java" -version
  exit 0
fi

mkdir -p "$DOWNLOAD_DIR"
curl -L "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse" -o "$ARCHIVE"
rm -rf "$JDK_ROOT"
mkdir -p "$JDK_ROOT"
tar -xzf "$ARCHIVE" -C "$JDK_ROOT" --strip-components=1
"$JDK_ROOT/bin/java" -version
