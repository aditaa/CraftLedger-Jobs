<p align="center">
  <img src=".github/assets/logo.png" alt="CraftLedger Jobs logo" width="160">
</p>

# CraftLedger Jobs

[![CI](https://github.com/aditaa/CraftLedger-Jobs/actions/workflows/ci.yml/badge.svg)](https://github.com/aditaa/CraftLedger-Jobs/actions/workflows/ci.yml)
[![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-62b47a)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
[![Forge 47.4.10](https://img.shields.io/badge/Forge-47.4.10-f16436)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)

Server-side jobs and economy for Forge 1.20.1 servers.

CraftLedger Jobs is built for Minecraft servers that want balances, shops, sell commands, and job payouts without requiring players to install an extra client-side mod.

Default configs are generated on first server start, so the mod is intended to be usable as a drop-in jar before deeper customization.

## Project Status

CraftLedger Jobs is pre-release. The first jar release is intentionally not published yet.

The current focus is making the repository trustworthy before public distribution:

- repeatable Gradle builds
- scaffolded multi-version Forge target profiles
- CI pass/fail/status reporting
- unit, repository, server-only source, jar metadata, and Forge server smoke checks
- dependency review and CodeQL security scanning
- server-only metadata validation
- clear contribution and security docs
- dedicated-server test checklists
- conservative economy behavior with transaction logs

The repository social preview asset is available at `.github/assets/social-preview.png`.

## Target

- Minecraft: `1.20.1`
- Forge: `47.4.10`
- Java target: `17` bytecode
- Mod id: `craftledger_jobs`
- Package: `dev.monkeycraft.craftledgerjobs`

## Server-Only Goal

The mod is designed to be installed on the server only. Players should be able to join without CraftLedger Jobs in their local modpack.

CI validates that the built jar contains:

- `META-INF/mods.toml`
- `modId="craftledger_jobs"`
- `displayTest="IGNORE_SERVER_VERSION"`
- `side="SERVER"` dependency metadata

Dedicated server testing is still required before release. See [Testing](docs/TESTING.md).

## Commands

Player commands:

- `/balance`
- `/balance <player>` for operators
- `/money`
- `/baltop [page]`
- `/pay <player> <amount>`
- `/sell hand [amount]`
- `/sell all [item]`
- `/shop list [page]`
- `/shop sell [page]`
- `/shop price <item>`
- `/shop buy <item> [amount]`
- `/jobs [page]`
- `/job join <job>`
- `/job current`
- `/job progress [job]`
- `/job leave`
- `/job info [job] [page]`

Operator commands:

- `/craftledger reload`
- `/craftledger balance top [page]`
- `/craftledger balance get <player>`
- `/craftledger balance set <player> <amount>`
- `/craftledger balance add <player> <amount>`
- `/craftledger balance take <player> <amount>`
- `/craftledger player info <player>`
- `/craftledger job set <player> <job>`
- `/craftledger job clear <player>`
- `/craftledger job level set <player> <job> <level>`
- `/craftledger job level reset <player> <job>`
- `/craftledger storage migrate json-to-sqlite [dry-run]`
- `/craftledger shop reload`
- `/craftledger jobs reload`
- `/craftledger transactions tail [lines]`
- `/craftledger transactions tail player <player> [lines]`

Offline admin balance commands can target players by last known name or UUID after they have joined the server at least once.

## Server Files

Config files are created in `config/craftledger/`:

- `common.toml`
- `shop.json`
- `jobs.json`
- `messages.json`

World data is stored in `world/craftledger/`:

- `players.json`
- `job_payouts.json`
- `placed_blocks.json`
- `transactions.log`

Player data is saved by UUID with last known player name. JSON writes go through a temporary file and atomic replace when the filesystem supports it.

For larger servers, `common.toml` can set `storageBackend = "sqlite"` to store player balances, jobs, job payout totals, and transactions in `world/craftledger/craftledger.sqlite`. The default remains JSON.

Use `/craftledger storage migrate json-to-sqlite dry-run` before switching an existing JSON-backed server to SQLite. The live migration creates a timestamped backup under `world/craftledger/`, writes the SQLite file, then requires setting `storageBackend = "sqlite"` and restarting.

See [Configuration](docs/CONFIGURATION.md) for examples.
See [Installation and Administration](docs/INSTALLATION.md) for server setup and backup notes.

Current permission gates use operator level 2 as the fallback:

- `craftledger_jobs.admin`
- `craftledger_jobs.balance.other`
- `craftledger_jobs.balance.top`
- `craftledger_jobs.transactions`

## Build

Requirements:

- Java 17 JDK
- Git

Linux/macOS:

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

The built jar is created under `build/libs/`.

Scaffolded Forge target profiles are available for future compatibility work. The default supported build remains Minecraft `1.20.1` / Forge `47.4.10`.

List target profiles:

```bash
./gradlew listMinecraftTargets
```

Build a named target profile:

```bash
./gradlew build -Pcraftledger_mc_target=1.19.2
```

See [Multi-Version Release Scaffolding](docs/MULTIVERSION.md) before publishing jars for any non-primary target.

Release candidate jars for all scaffolded target profiles are built by the `Release Build` GitHub Actions workflow on `v*` tags or manual dispatch.

CurseForge uploads are handled by a separate manual `CurseForge Publish` workflow after release validation is complete.

For local WSL/Windows setup details, see [Development Environment](docs/DEVELOPMENT.md).

## Testing

Automated checks:

```bash
./gradlew build
```

Manual server testing is required before the first public release:

- Forge 1.20.1 server boots with the jar.
- Client without CraftLedger Jobs can join.
- No rejected mod channel/version error appears.
- Economy, shop, sell, jobs, reload, and admin commands work.
- Balances persist across restart.

See [Testing](docs/TESTING.md) and [Release Checklist](docs/RELEASE_CHECKLIST.md).

## Contributing

Contributions are welcome once the basics are stable.

Start with:

- [Contributing Guide](CONTRIBUTING.md)
- [Agent/Maintainer Guidance](AGENTS.md)
- [Roadmap](ROADMAP.md)
- [Known Limitations](docs/KNOWN_LIMITATIONS.md)
- [Wiki Source](docs/WIKI.md)
- [Security Policy](SECURITY.md)

Please keep the server-only promise central to changes. Features that require client-side install are out of scope for the current project goal.

## License

CraftLedger Jobs is licensed under GPL-3.0. See [LICENSE](LICENSE).
