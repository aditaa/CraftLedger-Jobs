# Volunteer Beta Testing Guide

Thank you for helping test CraftLedger Jobs. This guide is for volunteer players testing a beta release in a normal Minecraft client.

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

## What You Need

- Minecraft Java Edition.
- Forge for the same Minecraft version as the beta release.
- The CraftLedger Jobs beta jar for your Minecraft version.
- A GitHub account if you will report results directly.

CraftLedger Jobs is designed to be server-side for public servers, but local beta testing uses singleplayer because Minecraft runs an integrated server inside your client.

## Download the Beta Release

1. Open the CraftLedger Jobs releases page:

   https://github.com/aditaa/CraftLedger-Jobs/releases

2. Download the beta jar for your Minecraft version.
3. Do not download source code `.zip` or `.tar.gz` files for testing.
4. Keep the jar file name available for your report.

If there are multiple beta jars, choose the one that matches your Minecraft and Forge version. For example, a Minecraft `1.20.1` beta should be tested with a Forge `1.20.1` client.

## Install with the Vanilla Minecraft Launcher

Use this path if you launch Minecraft through the normal Minecraft Launcher.

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

Use this path if you manage Minecraft profiles with the CurseForge app.

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

Do not mix beta jars for one Minecraft version with a different Minecraft profile version.

## Start a Fresh Test World

1. Start Minecraft with the Forge profile that has CraftLedger Jobs installed.
2. Click Singleplayer.
3. Click Create New World.
4. Name the world something like `CraftLedger Beta Test`.
5. Turn Allow Cheats on. This lets you test operator commands.
6. Use Survival or Creative, depending on the test you are running.
7. Create the world.
8. After joining, run `/balance`.

The mod is probably installed and running if `/balance`, `/jobs`, and `/shop list` appear in autocomplete and return CraftLedger Jobs messages.

## Basic Test Rules

- Test one thing at a time when possible.
- Write down the exact command you ran.
- Write down what you expected to happen.
- Write down what actually happened.
- Take screenshots of useful chat output.
- Include your Minecraft version, Forge version, CraftLedger Jobs jar name, and test date in every report.

For screenshots, press `F2` in Minecraft. Screenshots are saved in the client `screenshots` folder.

## Quick Smoke Test

Run this first. If any step fails, report it.

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

Use default items unless the beta notes say the config changed.

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

### Miner

1. Join miner with `/job join miner`.
2. Record your balance with `/balance`.
3. Break naturally generated coal ore, copper ore, iron ore, or another ore listed by `/job info miner`.
4. Run `/balance` and `/job progress miner`.

Expected result: balance and job progress increase.

### Farmer

1. Join farmer with `/job join farmer`.
2. Harvest fully grown wheat, carrots, potatoes, beetroots, pumpkin, melon, sugar cane, cactus, or cocoa.
3. Run `/balance` and `/job progress farmer`.

Expected result: configured crop actions reward the farmer job.

### Woodcutter

1. Join woodcutter with `/job join woodcutter`.
2. Break a configured log, such as oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, crimson stem, or warped stem.
3. Run `/balance` and `/job progress woodcutter`.

Expected result: configured log actions reward the woodcutter job.

### Hunter

1. Join hunter with `/job join hunter`.
2. Kill a configured hostile mob, such as zombie, skeleton, creeper, spider, enderman, witch, slime, drowned, husk, stray, or pillager.
3. Run `/balance` and `/job progress hunter`.

Expected result: configured mob kills reward the hunter job.

### Placed Block Anti-Abuse

1. Join woodcutter with `/job join woodcutter`.
2. Place an oak log yourself.
3. Break the oak log you just placed.
4. Check whether you got a payout.

Expected result: player-placed blocks should not pay job rewards when placed-block tracking is enabled.

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

Do not run storage migration tests unless the beta notes specifically ask for it.

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

## Reporting Bugs

Open a GitHub bug report:

https://github.com/aditaa/CraftLedger-Jobs/issues/new?template=bug_report.yml

Include:

- Minecraft version.
- Forge version.
- CraftLedger Jobs beta jar name.
- Test date.
- Exact steps to reproduce.
- What you expected.
- What actually happened.
- Screenshots, if the issue is visible in chat, UI, inventory, or disconnect screens.
- Latest log if Minecraft produced an error.

Good bug reports are specific. For example:

```text
Jar: craftledger-jobs-0.1.0-beta.1-forge-1.20.1.jar
Command: /shop buy minecraft:bread 1
Expected: receive 1 bread and lose 2 coins
Actual: command said success, balance changed, but no bread appeared
Screenshot: attached
```

## Reporting Successful Tests

Successful tests are useful. They show that a beta worked in a real Minecraft client.

Open a test report:

https://github.com/aditaa/CraftLedger-Jobs/issues/new?template=test_report.yml

Include:

- CraftLedger Jobs beta jar name.
- Minecraft and Forge version.
- Test date.
- Tester name or Minecraft username.
- Which checklist sections you completed.
- Any commands that worked.
- Any job actions that paid correctly.
- Screenshots of successful command output, job payouts, shop buys/sells, and balance changes.
- Anything you could not test.

If everything worked, say that clearly. A clean successful test is important release evidence.

## Screenshot Ideas

Useful screenshots include:

- Mods screen showing CraftLedger Jobs installed.
- `/balance`.
- `/shop price minecraft:bread` and `/shop buy minecraft:bread 1`.
- `/sell hand 1` with the item visible in the hotbar.
- `/jobs` and `/job current`.
- A job payout message after mining, farming, chopping, or hunting.
- `/job progress <job>` before and after a payout.
- Any error, disconnect, or rejected command message.

Do not post screenshots that reveal private server addresses, private chat, or sensitive player information.
