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

