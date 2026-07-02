# Troubleshooting

## Client cannot join

Confirm the server jar metadata still contains `displayTest="IGNORE_SERVER_VERSION"` and Forge dependency metadata with `side="SERVER"`. CI validates this, but staging should still test with a client that does not have CraftLedger Jobs installed.

## Config reload fails

Read the error from `/craftledger reload`. Common causes:

- Item ids are not namespaced, such as `bread` instead of `minecraft:bread`.
- Prices or payouts are `0`, negative, `NaN`, or infinite.
- Job ids contain spaces or uppercase letters.

## Shop command does not find an item

Use the same namespaced id that appears in `shop.json`, for example `/shop price minecraft:bread`.

## Job payouts are missing

Check that the player has joined the job, the event is listed in `jobs.json`, and `dailyPayoutLimit` or `payoutCooldownSeconds` is not blocking the payout.

If `currencyEnabled = false`, only XP payouts from `blockBreakXp` and `entityKillXp` are paid. Currency payout maps are ignored while currency is disabled.

## Balances look wrong after restart

Check `world/craftledger/players.json` and server logs for save errors. Back up `world/craftledger/` with the world.

## Need recent audit entries

Use `/craftledger transactions tail [lines]`. The command is capped at 50 lines.
