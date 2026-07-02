# Configuration

CraftLedger Jobs creates config files in `config/craftledger/`.

## `common.toml`

```toml
startingBalance = 100.0
currencyName = "coins"
currencySymbol = "$"
```

## `shop.json`

Configure buy and sell prices with namespaced item ids such as `minecraft:bread`.

## `jobs.json`

Configure job definitions, job payout tables, payout notifications, payout cooldowns, and daily payout limits.

`dailyPayoutLimit` uses UTC days. `0` disables the limit.

## Reload

Run `/craftledger reload` after editing config. Invalid config is rejected and the old active settings remain loaded.
