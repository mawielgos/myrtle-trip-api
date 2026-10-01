# Baseline candidates

Raw `pg_dump --schema-only` output is written here by
`scripts/Export-CurrentSchemaForFlyway.ps1`.

The generated `.sql` files are intentionally ignored by Git. Review the selected
candidate before promoting a cleaned version to:

`src/main/resources/db/migration/V001__baseline.sql`
