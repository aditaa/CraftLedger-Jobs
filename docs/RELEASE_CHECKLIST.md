# Release Checklist

CraftLedger Jobs is not ready for a first public jar release yet. Use this checklist before publishing.

## Code

- `./gradlew build` passes.
- CI passes on `main`.
- No known balance duplication bugs.
- No known permission bypasses.
- Config reload behavior is tested.
- Data persistence survives server restart.

## Server Compatibility

- Dedicated Forge 1.20.1 / Forge 47.4.10 server boots.
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
- GitHub release notes are written.
- Release jar is attached to GitHub release.
- CurseForge description and project metadata are ready.
