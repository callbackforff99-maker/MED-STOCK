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
Project foundation only. App code, production migrations, security policies, sync and backup flows are not implemented yet.
