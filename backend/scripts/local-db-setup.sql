-- One-time local Postgres setup. Run as a superuser (e.g. `psql -U postgres`).
--
-- Not run automatically by Flyway: role creation requires superuser
-- privileges the application's own database role does not have. The actual
-- readonly_query_user grants (USAGE on target, SELECT on its tables) are
-- handled automatically by V5__grant_readonly_role_access.sql the first
-- time the app starts against this database - nothing further to run here.
CREATE ROLE app_owner LOGIN PASSWORD 'app_owner_password';
CREATE DATABASE sqlgenie OWNER app_owner;
CREATE ROLE readonly_query_user LOGIN PASSWORD 'readonly_query_password';

-- No REVOKE ON SCHEMA app needed here (nor possible yet - that schema
-- doesn't exist until Flyway's first migration runs): readonly_query_user
-- is simply never granted anything there, and Postgres denies by default.
