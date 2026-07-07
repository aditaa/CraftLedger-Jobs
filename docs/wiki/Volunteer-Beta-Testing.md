# Volunteer Beta Testing

This page is for volunteer players testing a beta release in a normal Minecraft client.

For beta testing, install the CraftLedger Jobs jar in your own Forge client, start a fresh singleplayer world, and run the tests below.

## Super Quick Version

Use this if you already use CurseForge and just want the shortest path.

1. Download the beta jar from the CraftLedger Jobs releases page.
2. Open CurseForge.
3. Create a custom Minecraft profile for the same Minecraft version as the beta jar.
4. Choose Forge as the modloader.
5. Open the profile folder.
6. Put the beta jar in the `mods` folder.
7. Launch the profile.
8. Click Singleplayer.
9. Create a new world with cheats turned on.
10. Run `/balance`.
11. Run `/jobs`.
12. Run `/shop list`.
13. Take screenshots with `F2`.
14. Report anything broken, or report that the test worked.

If you get stuck before step 10, send a screenshot of where you got stuck and include the beta jar name you downloaded.

## Download the Beta Release

1. Open the CraftLedger Jobs releases page:

   https://github.com/aditaa/CraftLedger-Jobs/releases

2. Download the beta jar for your Minecraft version.
3. Do not download source code `.zip` or `.tar.gz` files for testing.
4. Keep the jar file name available for your report.

## Install with the Vanilla Minecraft Launcher

1. Install Forge for the Minecraft version you are testing.
2. Open the Minecraft Launcher.
3. Select the Forge profile.
4. Click Play once, then close Minecraft after it reaches the main menu.
5. Open your Minecraft folder.
6. Open or create the `mods` folder.
7. Put the CraftLedger Jobs beta jar in the `mods` folder.
8. Start Minecraft with the Forge profile.
9. From the main menu, open Mods and confirm CraftLedger Jobs appears in the mod list.

Common Minecraft folder locations:

- Windows: `%APPDATA%\.minecraft`
- macOS: `~/Library/Application Support/minecraft`
- Linux: `~/.minecraft`

## Install with CurseForge

1. Open CurseForge.
2. Create a new custom Minecraft profile.
3. Select the Minecraft version that matches the beta jar.
4. Select Forge as the modloader.
5. Open the profile.
6. Click the profile menu, then choose Open Folder.
7. Open or create the `mods` folder.
8. Put the CraftLedger Jobs beta jar in the `mods` folder.
9. Launch the profile.
10. From the main menu, open Mods and confirm CraftLedger Jobs appears in the mod list.

## Start a Fresh Test World

1. Start Minecraft with the Forge profile that has CraftLedger Jobs installed.
2. Click Singleplayer.
3. Click Create New World.
4. Name the world something like `CraftLedger Beta Test`.
5. Turn Allow Cheats on.
6. Create the world.
7. After joining, run `/balance`.

The mod is probably installed and running if `/balance`, `/jobs`, and `/shop list` appear in autocomplete and return CraftLedger Jobs messages.

## Quick Smoke Test

1. Start a fresh singleplayer world.
2. Run `/balance`.
3. Run `/money`.
4. Run `/jobs`.
5. Run `/shop list`.
6. Run `/shop price minecraft:bread`.
7. Run `/job join miner`.
8. Run `/job current`.
9. Run `/job progress miner`.

Expected result: every command returns a clear CraftLedger Jobs message and no command crashes the world.

## Player Command Tests

### Balance

1. Run `/balance`.
2. Run `/money`.
3. Run `/baltop`.
4. Run `/baltop 1`.

Expected result: your balance is shown, and balance top commands return a list or clear message.

### Shop and Sell

1. Run `/shop list`.
2. Run `/shop sell`.
3. Run `/shop price minecraft:bread`.
4. Run `/shop buy minecraft:bread 1`.
5. Confirm bread appears in your inventory and your balance decreases.
6. Give yourself or collect a sellable item such as cobblestone, coal, wheat, or oak logs.
7. Hold the item and run `/sell hand 1`.
8. Confirm the item count decreases and your balance increases.
9. With several sellable items in your inventory, run `/sell all`.
10. If you have cobblestone, run `/sell all minecraft:cobblestone`.

Expected result: buy commands give the item and charge the configured price. Sell commands remove matching items and pay the configured price.

### Jobs

1. Run `/jobs`.
2. Run `/job current`.
3. Run `/job info miner`.
4. Run `/job info farmer`.
5. Run `/job info woodcutter`.
6. Run `/job info hunter`.
7. Run `/job join miner`.
8. Run `/job current`.
9. Run `/job progress`.
10. Run `/job progress miner`.
11. Run `/job leave`.
12. Run `/job current`.

Expected result: job list, info, join, progress, and leave commands return clear messages and keep the correct current job state.

## Gameplay Reward Tests

- Miner: join with `/job join miner`, break naturally generated ore, then check `/balance` and `/job progress miner`.
- Farmer: join with `/job join farmer`, harvest fully grown crops, then check `/balance` and `/job progress farmer`.
- Woodcutter: join with `/job join woodcutter`, break configured logs, then check `/balance` and `/job progress woodcutter`.
- Hunter: join with `/job join hunter`, kill configured hostile mobs, then check `/balance` and `/job progress hunter`.

Expected result: configured job actions increase balance and job progress.

## Operator Command Tests

These work in a singleplayer world with cheats enabled.

1. `/craftledger reload`
2. `/craftledger shop reload`
3. `/craftledger jobs reload`
4. `/craftledger balance top`
5. `/craftledger balance get <your-player-name>`
6. `/craftledger balance set <your-player-name> 100`
7. `/craftledger balance add <your-player-name> 10`
8. `/craftledger balance take <your-player-name> 5`
9. `/craftledger player info <your-player-name>`
10. `/craftledger job set <your-player-name> miner`
11. `/craftledger job clear <your-player-name>`
12. `/craftledger job level set <your-player-name> miner 5`
13. `/craftledger job level reset <your-player-name> miner`
14. `/craftledger transactions tail 5`
15. `/craftledger transactions tail player <your-player-name> 5`

Expected result: valid admin commands succeed, invalid values are rejected, and balance/job changes appear in player-facing commands afterward.

## Optional Config Tests

Only do this if you are comfortable editing config files.

1. Close Minecraft.
2. Open your Minecraft folder.
3. Look for the CraftLedger config files under `config/craftledger`.
4. Back up the file you are editing.
5. Change one setting.
6. Start the world again.
7. Run `/craftledger reload` when the setting supports reload.
8. Repeat the related command or gameplay test.

Useful settings to test:

- `startingBalance`
- `maxBalance`
- `maxPayAmount`
- `payCooldownSeconds`
- `currencyEnabled`
- shop buy and sell prices
- job payout amounts
- `payoutCooldownSeconds`

Expected result: valid config changes affect gameplay, invalid config changes produce a clear error, and the world does not crash.

## Reporting Results

Report bugs here:

https://github.com/aditaa/CraftLedger-Jobs/issues/new?template=bug_report.yml

Report successful or partially successful tests here:

https://github.com/aditaa/CraftLedger-Jobs/issues/new?template=test_report.yml

Include the beta jar name, Minecraft version, Forge version, test date, what you tested, screenshots, and anything you could not test.
