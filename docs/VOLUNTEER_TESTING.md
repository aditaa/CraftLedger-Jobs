# Volunteer Testing Guide

Thank you for helping test CraftLedger Jobs. This guide is written for players who can join a test server or run a local singleplayer test world and try the mod in normal gameplay.

CraftLedger Jobs is a server-side mod. On public servers, players should be able to join without installing CraftLedger Jobs locally. For volunteer testing, there are two useful setups:

- Hosted server testing: the maintainer installs CraftLedger Jobs on a Forge server, and you join without the mod installed locally.
- Local singleplayer testing: you install CraftLedger Jobs in your Forge client and test in a singleplayer world. This tests the mod running on Minecraft's integrated server, but it does not prove that an unmodded client can join a dedicated server.

## What You Need

- Minecraft Java Edition `1.20.1`.
- A Forge `1.20.1` client, preferably Forge `47.4.10`.
- The test server address from the maintainer, if you are doing hosted server testing.
- The CraftLedger Jobs jar from the maintainer, if you are doing local singleplayer testing.
- The CraftLedger Jobs build/version or commit being tested.
- A GitHub account if you will report results directly.

Do not install the CraftLedger Jobs jar in your local client mods folder when you are testing clientless server join behavior. For local singleplayer testing, installing the jar in the client `mods` folder is expected because the same Minecraft process runs the integrated server.

## Install the Test Client Only

1. Open the Minecraft Launcher.
2. Install or select a Minecraft `1.20.1` profile.
3. Install Forge for Minecraft `1.20.1`.
4. Use Forge `47.4.10` when possible.
5. Launch the Forge profile once so it finishes setup.
6. Join the test server from Multiplayer.

Use this path when a maintainer gives you a server address. Leave CraftLedger Jobs out of your local client unless the test instructions say otherwise.

## Install the Mod for Local Singleplayer Testing

Use this path when you do not have a server and a maintainer gives you a CraftLedger Jobs jar to test.

1. Install Minecraft Java Edition `1.20.1`.
2. Install Forge `1.20.1`, preferably Forge `47.4.10`.
3. Launch the Forge profile once, then close the game.
4. Open your Minecraft folder.
5. Open or create the `mods` folder.
6. Put the CraftLedger Jobs jar in `mods`.
7. Start Minecraft with the Forge profile.
8. Create a new singleplayer world for testing.
9. Turn cheats on if you need to test operator commands.

This setup is good for command, shop, sell, job payout, and config testing. It is not a replacement for final dedicated server testing.

## Confirm the Mod Is Running

After joining the hosted server or opening your local test world:

1. Run `/balance`.
2. Run `/jobs`.
3. Run `/shop list`.

The mod is probably installed and running if those commands appear in autocomplete and return CraftLedger Jobs messages.

Report a problem if:

- The server rejects you during join.
- You see a mod channel or version mismatch error.
- `/balance`, `/jobs`, or `/shop list` is unknown.
- A command appears in autocomplete but fails with an error.

Take a screenshot of the error screen or chat message when possible.

## Basic Test Rules

- Test one thing at a time when possible.
- Write down the exact command you ran.
- Write down what you expected to happen.
- Write down what actually happened.
- Screenshot chat output for important command results.
- Include the Minecraft, Forge, and CraftLedger Jobs versions in every report.

For screenshots, press `F2` in Minecraft. The game saves screenshots in the client `screenshots` folder.

## Player Command Tests

Run these as a normal non-operator player unless a maintainer says otherwise.

### Balance and Pay

1. Run `/balance`.
2. Run `/money`.
3. Run `/baltop`.
4. Run `/baltop 1`.
5. With another tester online, run `/pay <player> 1`.
6. Run `/balance` again and confirm your balance changed.
7. Ask the other tester to run `/balance` and confirm their balance changed.
8. Try an invalid payment such as `/pay <player> -1` and confirm it is rejected.
9. Try paying more than your balance and confirm it is rejected.

Expected result: valid payments move money from one player to another. Invalid payments do not change balances.

### Shop and Sell

Use default items unless the maintainer says the server config changed.

1. Run `/shop list`.
2. Run `/shop sell`.
3. Run `/shop price minecraft:bread`.
4. Run `/shop buy minecraft:bread 1`.
5. Confirm bread appears in your inventory and your balance decreases.
6. Collect or receive one sellable item, such as `minecraft:cobblestone`, `minecraft:coal`, `minecraft:wheat`, or `minecraft:oak_log`.
7. Hold the item and run `/sell hand 1`.
8. Confirm the item count decreases and your balance increases.
9. With several sellable items in your inventory, run `/sell all`.
10. If you have a specific item, run `/sell all minecraft:cobblestone`.

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
11. Break a naturally generated configured block, such as coal ore, iron ore, or copper ore.
12. Confirm you receive a job payout or job progress.
13. Run `/job progress miner` again and confirm XP or progress changed.
14. Run `/job leave`.
15. Run `/job current` and confirm you no longer have an active job.

