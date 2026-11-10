# AGENTS.md - Database

## Commands
```bash
# Connect to psql shell on active container
make connect_database
```

> [!CAUTION]
> **NEVER apply migrations**: Agents must NEVER run `make migrate_database`, `make run_database`, or any command that applies migrations. All migrations are strictly manual.

## Boundaries

### Always do
- Use append-only SQL migration pairs in `database/migrations/`.
- Ensure down scripts are exact reversals (`DROP TABLE IF EXISTS ... CASCADE`).
- Use `IF EXISTS` / `IF NOT EXISTS` guards for idempotency.
- Use DB migrations strictly for DDL only (schema changes).
- Author migration files only; leave execution entirely to the user (all migrations are manual).
- Author any schema updates, new columns, or alterations in a new, separately numbered migration script pair (`XXX_...up.sql` / `XXX_...down.sql`).
- Name primary keys using the `<singular_table_name>_id` convention (e.g. `ticker_id`, `intraday_price_id`, `live_quote_id`), never a generic `id`.
- Ensure child tables enforce foreign keys with `ON DELETE CASCADE` from `tickers(ticker_id)`.

### Never do
- Apply migrations (never run `make migrate_database`, `make run_database`, `migrate`, or any automated migration execution). All migrations are manual—never ever apply them.
- Edit, retroactively update, or alter existing/already-authored migration files.
- Drop tables without `CASCADE`.
- Use PostgreSQL reserved keywords for column names.

## Project Structure
```text
migrations/        # Sequentially numbered up/down SQL migration pairs
data/              # Raw SQL seed data (seed_data.sql)
scripts/           # Initialization scripts (import_seed_data.sh)
```
