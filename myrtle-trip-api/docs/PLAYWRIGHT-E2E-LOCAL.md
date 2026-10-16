# Local Playwright E2E backend (Pass 166)

The normal DEV Spring profile and database are unchanged. The E2E API is isolated on port 8081, using a separate database on the same local PostgreSQL server.

## One-time database creation

Connect using pgAdmin or psql as a PostgreSQL administrator. The role `myrtle_app` is the default application user in `application.yml`. If your actual application role is different, change `OWNER` and `MYRTLE_E2E_DB_USERNAME` accordingly.

```sql
CREATE DATABASE myrtle_trip_e2e_test OWNER myrtle_app;
```

Do **not** copy DEV tables/data. Creating a fresh database lets Flyway execute `V001__baseline.sql`. Its existing settings keep Flyway `clean` disabled and Hibernate schema validation enabled.

## Start backend in PowerShell

From `C:\Devo\GolfTripManager\myrtle-trip-api`:

```powershell
$env:MYRTLE_E2E_DB_PASSWORD = '<your local app role password>'
$env:SPRING_PROFILES_ACTIVE = 'e2e'
.\mvnw.cmd spring-boot:run
```

Use an STS run configuration with `spring.profiles.active=e2e` and environment variable `MYRTLE_E2E_DB_PASSWORD` if preferred. Do not commit credentials. The API should start on port 8081. Verify the startup logs show `myrtle_trip_e2e_test` and that Flyway migrated the fresh schema. Stop if anything unexpected is logged.

If your local PostgreSQL instance is not on port 5432, adjust the profile and safeguard together; do not disable the safeguard.

## Safety behavior

When the `e2e` profile is active, startup fails **before Flyway** if the effective datasource URL is not the exact local E2E database or if the backend port differs from 8081. A URL override to the DEV database will therefore be rejected. Do not manually point browser tests at DEV until the UI E2E configuration and environment checks are completed in the next step.

## Next

After the E2E API is running and its schema validated, wire Playwright to port 8081 via Vite's proxy, add a database guard to the browser test harness, and add the first real event-creation test. The existing mocked smoke test continues to run separately.
