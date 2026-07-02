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

## Project Status

CraftLedger Jobs is pre-release. The first jar release is intentionally not published yet.

The current focus is making the repository trustworthy before public distribution:

- repeatable Gradle builds
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
- `/job leave`
- `/job info [job] [page]`

Operator commands:

- `/craftledger reload`
- `/craftledger balance get <player>`
- `/craftledger balance set <player> <amount>`
- `/craftledger balance add <player> <amount>`
- `/craftledger balance take <player> <amount>`
- `/craftledger shop reload`
- `/craftledger jobs reload`

Offline admin balance commands can target players by last known name or UUID after they have joined the server at least once.

## Server Files

Config files are created in `config/craftledger/`:

- `common.toml`
- `shop.json`
- `jobs.json`
- `messages.json`

World data is stored in `world/craftledger/`:

- `players.json`
- `transactions.log`

Player data is saved by UUID with last known player name. Writes go through a temporary file and atomic replace when the filesystem supports it.

See [Configuration](docs/CONFIGURATION.md) for examples.

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
- [Security Policy](SECURITY.md)

Please keep the server-only promise central to changes. Features that require client-side install are out of scope for the current project goal.

## License

CraftLedger Jobs is licensed under GPL-3.0. See [LICENSE](LICENSE).
