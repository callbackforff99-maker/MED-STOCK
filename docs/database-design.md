# MEDI STOCK Database Design

This is an initial logical model. Convert it into reviewed SQL migrations and tested RLS policies before using real shop data.

## Core entities

### owner_profiles
- `id`: UUID matching `auth.users.id`
- `full_name`, `phone`, `created_at`

Authentication email and password remain managed by Supabase Auth.

### shops
- `id`, `owner_id`, `shop_name`, `address`, `phone`, `is_active`, `created_at`
- An owner can manage multiple shops.

### medicine_catalog
- `id`, `owner_id`, `name`, `generic_name`, `company`, `strength`, `category`, `barcode`

### shop_inventory
- `id`, `shop_id`, `medicine_id`, `reorder_level`, `is_active`
- Enforce one row per shop and medicine.

### stock_batches
- `id`, `inventory_id`, `batch_number`, `expiry_date`, `quantity`, `purchase_price`, `selling_price`
- Use fixed-precision decimal types for money, never floating point.

### stock_movements
- `id`, `inventory_id`, `batch_id`, `movement_type`, `quantity`, `reason`, `client_operation_id`, `created_at`
- Types: IN, OUT, RETURN, ADJUSTMENT.
- Keep movement history append-only for ordinary workflows; correct errors with reversing or adjustment movements.

### purchase_shortlist
- `id`, `shop_id`, `medicine_id`, `required_quantity`, `status`, `notes`, `created_at`
- Status: PENDING, ORDERED, COMPLETED.

### backup_logs
- `id`, `owner_id`, `drive_file_id`, `backup_date`, `status`
- OAuth refresh tokens need a dedicated secure token strategy; do not store them as ordinary app data.

## Constraints and indexes to implement
- Foreign keys for all relationships.
- Unique `(shop_id, medicine_id)` in shop_inventory.
- Unique client operation ID for idempotent sync.
- Index owner/shop foreign keys and inventory/expiry lookup columns.
- Validate positive movement quantities and non-negative batch quantities.
- Validate that inventory, batch and medicine relationships belong to the same owner/shop.
- Use a database transaction or secured RPC for every stock movement.

## Row Level Security
Enable RLS on every owner-scoped table. Policies must derive the owner from `auth.uid()` and verify ownership through related shop/inventory records. Never trust a client-provided owner ID.

Exact SQL policies and transactional stock RPC are intentionally deferred until migration tests are prepared.
