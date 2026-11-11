-- MEDI STOCK: initial schema
-- Review and test in a non-production Supabase project before deployment.

create extension if not exists pgcrypto;

-- Owner profile is created from the authenticated Supabase user.
create table if not exists public.owner_profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null default '',
  phone text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.shops (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.owner_profiles(id) on delete cascade,
  shop_name text not null check (length(trim(shop_name)) between 1 and 160),
  address text,
  phone text,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, owner_id)
);

create table if not exists public.medicine_catalog (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.owner_profiles(id) on delete cascade,
  name text not null check (length(trim(name)) between 1 and 200),
  generic_name text,
  company text,
  strength text,
  category text,
  barcode text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, owner_id)
);

create index if not exists medicine_catalog_owner_name_idx
  on public.medicine_catalog (owner_id, lower(name));
create index if not exists medicine_catalog_owner_barcode_idx
  on public.medicine_catalog (owner_id, barcode)
  where barcode is not null;

create table if not exists public.shop_inventory (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.owner_profiles(id) on delete cascade,
  shop_id uuid not null,
  medicine_id uuid not null,
  reorder_level integer not null default 0 check (reorder_level >= 0),
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (shop_id, medicine_id),
  unique (id, owner_id, shop_id),
  foreign key (shop_id, owner_id)
    references public.shops(id, owner_id) on delete cascade,
  foreign key (medicine_id, owner_id)
    references public.medicine_catalog(id, owner_id) on delete restrict
);

create index if not exists shop_inventory_owner_shop_idx
  on public.shop_inventory (owner_id, shop_id);

create table if not exists public.stock_batches (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null,
  shop_id uuid not null,
  inventory_id uuid not null,
  batch_number text not null check (length(trim(batch_number)) between 1 and 100),
  expiry_date date not null,
  quantity integer not null default 0 check (quantity >= 0),
  purchase_price numeric(12,2) not null default 0 check (purchase_price >= 0),
  selling_price numeric(12,2) not null default 0 check (selling_price >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, owner_id, shop_id, inventory_id),
  unique (inventory_id, batch_number, expiry_date),
  foreign key (inventory_id, owner_id, shop_id)
    references public.shop_inventory(id, owner_id, shop_id) on delete cascade
);

create index if not exists stock_batches_inventory_expiry_idx
  on public.stock_batches (inventory_id, expiry_date);

create table if not exists public.stock_movements (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null,
  shop_id uuid not null,
  inventory_id uuid not null,
  batch_id uuid not null,
  movement_type text not null
    check (movement_type in ('IN', 'OUT', 'RETURN', 'ADJUSTMENT')),
  quantity_delta integer not null check (quantity_delta <> 0),
  reason text,
  client_operation_id uuid not null,
  created_at timestamptz not null default now(),
  unique (owner_id, client_operation_id),
  foreign key (inventory_id, owner_id, shop_id)
    references public.shop_inventory(id, owner_id, shop_id) on delete restrict,
  foreign key (batch_id, owner_id, shop_id, inventory_id)
    references public.stock_batches(id, owner_id, shop_id, inventory_id) on delete restrict,
  check (
    (movement_type in ('IN', 'RETURN') and quantity_delta > 0)
    or (movement_type = 'OUT' and quantity_delta < 0)
    or movement_type = 'ADJUSTMENT'
  )
);

create index if not exists stock_movements_inventory_created_idx
  on public.stock_movements (inventory_id, created_at desc);
create index if not exists stock_movements_owner_created_idx
  on public.stock_movements (owner_id, created_at desc);

create table if not exists public.purchase_shortlist (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null,
  shop_id uuid not null,
  medicine_id uuid not null,
  required_quantity integer not null default 1 check (required_quantity > 0),
  status text not null default 'PENDING'
    check (status in ('PENDING', 'ORDERED', 'COMPLETED')),
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  foreign key (shop_id, owner_id)
    references public.shops(id, owner_id) on delete cascade,
  foreign key (medicine_id, owner_id)
    references public.medicine_catalog(id, owner_id) on delete restrict
);

create index if not exists purchase_shortlist_shop_status_idx
  on public.purchase_shortlist (shop_id, status, created_at desc);

create table if not exists public.backup_logs (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.owner_profiles(id) on delete cascade,
  drive_file_id text,
  backup_date timestamptz not null default now(),
  status text not null check (status in ('STARTED', 'COMPLETED', 'FAILED')),
  details text
);

create index if not exists backup_logs_owner_date_idx
  on public.backup_logs (owner_id, backup_date desc);

-- Automatically create the owner profile after Supabase Auth signup.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.owner_profiles (id, full_name)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', '')
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- Update timestamps consistently.
create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger owner_profiles_set_updated_at before update on public.owner_profiles
  for each row execute procedure public.set_updated_at();
create trigger shops_set_updated_at before update on public.shops
  for each row execute procedure public.set_updated_at();
create trigger medicine_catalog_set_updated_at before update on public.medicine_catalog
  for each row execute procedure public.set_updated_at();
create trigger shop_inventory_set_updated_at before update on public.shop_inventory
  for each row execute procedure public.set_updated_at();
create trigger stock_batches_set_updated_at before update on public.stock_batches
  for each row execute procedure public.set_updated_at();
create trigger purchase_shortlist_set_updated_at before update on public.purchase_shortlist
  for each row execute procedure public.set_updated_at();

