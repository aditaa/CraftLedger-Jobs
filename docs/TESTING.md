# Testing

CraftLedger Jobs needs both automated checks and dedicated-server validation.

## Automated Checks

Run:

```bash
./gradlew build
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
11. Run `/jobs`, `/job current`, and `/job info miner`.
12. Run `/job join miner`, break a configured ore, and confirm payout.
13. Run `/craftledger reload` as an operator.
14. Run `/craftledger balance get <player>` as an operator.
15. Stop and restart the server, then confirm balances persisted.

## Pre-Release Standard

Do not publish a release jar until the automated checks pass and the manual dedicated-server checklist has been completed on a staging server.
