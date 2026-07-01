# Security Policy

CraftLedger Jobs handles player balances and admin-controlled economy actions, so security reports are welcome even before the first public release.

## Supported Versions

The project is currently pre-release. Security fixes will target the `main` branch until versioned releases exist.

## What To Report

Please report issues such as:

- Balance duplication or loss bugs
- Permission bypasses
- Command abuse that lets non-operators change balances or configs
- Data corruption risks
- Path traversal or unsafe file writes
- Server crashes caused by normal player actions
- Clientless-join regressions caused by networking or metadata changes

## How To Report

Use a private channel if available on GitHub, or open a GitHub issue with sensitive exploit details minimized. Include enough information for maintainers to reproduce the problem.

## Project Safety Rules

- Do not test exploits on public servers without permission.
- Do not include real player data in reports.
- Do not publish working abuse steps before maintainers have had time to fix the issue.

## Automated Security Checks

Pull requests run dependency review and CodeQL Java analysis. These checks are meant to catch vulnerable dependency changes and common code-level security issues before merge, but they do not replace manual review for economy logic, permission boundaries, or server-only compatibility.
