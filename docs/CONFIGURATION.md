# Configuration

CraftLedger Jobs creates config files in `config/craftledger/`.

## `common.toml`

Controls global economy settings.

```toml
startingBalance = 100.0
currencyName = "coins"
currencySymbol = "$"
```

## `shop.json`

Controls sell and buy prices.

```json
{
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
  "allowSwitching": true,
  "notifyPayouts": true,
  "payoutCooldownSeconds": 0,
  "dailyPayoutLimit": 0,
  "jobs": {
    "miner": {
      "displayName": "Miner",
      "description": "Earn money from ores and mining materials.",
      "blockBreak": {
        "minecraft:coal_ore": 1.0
      },
      "entityKill": {}
    }
  }
}
```

## Data Files

World data is stored in `world/craftledger/`:

- `players.json`
- `transactions.log`

Back up these files with the world.

## Validation

`/craftledger reload` validates config before replacing the active settings. If validation fails, the old in-memory config remains active and the command reports the failing setting.

Current validation rules:

- `startingBalance` must be finite and greater than or equal to `0`.
- `currencyName` and `currencySymbol` must not be blank.
- Shop item ids must use namespaced ids such as `minecraft:bread`.
- Shop prices must be finite and greater than `0`.
- Buy offer `maxStack` must be greater than or equal to `0`; `0` means use the item default.
- `allowSwitching` controls whether players can join another job without leaving first.
- `notifyPayouts` controls whether players receive chat messages for each job payout.
- `payoutCooldownSeconds` blocks repeated payouts for the same player and same configured payout id inside the cooldown window. `0` disables the cooldown.
- `dailyPayoutLimit` caps each player's total job payout earnings per UTC day. `0` disables the cap.
- Job ids may contain lowercase letters, numbers, underscores, and hyphens.
- Job payout ids must be namespaced ids such as `minecraft:coal_ore`.
- Job payouts must be finite and greater than `0`.
- `payoutCooldownSeconds` must be greater than or equal to `0`.
- `dailyPayoutLimit` must be finite and greater than or equal to `0`.

## Permissions

CraftLedger Jobs registers Forge permission nodes and falls back to Minecraft operator level `2` for admin-style commands when the default Forge permission handler is active:

- `craftledger_jobs.admin`
- `craftledger_jobs.balance.other`
- `craftledger_jobs.balance.top`
- `craftledger_jobs.transactions`
