# CurseForge Listing

## Summary

Server-side jobs, economy, shops, and payouts for Forge servers. No client install required.

## Description

# CraftLedger Jobs

CraftLedger Jobs is a server-side jobs and economy mod for Forge servers.

It is built for server owners who want balances, shops, sell commands, player payments, job payouts, and transaction logs without requiring players to install an extra client-side mod.

## Features

- Server-side install goal
- Player balances
- `/pay` player-to-player payments
- `/balance`, `/money`, and `/baltop`
- Configurable buy and sell shop
- `/sell hand` and `/sell all`
- Configurable jobs
- Currency and XP job payouts
- Job progression and levels
- Placed-block payout protection
- Operator balance tools
- Transaction log tail commands
- JSON storage by default
- Optional SQLite storage for larger servers
- Configurable messages

## Server-Only Goal

CraftLedger Jobs is intended to be installed on the server only. Players should be able to join without CraftLedger Jobs in their local modpack.

Before public release, each supported Minecraft and Forge target should be tested on a dedicated Forge server with a client that does not have CraftLedger Jobs installed.

## Configuration

Config files are generated on first server start in:

```text
config/craftledger/
```

Generated files include:

- `common.toml`
- `shop.json`
- `jobs.json`
- `messages.json`

World data is stored in:

```text
world/craftledger/
```

## Commands

Player commands include:

- `/balance`
- `/money`
- `/baltop`
- `/pay`
- `/sell hand`
- `/sell all`
- `/shop list`
- `/shop price`
- `/shop buy`
- `/jobs`
- `/job join`
- `/job current`
- `/job progress`
- `/job info`

Operator commands include reload, balance management, job management, storage migration, and transaction log tools.

## Storage

CraftLedger Jobs stores data as JSON by default.

For larger servers, SQLite storage can be enabled in `common.toml`:

```toml
storageBackend = "sqlite"
```

A migration command is available for moving existing JSON data to SQLite.

## Status

CraftLedger Jobs is currently pre-release. Please test carefully on a staging server before using it on a production server.

## Links

- Source: https://github.com/aditaa/CraftLedger-Jobs
- Wiki: https://github.com/aditaa/CraftLedger-Jobs/wiki
- Issues: https://github.com/aditaa/CraftLedger-Jobs/issues
