# Known Limitations

CraftLedger Jobs is still pre-release.

- Minecraft `1.20.1` with Forge `47.4.10` is the only release-certified target right now.
- Other common Forge target profiles are scaffolded for compatibility work, but they still need compile fixes and dedicated-server staging before release. See `docs/MULTIVERSION.md`.
- There is no GUI. All player workflows are command-based.
- Shop and job behavior is config-driven; there is no in-game editor yet.
- There is no migration tooling for future config or data format changes yet.
- Transaction log lookup is limited to recent tail output.
- Daily job payout limits use UTC days.
- Manual staging-server validation is still required before a public jar release.