-- Stock is changed only through this transaction-safe, idempotent RPC.
-- quantity_delta is positive for IN/RETURN, negative for OUT, and signed for ADJUSTMENT.
create or replace function public.apply_stock_movement(
  p_inventory_id uuid,
  p_batch_id uuid,
  p_movement_type text,
  p_quantity_delta integer,
  p_client_operation_id uuid,
  p_reason text default null
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_owner_id uuid := auth.uid();
  v_shop_id uuid;
  v_expiry_date date;
  v_movement_id uuid;
  v_existing public.stock_movements%rowtype;
begin
  if v_owner_id is null then
    raise exception 'Authentication required' using errcode = '28000';
  end if;
  if p_quantity_delta is null or p_quantity_delta = 0 then
    raise exception 'Quantity delta must not be zero' using errcode = '22023';
  end if;
  if p_client_operation_id is null then
    raise exception 'Client operation ID is required' using errcode = '22023';
  end if;
  if p_movement_type not in ('IN', 'OUT', 'RETURN', 'ADJUSTMENT') then
    raise exception 'Unsupported movement type' using errcode = '22023';
  end if;
  if (p_movement_type in ('IN', 'RETURN') and p_quantity_delta < 0)
     or (p_movement_type = 'OUT' and p_quantity_delta > 0) then
    raise exception 'Quantity sign does not match movement type' using errcode = '22023';
  end if;

  -- Fast idempotency check. A repeated ID with different content is rejected.
  select * into v_existing
  from public.stock_movements
  where owner_id = v_owner_id and client_operation_id = p_client_operation_id;

  if found then
    if v_existing.inventory_id <> p_inventory_id
       or v_existing.batch_id <> p_batch_id
       or v_existing.movement_type <> p_movement_type
       or v_existing.quantity_delta <> p_quantity_delta then
      raise exception 'Operation ID was already used for a different movement'
        using errcode = '23505';
    end if;
    return v_existing.id;
  end if;

  select b.shop_id, b.expiry_date
    into v_shop_id, v_expiry_date
  from public.stock_batches b
  join public.shop_inventory i
    on i.id = b.inventory_id and i.owner_id = b.owner_id and i.shop_id = b.shop_id
  where b.id = p_batch_id
    and b.inventory_id = p_inventory_id
    and b.owner_id = v_owner_id
    and i.owner_id = v_owner_id
  for update of b;

  if not found then
    raise exception 'Batch or inventory not found for this owner'
      using errcode = 'P0002';
  end if;

  if p_movement_type = 'OUT' and v_expiry_date <= current_date then
    raise exception 'Expired batch cannot be issued as sellable stock'
      using errcode = '22023';
  end if;

  update public.stock_batches
  set quantity = quantity + p_quantity_delta
  where id = p_batch_id
    and owner_id = v_owner_id
    and quantity + p_quantity_delta >= 0;

  if not found then
    raise exception 'Insufficient stock or invalid resulting quantity'
      using errcode = '23514';
  end if;

  insert into public.stock_movements (
    owner_id, shop_id, inventory_id, batch_id, movement_type,
    quantity_delta, reason, client_operation_id
  ) values (
    v_owner_id, v_shop_id, p_inventory_id, p_batch_id, p_movement_type,
    p_quantity_delta, p_reason, p_client_operation_id
  )
  returning id into v_movement_id;

  return v_movement_id;
end;
$$;

revoke all on function public.apply_stock_movement(uuid, uuid, text, integer, uuid, text) from public;
grant execute on function public.apply_stock_movement(uuid, uuid, text, integer, uuid, text) to authenticated;

-- Enable RLS everywhere. Policies intentionally keep owner data isolated.
alter table public.owner_profiles enable row level security;
alter table public.shops enable row level security;
alter table public.medicine_catalog enable row level security;
alter table public.shop_inventory enable row level security;
alter table public.stock_batches enable row level security;
alter table public.stock_movements enable row level security;
alter table public.purchase_shortlist enable row level security;
alter table public.backup_logs enable row level security;

create policy "owners read own profile" on public.owner_profiles
  for select to authenticated using (id = (select auth.uid()));
create policy "owners update own profile" on public.owner_profiles
  for update to authenticated using (id = (select auth.uid()))
  with check (id = (select auth.uid()));

create policy "owners manage own shops" on public.shops
  for all to authenticated using (owner_id = (select auth.uid()))
  with check (owner_id = (select auth.uid()));

create policy "owners manage own medicine catalogue" on public.medicine_catalog
  for all to authenticated using (owner_id = (select auth.uid()))
  with check (owner_id = (select auth.uid()));

create policy "owners manage own shop inventory" on public.shop_inventory
  for all to authenticated using (owner_id = (select auth.uid()))
  with check (owner_id = (select auth.uid()));

create policy "owners manage own stock batches" on public.stock_batches
  for all to authenticated using (owner_id = (select auth.uid()))
  with check (owner_id = (select auth.uid()));

create policy "owners read own stock movements" on public.stock_movements
  for select to authenticated using (owner_id = (select auth.uid()));

create policy "owners manage own purchase shortlist" on public.purchase_shortlist
  for all to authenticated using (owner_id = (select auth.uid()))
  with check (owner_id = (select auth.uid()));

create policy "owners read own backup logs" on public.backup_logs
  for select to authenticated using (owner_id = (select auth.uid()));
create policy "owners create own backup logs" on public.backup_logs
  for insert to authenticated with check (owner_id = (select auth.uid()));
create policy "owners update own backup logs" on public.backup_logs
  for update to authenticated using (owner_id = (select auth.uid()))
  with check (owner_id = (select auth.uid()));

-- Direct client writes to stock_movements are deliberately not granted by policy.
