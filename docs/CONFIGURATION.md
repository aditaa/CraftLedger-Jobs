# Configuration

CraftLedger Jobs creates config files in `config/craftledger/`.

## `common.toml`

Controls global economy settings.

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

Set `currencyEnabled = false` to turn off CraftLedger's virtual currency features. Jobs can still run with XP payouts while currency is disabled.

Set `storageBackend = "sqlite"` to store player accounts, job assignments, daily payout totals, and transactions in a SQLite database under `world/craftledger/`. `sqliteFile` must be a file name, not a path. Backend changes require a server restart.

`maxBalance`, `maxPayAmount`, and `payCooldownSeconds` are economy safety controls. `0` disables the relevant limit.

## `shop.json`

Controls sell and buy prices.

```json
{
  "version": 1,
  "sellPrices": {
    "minecraft:cobblestone": 0.1
  },
  "buyPrices": {
    "minecraft:bread": {
      "price": 2.0,
      "maxStack": 16
    }
  }
}
```

## `jobs.json`

Controls jobs and event payouts.

```json
{
  "version": 1,
  "enabled": true,
  "allowSwitching": true,
  "notifyPayouts": true,
  "trackPlacedBlocks": true,
  "maxTrackedPlacedBlocks": 50000,
  "payoutCooldownSeconds": 0,
  "dailyPayoutLimit": 0,
  "jobs": {
    "miner": {
      "displayName": "Miner",
      "description": "Earn money from ores and mining materials.",
      "blockBreak": {
        "minecraft:coal_ore": 1.0
      },
      "entityKill": {},
      "blockBreakXp": {
        "minecraft:coal_ore": 1
      },
      "entityKillXp": {}
    }
  }
}
```

`blockBreak` and `entityKill` are currency payouts. `blockBreakXp` and `entityKillXp` are XP payouts. Configure either payout type, or both, for the same action.

`trackPlacedBlocks` blocks job payouts for player-placed blocks. This should stay enabled on public servers to prevent place-and-break payout farming. `maxTrackedPlacedBlocks` caps the persisted tracking set; older entries are evicted first.

## `messages.json`

Controls configurable player/admin messages. Missing keys fall back to built-in defaults.

```json
{
  "version": 1,
  "messages": {
    "balance.self": "Balance: {balance}",
    "pay.sent": "Paid {target} {amount}. Balance: {balance}",
    "job.payout": "Job payout: {payout}"
  }
}
```

Placeholders use `{name}` syntax. Unknown placeholders are left as text.

## Data Files

World data is stored in `world/craftledger/`:

- `players.json`
- `job_payouts.json`
- `placed_blocks.json`
- `transactions.log`
- `craftledger.sqlite` when SQLite storage is enabled

Back up these files with the world.

## Validation

`/craftledger reload` validates config before replacing the active settings. If validation fails, the old in-memory config remains active and the command reports the failing setting.

Current validation rules:

- `startingBalance` must be finite and greater than or equal to `0`.
- `currencyEnabled` controls whether virtual currency, balances, shops, sell commands, and pay commands are active.
- `currencyName` and `currencySymbol` must not be blank.
- `storageBackend` must be `json` or `sqlite`.
- `sqliteFile` must be a file name, not a path.
- `maxBalance` caps player balances from pay, sell, jobs, and admin add/set commands. `0` disables the cap.
- `maxPayAmount` caps a single `/pay` transfer. `0` disables the cap.
- `payCooldownSeconds` adds a per-player cooldown to `/pay`. `0` disables the cooldown.
- Shop item ids must use namespaced ids such as `minecraft:bread`.
- Shop prices must be finite and greater than `0`.
- Buy offer `maxStack` must be greater than or equal to `0`; `0` means use the item default.
- `enabled` controls whether the jobs system is active.
- `allowSwitching` controls whether players can join another job without leaving first.
- `notifyPayouts` controls whether players receive chat messages for each job payout.
- `trackPlacedBlocks` controls whether player-placed blocks are excluded from job payouts.
- `maxTrackedPlacedBlocks` caps the placed-block tracking set and must be greater than or equal to `0`.
- `payoutCooldownSeconds` blocks repeated payouts for the same player and same configured payout id inside the cooldown window. `0` disables the cooldown.
- `dailyPayoutLimit` caps each player's total job payout earnings per UTC day. `0` disables the cap.
- Job ids may contain lowercase letters, numbers, underscores, and hyphens.
- Job payout ids must be namespaced ids such as `minecraft:coal_ore`.
- Job payouts must be finite and greater than `0`.
- Job XP payouts must be whole numbers greater than `0`.
- `payoutCooldownSeconds` must be greater than or equal to `0`.
- `dailyPayoutLimit` must be finite and greater than or equal to `0`.

## Permissions

CraftLedger Jobs registers Forge permission nodes and falls back to Minecraft operator level `2` for admin-style commands when the default Forge permission handler is active:

- `craftledger_jobs.admin`
- `craftledger_jobs.balance.other`
- `craftledger_jobs.balance.top`
- `craftledger_jobs.transactions`

## JSON to SQLite Migration

Run a dry-run first:

```text
/craftledger storage migrate json-to-sqlite dry-run
```

Then run the live migration while `storageBackend = "json"`:

```text
/craftledger storage migrate json-to-sqlite
```

The command saves current JSON data, creates a timestamped backup directory under `world/craftledger/`, creates the SQLite file, and imports player accounts, job assignments, daily payout totals, and transactions. It does not edit `common.toml`; after a successful migration, set `storageBackend = "sqlite"` and restart the server.
