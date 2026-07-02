# Release Checklist

CraftLedger Jobs is not ready for a first public jar release yet. Use this checklist before publishing.

## Code

- `./gradlew build` passes.
- For each non-primary Minecraft target jar, `./gradlew build -Pcraftledger_mc_target=<profile>` passes.
- The `Release Build` GitHub Actions workflow passes for every target jar intended for release.
- CI passes on `main`.
- No known balance duplication bugs.
- No known permission bypasses.
- Config reload behavior is tested.
- Data persistence survives server restart.

## Server Compatibility

- Dedicated Forge 1.20.1 / Forge 47.4.10 server boots.
- Each non-primary target jar has equivalent dedicated-server staging evidence before it is published.
- Client without CraftLedger Jobs can join.
- No rejected mod channel/version error appears.
- No client-side install is required.

## Documentation

- README is current.
- Commands are documented.
- Config examples are documented.
- Installation/admin docs are documented.
- GitHub Wiki pages are published or staged from `docs/wiki/`.
- Repository social preview is set from `.github/assets/social-preview.png`.
- Changelog entry is written.
- Known limitations are documented.

## Release

- Version number is updated.
- A `v*` tag release build has produced reviewed workflow artifacts.
- GitHub release notes are written.
- Release jar is attached to GitHub release.
- CurseForge description and project metadata are ready.
