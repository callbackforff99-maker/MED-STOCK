-- MEDI STOCK: initial Supabase schema
-- Apply this migration in the Supabase SQL Editor.
-- Owner-only model: each account owns one or more shops; no shared shop access.

begin;

create extension if not exists pgcrypto;

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

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text,
  phone text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.shops (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  shop_name text not null check (length(trim(shop_name)) between 1 and 120),
  owner_name text,
  contact_phone text,
  address text,
  currency_code char(3) not null default 'BDT',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists shops_owner_id_idx on public.shops(owner_id);

create table if not exists public.medicines (
  id uuid primary key default gen_random_uuid(),
  shop_id uuid not null references public.shops(id) on delete cascade,
  name text not null check (length(trim(name)) between 1 and 200),
  generic_name text,
  strength text,
  dosage_form text,
  manufacturer text,
  barcode text,
  reorder_level numeric(14,3) not null default 0 check (reorder_level >= 0),
  default_unit text not null default 'pcs',
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (id, shop_id)
);

create index if not exists medicines_shop_name_idx
  on public.medicines(shop_id, lower(name));

create table if not exists public.medicine_batches (
  id uuid primary key default gen_random_uuid(),
  shop_id uuid not null,
  medicine_id uuid not null,
  batch_number text,
  expiry_date date,
  quantity numeric(14,3) not null default 0 check (quantity >= 0),
  purchase_price numeric(14,2) not null default 0 check (purchase_price >= 0),
  sale_price numeric(14,2) not null default 0 check (sale_price >= 0),
  supplier_name text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint batches_medicine_shop_fk
    foreign key (medicine_id, shop_id)
    references public.medicines(id, shop_id) on delete cascade,
  unique (id, shop_id, medicine_id)
);

create index if not exists batches_shop_expiry_idx
  on public.medicine_batches(shop_id, expiry_date);
create index if not exists batches_medicine_idx
  on public.medicine_batches(medicine_id);

create table if not exists public.stock_movements (
  id uuid primary key default gen_random_uuid(),
  shop_id uuid not null,
  medicine_id uuid not null,
  batch_id uuid not null,
  movement_type text not null check (movement_type in ('IN', 'OUT')),
  quantity numeric(14,3) not null check (quantity > 0),
  unit_cost numeric(14,2) check (unit_cost is null or unit_cost >= 0),
  note text,
  idempotency_key uuid,
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now(),
  constraint movement_batch_fk
    foreign key (batch_id, shop_id, medicine_id)
    references public.medicine_batches(id, shop_id, medicine_id) on delete cascade,
  unique (shop_id, idempotency_key)
);

create index if not exists movements_shop_created_idx
  on public.stock_movements(shop_id, created_at desc);
create index if not exists movements_batch_created_idx
  on public.stock_movements(batch_id, created_at desc);

create table if not exists public.purchase_shortlist (
  id uuid primary key default gen_random_uuid(),
  shop_id uuid not null references public.shops(id) on delete cascade,
  medicine_id uuid references public.medicines(id) on delete set null,
  item_name text not null check (length(trim(item_name)) between 1 and 200),
  desired_quantity numeric(14,3) check (desired_quantity is null or desired_quantity > 0),
  unit text not null default 'pcs',
  note text,
  is_purchased boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists shortlist_shop_status_idx
  on public.purchase_shortlist(shop_id, is_purchased, created_at desc);

-- Keep updated_at current on editable records.
drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at before update on public.profiles
for each row execute function public.set_updated_at();

drop trigger if exists shops_set_updated_at on public.shops;
create trigger shops_set_updated_at before update on public.shops
for each row execute function public.set_updated_at();

drop trigger if exists medicines_set_updated_at on public.medicines;
create trigger medicines_set_updated_at before update on public.medicines
for each row execute function public.set_updated_at();

drop trigger if exists batches_set_updated_at on public.medicine_batches;
create trigger batches_set_updated_at before update on public.medicine_batches
for each row execute function public.set_updated_at();

drop trigger if exists shortlist_set_updated_at on public.purchase_shortlist;
create trigger shortlist_set_updated_at before update on public.purchase_shortlist
for each row execute function public.set_updated_at();

-- Enable Row Level Security on every user-data table.
alter table public.profiles enable row level security;
alter table public.shops enable row level security;
alter table public.medicines enable row level security;
alter table public.medicine_batches enable row level security;
alter table public.stock_movements enable row level security;
alter table public.purchase_shortlist enable row level security;

-- Profiles: a user can only read and change their own profile.
drop policy if exists "profile_select_own" on public.profiles;
create policy "profile_select_own" on public.profiles
for select to authenticated using (id = (select auth.uid()));

drop policy if exists "profile_insert_own" on public.profiles;
create policy "profile_insert_own" on public.profiles
for insert to authenticated with check (id = (select auth.uid()));

drop policy if exists "profile_update_own" on public.profiles;
create policy "profile_update_own" on public.profiles
for update to authenticated
using (id = (select auth.uid()))
with check (id = (select auth.uid()));

-- Shops: only the owner account can access its shops.
drop policy if exists "shops_select_owner" on public.shops;
create policy "shops_select_owner" on public.shops
for select to authenticated using (owner_id = (select auth.uid()));

drop policy if exists "shops_insert_owner" on public.shops;
create policy "shops_insert_owner" on public.shops
for insert to authenticated with check (owner_id = (select auth.uid()));

drop policy if exists "shops_update_owner" on public.shops;
create policy "shops_update_owner" on public.shops
for update to authenticated
using (owner_id = (select auth.uid()))
with check (owner_id = (select auth.uid()));

drop policy if exists "shops_delete_owner" on public.shops;
create policy "shops_delete_owner" on public.shops
for delete to authenticated using (owner_id = (select auth.uid()));

-- Child records are accessible only when their shop belongs to the signed-in owner.
drop policy if exists "medicines_owner_all" on public.medicines;
create policy "medicines_owner_all" on public.medicines
for all to authenticated
using (exists (
  select 1 from public.shops s
  where s.id = medicines.shop_id and s.owner_id = (select auth.uid())
))
with check (exists (
  select 1 from public.shops s
  where s.id = medicines.shop_id and s.owner_id = (select auth.uid())
));

drop policy if exists "batches_owner_select" on public.medicine_batches;
create policy "batches_owner_select" on public.medicine_batches
for select to authenticated
using (exists (
  select 1 from public.shops s
  where s.id = medicine_batches.shop_id and s.owner_id = (select auth.uid())
));

drop policy if exists "batches_owner_insert" on public.medicine_batches;
create policy "batches_owner_insert" on public.medicine_batches
for insert to authenticated
with check (exists (
  select 1 from public.shops s
  where s.id = medicine_batches.shop_id and s.owner_id = (select auth.uid())
));

-- Quantity changes are intentionally not directly updateable by clients.
-- They must go through post_stock_movement() below so stock and history stay in sync.

drop policy if exists "movements_owner_select" on public.stock_movements;
create policy "movements_owner_select" on public.stock_movements
for select to authenticated
using (exists (
  select 1 from public.shops s
  where s.id = stock_movements.shop_id and s.owner_id = (select auth.uid())
));

drop policy if exists "shortlist_owner_all" on public.purchase_shortlist;
create policy "shortlist_owner_all" on public.purchase_shortlist
for all to authenticated
using (exists (
  select 1 from public.shops s
  where s.id = purchase_shortlist.shop_id and s.owner_id = (select auth.uid())
))
with check (exists (
  select 1 from public.shops s
  where s.id = purchase_shortlist.shop_id and s.owner_id = (select auth.uid())
));

-- Atomic stock operation. The batch quantity and movement history commit together.
-- Repeating the same idempotency key returns the original movement without applying twice.
create or replace function public.post_stock_movement(
  p_batch_id uuid,
  p_movement_type text,
  p_quantity numeric,
  p_unit_cost numeric default null,
  p_note text default null,
  p_idempotency_key uuid default null
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_batch public.medicine_batches%rowtype;
  v_movement_id uuid;
begin
  if v_user_id is null then
    raise exception 'Authentication required' using errcode = '28000';
  end if;

  if p_movement_type not in ('IN', 'OUT') then
    raise exception 'Movement type must be IN or OUT' using errcode = '22023';
  end if;

  if p_quantity is null or p_quantity <= 0 then
    raise exception 'Quantity must be greater than zero' using errcode = '22023';
  end if;

  if p_unit_cost is not null and p_unit_cost < 0 then
    raise exception 'Unit cost cannot be negative' using errcode = '22023';
  end if;

  select b.* into v_batch
  from public.medicine_batches b
  join public.shops s on s.id = b.shop_id
  where b.id = p_batch_id and s.owner_id = v_user_id
  for update of b;

  if not found then
    raise exception 'Batch not found or access denied' using errcode = '42501';
  end if;

  if p_idempotency_key is not null then
    select m.id into v_movement_id
    from public.stock_movements m
    where m.shop_id = v_batch.shop_id
      and m.idempotency_key = p_idempotency_key;

    if found then
      return v_movement_id;
    end if;
  end if;

  if p_movement_type = 'OUT' and v_batch.quantity < p_quantity then
    raise exception 'Insufficient stock' using errcode = '22003';
  end if;

  if p_movement_type = 'IN' then
    update public.medicine_batches
    set quantity = quantity + p_quantity,
        purchase_price = coalesce(p_unit_cost, purchase_price)
    where id = p_batch_id;
  else
    update public.medicine_batches
    set quantity = quantity - p_quantity
    where id = p_batch_id;
  end if;

  begin
    insert into public.stock_movements (
      shop_id, medicine_id, batch_id, movement_type,
      quantity, unit_cost, note, idempotency_key, created_by
    )
    values (
      v_batch.shop_id, v_batch.medicine_id, v_batch.id, p_movement_type,
      p_quantity, p_unit_cost, p_note, p_idempotency_key, v_user_id
    )
    returning id into v_movement_id;
  exception when unique_violation then
    -- The subtransaction rolls back the quantity update if another request
    -- inserted the same idempotency key concurrently.
    if p_idempotency_key is null then
      raise;
    end if;
    select m.id into v_movement_id
    from public.stock_movements m
    where m.shop_id = v_batch.shop_id
      and m.idempotency_key = p_idempotency_key;
    if v_movement_id is null then
      raise;
    end if;
  end;

  return v_movement_id;
end;
$$;

revoke all on function public.post_stock_movement(uuid, text, numeric, numeric, text, uuid) from public;
grant execute on function public.post_stock_movement(uuid, text, numeric, numeric, text, uuid) to authenticated;

-- Remove anonymous access; authenticated users work through RLS.
revoke all on public.profiles, public.shops, public.medicines,
  public.medicine_batches, public.stock_movements, public.purchase_shortlist
from anon;

grant select, insert, update on public.profiles to authenticated;
grant select, insert, update, delete on public.shops to authenticated;
grant select, insert, update, delete on public.medicines to authenticated;
grant select, insert on public.medicine_batches to authenticated;
grant select on public.stock_movements to authenticated;
grant select, insert, update, delete on public.purchase_shortlist to authenticated;

commit;
