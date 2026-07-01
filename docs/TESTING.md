# Testing

CraftLedger Jobs needs both automated checks and dedicated-server validation.

## Automated Checks

Run:

```bash
./gradlew build
```

CI also validates that the built jar contains server-only metadata:

- `META-INF/mods.toml` exists.
- `displayTest="IGNORE_SERVER_VERSION"` is present.
- The mod id is `craftledger_jobs`.

## Manual Dedicated Server Checklist

Use a Forge 1.20.1 dedicated server with Forge 47.4.10.

1. Place the built jar in the server `mods/` folder.
2. Start the server.
3. Confirm the server creates `config/craftledger/`.
4. Join from a client that does not have CraftLedger Jobs installed.
5. Confirm there is no rejected channel/version error.
6. Run `/balance`.
7. Run `/pay <player> <amount>` with two players.
8. Run `/sell hand` with a configured sellable item.
9. Run `/shop list` and `/shop buy minecraft:bread 1`.
10. Run `/job join miner`, break a configured ore, and confirm payout.
11. Run `/craftledger reload` as an operator.
12. Stop and restart the server, then confirm balances persisted.

## Pre-Release Standard

Do not publish a release jar until the automated checks pass and the manual dedicated-server checklist has been completed on a staging server.

