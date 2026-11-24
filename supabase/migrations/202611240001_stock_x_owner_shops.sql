-- STOCK X owner and multi-shop setup
-- Run this in Supabase SQL Editor. Existing medicine tables/data are untouched.

create table if not exists public.owner_profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null,
  phone text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.shops (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  phone text not null default '',
  address text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists shops_owner_id_idx on public.shops(owner_id);
alter table public.owner_profiles enable row level security;
alter table public.shops enable row level security;

drop policy if exists "Owners manage own profile" on public.owner_profiles;
create policy "Owners manage own profile" on public.owner_profiles
  for all to authenticated using (auth.uid() = id) with check (auth.uid() = id);

drop policy if exists "Owners manage own shops" on public.shops;
create policy "Owners manage own shops" on public.shops
  for all to authenticated using (auth.uid() = owner_id) with check (auth.uid() = owner_id);

grant select, insert, update, delete on public.owner_profiles to authenticated;
grant select, insert, update, delete on public.shops to authenticated;
