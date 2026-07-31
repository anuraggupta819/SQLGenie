-- Runs once, immediately after the Testcontainers Postgres instance starts,
-- before Spring context (and therefore Flyway) initializes - see
-- AbstractIntegrationTest. Creates the readonly_query_user role so
-- V5__grant_readonly_role_access.sql doesn't fail in every integration
-- test, not just the ones that specifically exercise that role.
CREATE ROLE readonly_query_user LOGIN PASSWORD 'readonly_query_password';
