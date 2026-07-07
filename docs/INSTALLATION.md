# Installation and Administration

CraftLedger Jobs is a server-side Forge mod for Minecraft 1.20.1.

## Requirements

- Minecraft `1.20.1`
- Forge `47.4.10`
- Java `17` or newer to run the server
- A dedicated Forge server

Players should not need CraftLedger Jobs installed in their client modpack.

Volunteer testers who only have Minecraft clients should follow the [Volunteer Testing Guide](VOLUNTEER_TESTING.md). They should download the beta release for their Minecraft version, install it in a Forge client, start a fresh singleplayer world, and run the documented tests.

## Install

1. Build the jar with `./gradlew build`.
2. Copy the reobfuscated jar from `build/libs/` into the server `mods/` folder.
3. Start the server.
4. Confirm the server creates `config/craftledger/`.
5. Join from a client without CraftLedger Jobs installed.
6. Run the manual checklist in [Testing](TESTING.md).

The generated default configs are meant to be playable without customization. A server can drop in the jar, restart, and get starter balances, a small buy/sell shop, and four default jobs.

## Files

Config files live in `config/craftledger/`:

- `common.toml`
- `shop.json`
- `jobs.json`
- `messages.json`

World data lives in `world/craftledger/`:

- `players.json`
- `job_payouts.json`
- `placed_blocks.json`
- `transactions.log`

Back up the world data files with the world. They contain balances, job selections, placed-block anti-abuse tracking, daily payout totals, and the transaction audit trail.

If `storageBackend = "sqlite"` is enabled in `common.toml`, balances, jobs, daily payout totals, and transactions move to `world/craftledger/craftledger.sqlite`. `placed_blocks.json` remains a separate anti-abuse tracking file.

For an existing JSON-backed server, run `/craftledger storage migrate json-to-sqlite dry-run`, then `/craftledger storage migrate json-to-sqlite`. After it succeeds, set `storageBackend = "sqlite"` in `common.toml` and restart.

## Reloading

Use `/craftledger reload` after editing config files. Reload validates config before replacing the active settings. If validation fails, the previous active settings remain in memory.

Set `currencyEnabled = false` in `common.toml` to turn off virtual currency features. Set `enabled = false` in `jobs.json` to turn off jobs.

Changing `storageBackend` or `sqliteFile` requires a full server restart. `/craftledger reload` rejects storage backend changes so the server does not silently keep using the old open data store.

## Permissions

CraftLedger Jobs registers these Forge permission nodes:

- `craftledger_jobs.admin`
- `craftledger_jobs.balance.other`
- `craftledger_jobs.balance.top`
- `craftledger_jobs.transactions`

With the default Forge permission handler, admin-style commands fall back to Minecraft operator level `2`.

## Release Notes

CraftLedger Jobs is still pre-release. Do not publish a public jar until the dedicated-server checklist passes on a staging server.
