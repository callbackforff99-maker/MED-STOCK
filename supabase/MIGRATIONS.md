# Supabase migration notes

## Initial schema migration

File: `migrations/202611110001_initial_schema.sql`

The migration defines owner profiles, multiple shops, owner-scoped medicine catalogue, shop inventory, expiry-dated batches, stock movements, purchase shortlist and backup logs.

It also includes:
- An auth signup trigger that creates an owner profile.
- Owner-scoped Row Level Security policies.
- Composite foreign keys to prevent cross-owner shop/catalogue/batch relationships.
- A transaction-safe stock movement RPC with an idempotency key.
- Expired-batch rejection for stock OUT operations.

## Important review notes

- This migration is a starting point and must be tested in a disposable Supabase project before production.
- Run it as a Supabase database administrator so it can create the auth trigger and functions.
- The app should create a batch at quantity zero, then add stock through `apply_stock_movement`.
- For stock OUT, pass a negative `quantity_delta`; for IN and RETURN, pass a positive value.
- ADJUSTMENT accepts a signed non-zero delta.
- Expired stock remains recorded but cannot be issued through the OUT RPC.
- The app must not write directly to `stock_movements`; use the RPC.
- Review whether RETURNs for expired batches should be held separately from sellable quantity in the final business rules.
- Google Drive OAuth token storage and backup encryption/key recovery need a separate design before implementation.

## Apply later

No Supabase project has been connected or migrated by this commit. Apply only after reviewing the SQL in a development Supabase project.
