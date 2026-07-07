# Testing

CraftLedger Jobs needs both automated checks and dedicated-server validation before release.

For volunteer player beta testing, see [Volunteer Beta Testing](Volunteer-Beta-Testing).

## Automated Checks

Maintainers should run the normal Gradle build before submitting release-related changes:

```bash
./gradlew build
```

CI also validates that the jar contains server-only metadata:

- `META-INF/mods.toml` exists.
- `displayTest="IGNORE_SERVER_VERSION"` is present.
- The mod id is `craftledger_jobs`.
- The Forge dependency metadata is marked `side="SERVER"`.

## Manual Dedicated Server Checklist

Use a Forge 1.20.1 dedicated server with Forge 47.4.10.

1. Place the built or release-candidate jar in the server `mods/` folder.
2. Start the server.
3. Confirm the server creates `config/craftledger/`.
4. Join from a client that does not have CraftLedger Jobs installed.
5. Confirm there is no rejected channel/version error.
6. Run `/balance`.
7. Run `/baltop`.
8. Run `/pay <player> <amount>` with two players.
9. Run `/sell hand 1` and `/sell all minecraft:cobblestone` with configured sellable items.
10. Run `/shop list`, `/shop sell`, `/shop price minecraft:bread`, and `/shop buy minecraft:bread 1`.
11. Run `/jobs`, `/job current`, `/job progress miner`, and `/job info miner`.
12. Run `/job join miner`, break a configured ore, and confirm payout.
13. Run `/job progress miner` again and confirm job XP increased.
14. Run `/craftledger reload` as an operator.
15. Run `/craftledger balance get <player>` as an operator.
16. Run `/craftledger balance top` and `/craftledger transactions tail 5` as an operator.
17. Run `/craftledger player info <player>`, `/craftledger job set <player> miner`, and `/craftledger job clear <player>` as an operator.
18. Run `/craftledger job level set <player> miner 5`, `/job progress miner`, then `/craftledger job level reset <player> miner`.
19. Run `/craftledger transactions tail player <player> 5`.
20. Temporarily set `payoutCooldownSeconds` in `jobs.json`, reload, and confirm repeated job actions do not spam payouts.
21. Place a configured payout block, break it, and confirm it does not produce a job payout when `trackPlacedBlocks` is enabled.
22. Stop and restart the server, then confirm balances and job progress persisted.

## SQLite Checklist

Run this on a disposable world before using SQLite on a real server:

1. Set `storageBackend = "sqlite"` in `config/craftledger/common.toml`.
2. Restart the server.
3. Confirm `world/craftledger/craftledger.sqlite` is created.
4. Run balance, pay, shop, sell, job join, job payout, and transaction tail commands.
5. Stop and restart the server, then confirm balances, job assignments, payout totals, and transactions persisted.
6. Confirm `placed_blocks.json` is still created and block-place anti-abuse still works.

Do not publish a public release jar until automated checks pass and the manual dedicated-server checklist has been completed on a staging server.
