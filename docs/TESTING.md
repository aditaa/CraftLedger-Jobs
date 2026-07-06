# Testing

CraftLedger Jobs needs both automated checks and dedicated-server validation.

## Automated Checks

Run:

```bash
./gradlew build
```

For a scaffolded Minecraft target profile:

```bash
./gradlew build -Pcraftledger_mc_target=1.19.2
```

Run only unit tests:

```bash
./gradlew test
```

CI also validates that the built jar contains server-only metadata:

- `META-INF/mods.toml` exists.
- `displayTest="IGNORE_SERVER_VERSION"` is present.
- The mod id is `craftledger_jobs`.
- The Forge dependency metadata is marked `side="SERVER"`.

CI is split into these layers:

- Gradle wrapper validation.
- Unit tests.
- Repository verification tasks.
- Server-only source validation, which blocks known client-only Minecraft/Forge imports.
- Full Gradle build and reobfuscated jar creation.
- Forge development server smoke test.
- Dependency review for PR dependency changes.
- CodeQL static analysis for Java.

Unit tests include JSON storage behavior, SQLite storage behavior, configurable message fallback/substitution, transaction tail filtering, and placed-block anti-abuse persistence.

Run the same verification task set locally:

```bash
./gradlew check
```

Run the CI-style server smoke test on Linux or WSL:

```bash
bash scripts/ci/smoke-server.sh
```

The smoke test passes when the Forge development server reaches the Minecraft `Done (...)!` line and logs `CraftLedger Jobs loaded`.

## Manual Dedicated Server Checklist

Use a Forge 1.20.1 dedicated server with Forge 47.4.10.

For non-primary target profiles, use the exact Minecraft and Forge versions listed in `docs/MULTIVERSION.md`.

1. Place the built jar in the server `mods/` folder.
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

For migration testing from JSON storage:

1. Start with `storageBackend = "json"` and create balances, job assignments, payout totals, and transactions.
2. Run `/craftledger storage migrate json-to-sqlite dry-run`.
3. Run `/craftledger storage migrate json-to-sqlite`.
4. Confirm a `migration-backup-*` directory exists under `world/craftledger/`.
5. Set `storageBackend = "sqlite"` and restart.
6. Confirm migrated balances, job assignments, payout totals, and transaction tail output are present.

## Pre-Release Standard

Do not publish a release jar until the automated checks pass and the manual dedicated-server checklist has been completed on a staging server.
