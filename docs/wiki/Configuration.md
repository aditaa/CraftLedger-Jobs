# Configuration

CraftLedger Jobs creates config files in `config/craftledger/`.

## `common.toml`

```toml
configVersion = 1
currencyEnabled = true
startingBalance = 100.0
currencyName = "coins"
currencySymbol = "$"
storageBackend = "json"
sqliteFile = "craftledger.sqlite"
maxBalance = 0.0
maxPayAmount = 0.0
payCooldownSeconds = 0
```

Set `currencyEnabled = false` to disable virtual currency features. Jobs can still pay XP while currency is disabled.

Set `storageBackend = "sqlite"` to store balances, job assignments, payout totals, and transactions in `world/craftledger/craftledger.sqlite`. Changing storage backend or SQLite file requires a server restart.

Run `/craftledger storage migrate json-to-sqlite dry-run` before migrating an existing JSON server. After the live migration, set `storageBackend = "sqlite"` and restart.

`maxBalance`, `maxPayAmount`, and `payCooldownSeconds` are optional economy safety controls. Set them to `0` to disable the limit.

## `shop.json`

Configure buy and sell prices with namespaced item ids such as `minecraft:bread`.

## `jobs.json`

Configure job definitions, job payout tables, payout notifications, placed-block anti-abuse, payout cooldowns, and daily payout limits.

Set `enabled = false` to disable jobs.

Currency payout maps:

- `blockBreak`
- `entityKill`

XP payout maps:

- `blockBreakXp`
- `entityKillXp`

`dailyPayoutLimit` uses UTC days. `0` disables the limit.

Keep `trackPlacedBlocks = true` on public servers. Player-placed blocks are tracked in `world/craftledger/placed_blocks.json` and do not produce job payouts when broken.

## `messages.json`

Configure command messages and payout messages. Missing keys fall back to built-in defaults. Placeholders use `{name}` syntax.

## Reload

Run `/craftledger reload` after editing config. Invalid config is rejected and the old active settings remain loaded.
