# Configuration

CraftLedger Jobs creates config files in `config/craftledger/`.

## `common.toml`

```toml
currencyEnabled = true
startingBalance = 100.0
currencyName = "coins"
currencySymbol = "$"
```

Set `currencyEnabled = false` to disable virtual currency features. Jobs can still pay XP while currency is disabled.

## `shop.json`

Configure buy and sell prices with namespaced item ids such as `minecraft:bread`.

## `jobs.json`

Configure job definitions, job payout tables, payout notifications, payout cooldowns, and daily payout limits.

Set `enabled = false` to disable jobs.

Currency payout maps:

- `blockBreak`
- `entityKill`

XP payout maps:

- `blockBreakXp`
- `entityKillXp`

`dailyPayoutLimit` uses UTC days. `0` disables the limit.

## Reload

Run `/craftledger reload` after editing config. Invalid config is rejected and the old active settings remain loaded.
