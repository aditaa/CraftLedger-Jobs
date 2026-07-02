# Changelog

CraftLedger Jobs has not shipped a public release yet.

This changelog follows the spirit of Keep a Changelog once versioned releases begin.

## Unreleased

This section tracks work intended for the first public jar release. The release should not be published until the staging-server checklist is complete.

### Added

- Forge 1.20.1 project scaffold targeting Forge 47.4.10.
- Server-only mod metadata.
- Initial economy commands: `/balance`, `/money`, `/baltop`, `/pay`, and operator balance management.
- Configurable shop and sell commands: `/sell hand`, `/sell all`, `/shop list`, `/shop sell`, `/shop price`, and `/shop buy`.
- Configurable jobs system with miner, farmer, hunter, and woodcutter defaults.
- Job payout cooldowns and daily payout limits.
- Forge permission nodes for admin, balance lookup, balance leaderboard, and transaction-log access.
- Persistent player balances, job selections, job payout totals, and transaction logs.
- GitHub Actions build, unit test, repository validation, metadata validation, server-only source validation, CodeQL, dependency review, and Forge smoke-test workflows.
- Contributor, security, and project documentation.

### Changed

- Shop item command arguments now parse namespaced item ids such as `minecraft:bread`.
- Operator balance commands support both stored player names/UUIDs and online player selectors.
- Transaction log tailing reads from the end of the file instead of loading the entire log.

### Fixed

- Prevented item loss when a sale cannot safely credit the player's balance.
- Persisted daily job payout totals across restarts.
- Recorded players on login so offline admin lookup works after a normal join.

### Not Released Yet

- No public GitHub release jar has been published.
- No CurseForge project page has been published.
