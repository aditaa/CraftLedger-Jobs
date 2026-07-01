# Contributing

Thanks for helping make CraftLedger Jobs a trustworthy server-side Minecraft mod.

The project is pre-release. Stability, testing, and clear behavior matter more than adding features quickly.

## Before You Start

- Check open issues and pull requests to avoid duplicate work.
- For larger changes, open an issue first so the design can be discussed.
- Read `AGENTS.md` for project constraints, especially the server-only requirements.

## Local Setup

Requirements:

- Java 17 JDK
- Git
- Internet access for the first Gradle/Forge dependency download

Build:

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

## Pull Request Checklist

Before opening a pull request:

- The code builds locally with `./gradlew build`.
- New or changed commands are documented.
- New config keys are documented.
- Persistent data changes are backward-compatible or include a migration plan.
- Server-only compatibility has not been weakened.
- Transaction-affecting behavior writes a clear transaction log entry.

## Design Principles

- Server owners should be able to understand and audit the economy.
- Players should use normal commands, not `/trigger`.
- Defaults should be safe for testing but easy to tune.
- Economy data should survive crashes and backups.
- Permissioned admin commands should be explicit and logged.

## Commit Style

Use short, plain-English commit messages:

- `Add shop buy command`
- `Validate server-only mod metadata in CI`
- `Document staging test checklist`

## Reporting Bugs

Include:

- Minecraft version
- Forge version
- CraftLedger Jobs version or commit hash
- Server log excerpt
- Steps to reproduce
- Expected behavior
- Actual behavior

