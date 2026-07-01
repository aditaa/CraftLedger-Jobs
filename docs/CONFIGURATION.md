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
  "jobs": {
    "miner": {
      "displayName": "Miner",
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
- Job ids may contain lowercase letters, numbers, underscores, and hyphens.
- Job payout ids must be namespaced ids such as `minecraft:coal_ore`.
- Job payouts must be finite and greater than `0`.
