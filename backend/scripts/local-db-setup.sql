-- One-time local Postgres setup. Run the first block as a superuser
-- (e.g. `psql -U postgres`), then the second block connected to the
-- sqlgenie database as app_owner (it must own the target schema/tables
-- for ALTER DEFAULT PRIVILEGES to apply to future migrations).
--
-- Not run automatically by Flyway: role creation requires superuser
-- privileges the application's own database role does not have.

-- ===== Run as superuser =====
CREATE ROLE app_owner LOGIN PASSWORD 'app_owner_password';
CREATE DATABASE sqlgenie OWNER app_owner;
CREATE ROLE readonly_query_user LOGIN PASSWORD 'readonly_query_password';

-- ===== Run connected to `sqlgenie` as app_owner, AFTER Flyway has
--       created the target schema (i.e. after the app has started once) =====
GRANT USAGE ON SCHEMA target TO readonly_query_user;
GRANT SELECT ON ALL TABLES IN SCHEMA target TO readonly_query_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA target GRANT SELECT ON TABLES TO readonly_query_user;

-- Explicit, even though it's the Postgres default: readonly_query_user
-- is never granted anything on the app schema, so it cannot see users,
-- query_history, refresh_tokens, or favorite_queries under any circumstance.
REVOKE ALL ON SCHEMA app FROM readonly_query_user;
