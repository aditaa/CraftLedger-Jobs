# Multi-Version Release Scaffolding

CraftLedger Jobs is still developed and validated first against Minecraft `1.20.1` and Forge `47.4.10`.

The repository also contains scaffolded Forge target profiles for common server versions so compatibility work can happen one lane at a time without changing the default build.

## Current Target Profiles

Profiles live under `gradle/minecraft-targets/`.

| Profile | Minecraft | Forge | Java toolchain | Status |
| --- | --- | --- | --- | --- |
| `1.18.2` | `1.18.2` | `40.3.12` | `17` | Scaffolded, not release-certified |
| `1.19.2` | `1.19.2` | `43.5.2` | `17` | Scaffolded, not release-certified |
| `1.19.4` | `1.19.4` | `45.4.0` | `17` | Scaffolded, not release-certified |
| `1.20.1` | `1.20.1` | `47.4.10` | `17` | Primary supported target |
| `1.20.4` | `1.20.4` | `49.2.7` | `17` | Scaffolded, not release-certified |
| `1.20.6` | `1.20.6` | `50.2.8` | `21` | Scaffolded, not release-certified |
| `1.21.1` | `1.21.1` | `52.1.14` | `21` | Scaffolded, not release-certified |

## Commands

List known target profiles:

```bash
./gradlew listMinecraftTargets
```

Build the default supported target:

```bash
./gradlew build
```

Build a named target profile:

```bash
./gradlew build -Pcraftledger_mc_target=1.19.2
```

On Windows:

```powershell
.\gradlew.bat build "-Pcraftledger_mc_target=1.19.2"
```

Quote the target property in PowerShell so the dotted Minecraft version is passed to Gradle unchanged.

Named target builds include the Minecraft and Forge versions in the jar base name, for example:

```text
craftledger_jobs-mc1.19.2-forge43.5.2-0.1.0.jar
```

## Version-Specific Code

Common code stays in `src/main/java` and `src/main/resources`.

When a Minecraft or Forge API difference needs a compatibility shim, put version-specific source or resources under:

```text
src/minecraft/<profile>/java
src/minecraft/<profile>/resources
```

Those directories are only added to the build when `-Pcraftledger_mc_target=<profile>` is provided.

Avoid duplicating gameplay logic across target folders. Prefer small adapters around Minecraft or Forge API differences, keeping economy, storage, command behavior, and transaction logging shared whenever possible.

The selected toolchain can vary by Minecraft target, but compiled mod bytecode still uses Java 17 unless the project deliberately changes that release policy.

## Release Certification

A target profile is not release-ready just because it compiles. Before publishing a jar for a target:

- `./gradlew build -Pcraftledger_mc_target=<profile>` passes.
- The `Release Build` GitHub Actions workflow passes for the target.
- The jar contains `META-INF/mods.toml`.
- The expanded `mods.toml` keeps `displayTest="IGNORE_SERVER_VERSION"`.
- The Forge and Minecraft dependencies keep `side="SERVER"`.
- A dedicated server for that exact Minecraft and Forge version boots with the jar.
- A client without CraftLedger Jobs can join that server.
- Balance, pay, shop, sell, reload, and jobs commands work server-side.
- The target is marked release-certified in this document.

Do not publish a target jar until the manual staging evidence exists for that target.

## Release Build Workflow

The `Release Build` workflow runs when a `v*` tag is pushed, and it can also be started manually from GitHub Actions.

It builds every scaffolded target profile, validates each jar's server-only metadata, runs target-specific Forge dev-server smoke tests after the build matrix is green, and uploads per-target workflow artifacts for maintainer review. It does not create a GitHub release, upload to CurseForge, or otherwise publish jars.

The uploaded artifacts include:

- the built target jar
- the expanded `mods.toml`
- the expanded `pack.mcmeta`

If one target fails, the workflow still tries the rest of the matrix so maintainers can see which version lanes are ready and which need compatibility work.

## CurseForge Publishing

The `CurseForge Publish` workflow is manual only. It builds the selected target profile or every profile, runs the Forge dev-server smoke test, uploads the resulting jar to CurseForge, and attaches the same jar to the matching GitHub Release.

Required GitHub configuration:

- Repository variable `CURSEFORGE_PROJECT_ID`: the numeric CurseForge project ID.
- Repository secret `CURSEFORGE_TOKEN`: a CurseForge API token for the project.
- GitHub Actions environment `curseforge`: recommended with required reviewer approval.

The workflow requires a `confirm` input of `publish` before any upload job can run. Do not run it until the target profile is release-certified and manual staging has passed.

GitHub Release tags are created as `v<version>`, for example `v0.1.0-beta.1`. Beta and alpha uploads are marked as GitHub prereleases. The GitHub Release title is version-wide, while CurseForge file names include the Minecraft and Forge target.

CurseForge files are uploaded one Minecraft/Forge target at a time with:

- loader: `forge`
- game version: the selected Minecraft profile
- Java version: the target profile Java toolchain
- release type: selected at workflow dispatch
