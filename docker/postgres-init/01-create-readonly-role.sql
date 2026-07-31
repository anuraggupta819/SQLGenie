-- Runs automatically on first container init (official postgres image
-- convention: any .sql/.sh file in /docker-entrypoint-initdb.d/ runs once,
-- as the bootstrap superuser, only when the data volume is empty).
--
-- app_owner and the sqlgenie database itself are created by the image's own
-- POSTGRES_USER/POSTGRES_PASSWORD/POSTGRES_DB env vars - this script only
-- adds the second, deliberately-restricted role. Its actual grants on the
-- target schema are applied later by V5__grant_readonly_role_access.sql,
-- once the backend's own Flyway migrations have created that schema.
CREATE ROLE readonly_query_user LOGIN PASSWORD 'readonly_query_password';
