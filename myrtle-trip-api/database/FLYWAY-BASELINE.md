# Golf Event Manager Flyway baseline

`V001__baseline.sql` was generated from a schema-only `pg_dump` of the known-good
`myrtle_trip` PostgreSQL database on 2026-10-01.

## Existing database

Flyway uses `baseline-on-migrate=true` and `baseline-version=1`. On the first
startup against the existing non-empty database, Flyway creates its schema
history table and records version 1 as the baseline. It does **not** execute
`V001__baseline.sql` over the existing tables.

## Empty database

On a fresh empty database, Flyway executes `V001__baseline.sql`, after which
Hibernate's `ddl-auto=validate` checks that the resulting schema matches the
JPA mappings.

## Going forward

All schema changes must be committed as new versioned migrations, for example:

- `V002__add_example_column.sql`
- `V003__create_example_index.sql`

Do not edit `V001__baseline.sql` after this foundation is committed.
Do not make manual schema changes without a corresponding Flyway migration.
