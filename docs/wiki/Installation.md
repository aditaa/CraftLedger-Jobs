# Installation

## Requirements

- Minecraft `1.20.1`
- Forge `47.4.10`
- Dedicated Forge server

## Steps

1. Build the mod with `./gradlew build`.
2. Copy the reobfuscated jar from `build/libs/` to the server `mods/` folder.
3. Start the server.
4. Confirm `config/craftledger/` is created.
5. Join from a client without CraftLedger Jobs installed.
6. Run `/balance` and the rest of the [manual test checklist](Testing).

Volunteer beta testers using a normal Minecraft client should follow [Volunteer Beta Testing](Volunteer-Beta-Testing) instead.

## Server Data

Back up `world/craftledger/` with the world. It contains player balances, job data, placed-block anti-abuse data, payout totals, and transaction logs.

For larger servers, set `storageBackend = "sqlite"` in `config/craftledger/common.toml`, restart the server, and confirm `world/craftledger/craftledger.sqlite` is created. Test this on a disposable world before using it on production data.

For existing JSON data, run `/craftledger storage migrate json-to-sqlite dry-run`, then `/craftledger storage migrate json-to-sqlite`, then switch `storageBackend` and restart.
