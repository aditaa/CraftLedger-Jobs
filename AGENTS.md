# AGENTS.md

Guidance for AI coding agents and human maintainers working in this repository.

## Project Intent

CraftLedger Jobs is a Forge 1.20.1 server-side economy and jobs mod for Minecraft servers. The core promise is that server owners can install the mod on the server without requiring players to install it on their clients.

Do not add gameplay features that require client-side assets, screens, packets, keybinds, or client-only classes unless the project explicitly changes direction.

## Hard Requirements

- Minecraft version: `1.20.1`
- Forge target: `47.4.10`
- Java bytecode target: `17`
- Mod id: `craftledger_jobs`
- Package root: `dev.monkeycraft.craftledgerjobs`
- The mod must remain server-compatible for clients that do not have the mod installed.
- `src/main/resources/META-INF/mods.toml` must keep `displayTest="IGNORE_SERVER_VERSION"` unless there is a deliberate, reviewed replacement.
- Avoid registering custom networking channels unless the clientless-join behavior is tested afterward.

## Repository Safety

- Do not publish releases or upload CurseForge artifacts without explicit maintainer approval.
- Do not touch production servers from repository tasks.
- Do not commit generated build outputs such as `build/`, `.gradle/`, or jars.
- Preserve user changes in the working tree. If unrelated changes exist, work around them.

## Development Workflow

1. Read `README.md`, `CONTRIBUTING.md`, and this file before larger changes.
2. Keep changes scoped to one feature or fix.
3. Run `./gradlew build` before submitting changes.
4. If commands, config formats, or data formats change, update docs in the same change.
5. For economy logic, prefer explicit checks and clear transaction logging over clever shortcuts.

## Testing Expectations

Minimum checks before a PR is considered healthy:

- Gradle build passes.
- The jar contains `META-INF/mods.toml`.
- The expanded jar metadata includes `displayTest="IGNORE_SERVER_VERSION"`.
- Commands touched by the change are tested on a dedicated Forge server when practical.

Before the first public release, the project should also have manual staging evidence that:

- A Forge 1.20.1 server boots with the jar.
- A client without CraftLedger Jobs can join.
- No rejected channel/version error appears.
- Balance, pay, shop, sell, and jobs commands work server-side.

## Code Style

- Keep Java code simple and readable.
- Use server-side Minecraft and Forge APIs only.
- Use UUIDs for persistent player identity.
- Use atomic or backup-safe writes for durable data.
- Keep transaction logs append-only.
- Prefer configurable values over hard-coded economy tuning.

## Useful Commands

Windows:

```powershell
.\gradlew.bat build
```

Linux/macOS/GitHub Actions:

```bash
./gradlew build
```

