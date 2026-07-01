# Development Environment

CraftLedger Jobs can be built and smoke-tested mostly from WSL. Use Windows for IDE work and Minecraft client testing.

## Recommended Local Tools

- WSL2 with Ubuntu
- Java 17 JDK
- Git
- IntelliJ IDEA or another Java IDE
- Minecraft 1.20.1 client for manual join testing
- A dedicated Forge 47.4.10 server for final staging tests

## WSL Setup

Install a user-local Temurin JDK 17 inside WSL:

```bash
scripts/dev/wsl-setup-jdk.sh
```

Build from WSL:

```bash
scripts/dev/wsl-build.sh
```

Run the Forge development server:

```bash
scripts/dev/wsl-run-server.sh
```

The development server uses the repo `run/` directory, which is ignored by Git.
The first `runServer` may download a large Minecraft asset set. The helper retries `downloadAssets` up to five times because Mojang asset downloads can occasionally fail with transient `Connection reset` errors. Override this with `CRAFTLEDGER_ASSET_RETRIES=10 scripts/dev/wsl-run-server.sh` if needed.

Run a bounded smoke test:

```bash
scripts/dev/wsl-smoke-server.sh
```

The smoke test passes when the Forge development server reaches the Minecraft `Done (...)!` line and logs `CraftLedger Jobs loaded`. It writes a temporary log to `tmp/dev/wsl-smoke-server.log`.

## Windows Build

If `JAVA_HOME` points to a Java 17 JDK:

```powershell
.\scripts\dev\windows-build.ps1
```

The script also detects the temporary JDK path used during local setup if it exists.

If PowerShell blocks local scripts, run it with a process-only bypass:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\dev\windows-build.ps1
```

## WSL vs Windows

Use WSL for:

- Gradle builds
- Jar metadata validation
- Command-line smoke tests
- Dedicated server process experiments

Use Windows for:

- IntelliJ/IDE setup if preferred
- Minecraft client testing
- Joining a local or staging server
- Inspecting generated images/assets

For best WSL performance, a future clone under the Linux filesystem, such as `~/src/CraftLedger-Jobs`, will be faster than working through `/mnt/c/...`. The current Windows checkout still works.

## Test Layers

1. `./gradlew build`
2. Jar metadata validation through the Gradle `validateJarMetadata` task
3. Forge development server boot with `runServer`
4. Dedicated Forge server boot with the built jar
5. Client join test without CraftLedger Jobs installed
6. Multiplayer command tests for balance, pay, shop, sell, jobs, reload, and persistence

## Useful Paths

- Built jar: `build/libs/craftledger_jobs-*.jar`
- Dev server directory: `run/`
- Generated config during dev server runs: `run/config/craftledger/`
- Generated world data during dev server runs: `run/world/craftledger/`