Expected result: joining a job sets your current job, configured actions pay rewards, and progress updates after successful payouts.

## Job Functionality Tests

Try these if the maintainer has time to set up test areas.

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

This test confirms that players cannot place and break blocks forever for job money.

1. Join miner or woodcutter.
2. Place a configured payout block yourself, such as an oak log.
3. Break the block you just placed.
4. Check whether you got a payout.

Expected result when `trackPlacedBlocks = true`: player-placed blocks do not pay job rewards.

## Optional Config Tests

These tests require a maintainer or server operator to edit server config files and run `/craftledger reload`, or restart when required.

### Economy Limits

Ask the maintainer to test one setting at a time in `config/craftledger/common.toml`:

- `startingBalance`
- `maxBalance`
- `maxPayAmount`
- `payCooldownSeconds`
- `currencyEnabled`

Expected result: limits are enforced, cooldowns block repeated pay attempts, and disabled currency features report clear messages.

### Shop Prices

Ask the maintainer to edit `config/craftledger/shop.json`:

1. Change a buy price for an existing item.
2. Run `/craftledger shop reload` or `/craftledger reload`.
3. Run `/shop price <item>`.
4. Buy the item and confirm the new price is used.

Expected result: valid shop changes reload without restart and commands use the new values.

### Job Payouts and Cooldowns

Ask the maintainer to edit `config/craftledger/jobs.json`:

1. Change a payout amount for a job action.
2. Set `payoutCooldownSeconds` to a small value, such as `10`.
3. Run `/craftledger jobs reload` or `/craftledger reload`.
4. Trigger the same job action twice quickly.

Expected result: the first action pays, repeated actions inside the cooldown do not spam payouts, and actions work again after the cooldown.

### Storage Backend

SQLite testing should happen only on a disposable test world.

1. Ask the maintainer to set `storageBackend = "sqlite"` in `common.toml`.
2. Restart the server.
3. Run balance, pay, shop, sell, and job tests.
4. Restart the server again.
5. Confirm balances and job progress persisted.

Expected result: gameplay works the same after switching to SQLite, and data persists after restart.

## Operator Command Tests

Only run these if the maintainer gives you operator access on a test server.

1. `/craftledger reload`
2. `/craftledger shop reload`
3. `/craftledger jobs reload`
4. `/craftledger balance top`
5. `/craftledger balance get <player>`
6. `/craftledger balance set <player> 100`
7. `/craftledger balance add <player> 10`
8. `/craftledger balance take <player> 5`
9. `/craftledger player info <player>`
10. `/craftledger job set <player> miner`
11. `/craftledger job clear <player>`
12. `/craftledger job level set <player> miner 5`
13. `/craftledger job level reset <player> miner`
14. `/craftledger transactions tail 5`
15. `/craftledger transactions tail player <player> 5`

Expected result: valid admin commands succeed, invalid values are rejected, and balance/job changes appear in player-facing commands afterward.

Only run `/craftledger storage migrate json-to-sqlite` when the maintainer explicitly asks for migration testing on a disposable world.

## Reporting Bugs

Open a GitHub bug report:

https://github.com/aditaa/CraftLedger-Jobs/issues/new?template=bug_report.yml

Include:

- Minecraft version.
- Forge version.
- CraftLedger Jobs version or commit.
- Server name or test session date.
- Exact steps to reproduce.
- What you expected.
- What actually happened.
- Screenshots, if the issue is visible in chat, UI, inventory, or disconnect screens.
- Relevant server log excerpt, if a maintainer provides one.

Good bug reports are specific. For example:

```text
Build: 0.1.0-test3
Command: /shop buy minecraft:bread 1
Expected: receive 1 bread and lose 2 coins
Actual: command said success, balance changed, but no bread appeared
Screenshot: attached
```

## Reporting Successful Tests

Successful tests are useful. They show that a build worked on a real server with real players.

Open a test report:

https://github.com/aditaa/CraftLedger-Jobs/issues/new?template=test_report.yml

Include:

- CraftLedger Jobs version or commit.
- Test date.
- Tester name or Minecraft username.
- Server name or test world.
- Which checklist sections you completed.
- Any commands that worked.
- Any job actions that paid correctly.
- Screenshots of successful command output, job payouts, shop buys/sells, and balance changes.
- Anything you could not test.

If everything worked, say that clearly. A clean successful test is still important release evidence.

## Screenshot Ideas

Useful screenshots include:

- `/balance` before and after `/pay`.
- `/shop price minecraft:bread` and `/shop buy minecraft:bread 1`.
- `/sell hand 1` with the item visible in the hotbar.
- `/jobs` and `/job current`.
- A job payout message after mining, farming, chopping, or hunting.
- `/job progress <job>` before and after a payout.
- Any error, disconnect, or rejected command message.

Do not post screenshots that reveal private server IPs, passwords, private chat, or sensitive player information unless the maintainer says it is okay.
