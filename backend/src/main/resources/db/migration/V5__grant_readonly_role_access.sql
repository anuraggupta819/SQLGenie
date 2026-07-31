-- Grants readonly_query_user SELECT-only access to the target schema, and
-- ensures it stays granted on any future migrations that add tables there.
-- Idempotent: re-running a GRANT on an already-granted privilege is a
-- harmless no-op, so this is safe whether or not it was already applied
-- manually (as it was for local dev before this migration existed).
--
-- The role itself must already exist (created out-of-band, since role
-- creation needs superuser privileges Flyway's own connection does not
-- have) - see backend/scripts/local-db-setup.sql for local dev, or
-- docker/postgres-init/ for the Docker Compose setup.
GRANT USAGE ON SCHEMA target TO readonly_query_user;
GRANT SELECT ON ALL TABLES IN SCHEMA target TO readonly_query_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA target GRANT SELECT ON TABLES TO readonly_query_user;
