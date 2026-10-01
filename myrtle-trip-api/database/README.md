# Golf Event Manager database workflow

The application currently relies on an already-created PostgreSQL database and
Hibernate `ddl-auto: validate`. Flyway is installed but intentionally remains
disabled until the current working schema has been captured and reviewed.

## Migration policy

Once the baseline is established:

1. `src/main/resources/db/migration/V001__baseline.sql` creates the current schema
   from an empty PostgreSQL database.
2. Every later schema change is represented by a new immutable Flyway migration,
   for example `V002__add_round_setting.sql`.
3. Hibernate remains `ddl-auto: validate`; Hibernate does not create or modify
   production schema objects.
4. Existing versioned migration files are never edited after they have been
   applied to a shared database. Corrections are made with a new migration.
5. Flyway `clean` remains disabled.

## First baseline capture

From the API project root on Windows PowerShell:

```powershell
.\scripts\Export-CurrentSchemaForFlyway.ps1
```

If `pg_dump` is not on PATH:

```powershell
.\scripts\Export-CurrentSchemaForFlyway.ps1 `
  -PgDumpPath "C:\Program Files\PostgreSQL\17\bin\pg_dump.exe"
```

The script exports schema only. It writes the result under
`database\baseline-candidate\` and does not put it in Flyway's migration folder.
The raw dump must be reviewed before it becomes `V001__baseline.sql`.

Do not enable Flyway merely because a raw dump exists. The baseline must first be
verified by building a new empty database and allowing Hibernate validation to
confirm that the reconstructed schema matches the entities.
