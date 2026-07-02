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
6. Run `/balance` and the rest of the manual test checklist.

## Server Data

Back up `world/craftledger/` with the world. It contains player balances, job data, payout totals, and transaction logs.
