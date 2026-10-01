# Local Development

## Required database environment

Golf Event Manager no longer stores the local PostgreSQL password in source control.

For the normal local database, the API defaults to:

- URL: `jdbc:postgresql://localhost:5432/myrtle_trip`
- Username: `myrtle_app`
- Password: supplied through `MYRTLE_DB_PASSWORD`

In STS, add `MYRTLE_DB_PASSWORD` to the application's Run Configuration -> Environment tab.
Only add `MYRTLE_DB_URL` when intentionally targeting a different database.

Optional overrides:

- `MYRTLE_DB_URL`
- `MYRTLE_DB_USERNAME`
- `MYRTLE_DB_PASSWORD` (required)
- `MYRTLE_FLYWAY_ENABLED` (defaults to `true`)

## Database schema

Flyway is authoritative for schema changes. Do not make an application schema change only by hand in PostgreSQL.
Add a new versioned migration under:

`src/main/resources/db/migration`

Hibernate remains configured with `ddl-auto: validate`, so the application validates that the Flyway-managed schema matches the JPA mappings.

## Backend tests

Run all backend tests from Windows with:

`scripts\test-backend.cmd`

or directly with:

`mvnw.cmd test`

The current fast unit-test suite does not require a running PostgreSQL database. Database-backed integration testing will be introduced separately so it can have an explicit, repeatable provisioning strategy rather than depending on a developer's normal `myrtle_trip` database.
