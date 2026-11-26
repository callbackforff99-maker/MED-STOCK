# MEDI STOCK

Medicine inventory management for independent medicine shops.

Planned Android and web apps with owner accounts, multiple shops, batch-level inventory, stock IN/OUT, expiry tracking, purchase shortlist, offline sync and encrypted Google Drive backups.

## Planned stack
- Android: Kotlin, Jetpack Compose, Room
- Website: React + TypeScript
- Auth and cloud database: Supabase Auth + PostgreSQL
- Repository and CI: GitHub + GitHub Actions
- Backups: encrypted archives stored in Google Drive

## Repository layout
- `android/` — Android application
- `website/` — web application
- `supabase/` — SQL migrations, policies and backend configuration
- `docs/` — architecture and database design

## Security
- Owner-scoped data with PostgreSQL Row Level Security.
- Passwords managed by Supabase Auth; never stored in plaintext by the app.
- Stock movements applied atomically and retried idempotently.
- Google Drive stores encrypted backups, not the live database.

## Status
STOCK X Android v2.0 implementation is in progress on `release/v2-zerox`. The v2 branch contains the Obsidian Emerald UI, Room v4 migration for box quantities and a date-based purchase shortlist. Cloud authentication, live sync and encrypted Google Drive backup remain separate pending work.
