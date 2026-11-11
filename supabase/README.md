# Supabase backend

This directory will contain versioned SQL migrations and backend configuration.

## Planned first migration
1. Owner profile and shop tables.
2. Owner-scoped medicine catalogue and shop inventory.
3. Batch and stock movement tables.
4. Purchase shortlist and backup log.
5. Indexes, constraints and RLS policies.
6. Transactional, idempotent stock movement function.

The schema and policies are not deployed. Review and test migrations in a development Supabase project before production use.

Never commit service-role keys, database passwords or OAuth secrets.
