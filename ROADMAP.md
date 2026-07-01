# Roadmap

This roadmap describes the path to a first trusted public release. It is not a promise of dates.

## Phase 1: Project Foundation

- Forge 1.20.1 project boots.
- Server-only metadata is validated.
- CI builds every pull request.
- Contributor docs, issue templates, and security policy exist.

## Phase 2: Economy MVP

- Balances are persisted by UUID.
- `/balance`, `/money`, and `/pay` work.
- Admin balance commands work and are logged.
- Data writes are backup-safe.

## Phase 3: Shop and Sell

- Configured sell prices work for hand and inventory.
- Configured buy prices work through `/shop`.
- Config validation gives useful errors.
- Edge cases around full inventories and invalid item ids are tested.

## Phase 4: Jobs

- Miner, farmer, hunter, and woodcutter jobs work.
- Payouts are configurable.
- Abuse-prone actions are reviewed.
- Job payout spam is controlled.

## Phase 5: Test Server Validation

- Dedicated Forge 1.20.1 server boots with the jar.
- Client without the mod can join.
- No rejected channel or version error appears.
- Commands work in a real multiplayer environment.

## Phase 6: First Public Release

- Release checklist complete.
- Changelog written.
- GitHub release created.
- CurseForge project prepared.
- Documentation includes installation, config examples, and upgrade notes.

