# MEDI STOCK Architecture

## Product
MEDI STOCK will provide an Android app and a website for the same owner account. One owner can manage multiple shops. Staff accounts are out of scope for the first release.

## System overview

```text
Android (Compose + Room)       Website (React + TypeScript)
          |                               |
          +---------- Supabase Auth ------+
                         |
                  PostgreSQL + RLS
                         |
                 Owner-scoped cloud data

Android backup -> encrypted archive -> Google Drive
```

## Responsibilities
- Room: Android local cache and pending offline operations.
- Supabase Auth: account identity and authentication.
- PostgreSQL: shared cloud data and authoritative stock transactions.
- Google Drive: encrypted backup storage only, not the live database.

## Offline sync
1. Save a local change with a unique client operation ID.
2. Queue it while offline.
3. Submit pending operations when connectivity returns.
4. Validate ownership and apply stock changes atomically on the server.
5. Record operation IDs so retries cannot duplicate movements.
6. Refresh affected records and mark successful operations synced.

Conflicting stock changes must not be silently resolved by last-write-wins.

## Security
- Enable Row Level Security on every owner-scoped table.
- Derive owner identity from the authenticated session, never a client-supplied owner ID.
- Validate shop, inventory, medicine and batch relationships.
- Never ship service-role keys or OAuth secrets in client apps.
- Encrypt backup files before uploading to Drive.

## Stock rules
- Track quantities by batch and expiry date.
- Suggest FEFO (first-expiry, first-out) when issuing stock.
- Exclude expired stock from sellable quantity.
- Record IN, OUT, RETURN and ADJUSTMENT as movement history.
- Apply quantity changes and movement records in one transaction.
- Prevent negative stock except through an explicitly audited adjustment process.

## Delivery sequence
1. Review schema and business rules.
2. Configure Supabase Auth, schema, indexes and RLS.
3. Build Android foundation and Room persistence.
4. Build web authentication and shop workflows.
5. Implement transactional stock operations.
6. Add offline sync and conflict handling.
7. Add encrypted Drive backup and restore.
8. Test permissions, concurrency, expiry and recovery.
9. Build APK and deploy website.

This is the intended design, not a claim that these components are implemented.
