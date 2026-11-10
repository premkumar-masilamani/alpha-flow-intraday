# AGENTS.md

## Commands
```bash
# Start Postgres container + run migrations + seed data
make run_database

# Connect to psql inside the running DB container
make connect_database

# Start Spring Boot backend with continuous live reload
make run_backend

# Install dependencies and start Vite frontend dev server
make run_frontend

# Run tests and static analysis
make test
make lint
```

## Boundaries

### Always do
- Read the corresponding `.agents/AGENTS.md` file inside the `backend/`, `frontend/`, or `database/` submodules before modifying their files.
- Fail fast immediately on startup or invocation if any required configuration, environment variable, or parameter is missing or unexpanded (e.g. `${VAR}`).
- Use `org.apache.commons.lang3.StringUtils` for string blank/empty checks in the backend rather than custom string utility classes.

### Ask first
- Modifying scheduled core jobs or the downloader frequency/rate-limiting logic.

### Never do
- Commit secrets, private API keys, or `.env` files.
- Apply database migrations (all migrations are strictly manual—never ever apply them).
- Default any variable, configuration property, environment variable, or method/request parameter unless explicitly specified. Silent fallbacks are strictly prohibited.
- Add `isConfigured()` helper methods or runtime configuration presence checks in services; all required environment variables are mandatory at startup.

## Project Structure
```text
backend/           # Spring Boot 4.1 / Java 27 / Gradle
frontend/          # React 19 / Vite / TypeScript / Tailwind
database/          # Postgres 16 / Docker / migrate
.agents/           # Agent rules and context
```

## Git Workflow
- **Branch naming**: `feature/[short-desc]` or `fix/[short-desc]`.
- **Commit format**: Conventional commits (`feat(<scope>): description`).
- **PR conventions**: One logical change per PR. Never push directly to main.

## Key Invariants & Learnings
- **Intraday Engine Independence**: `alpha-flow-intraday` operates exclusively on 15M candles (`intraday_prices`), `live_quotes`, `tickers`, `market_holidays`, and `notification_logs`. There is no `daily_prices` table; previous day high (PDH), low (PDL), and close (PDC) are computed directly from the prior trading day's 15-minute candles via `intradayPriceRepository.findByTickerAndTimeframeAndPriceTimeBetweenOrderByPriceTimeAsc`.
- **Dedicated Port Alignments**:
  - Backend: `8081`
  - Frontend: `5174` (Vite, strictPort, proxying `/api` -> `http://localhost:8081`)
  - PostgreSQL Database: `5433` (Database name: `alphaflow_intraday`)
- **Single-Screen Intraday UI**: The root screen directly displays the 15-minute trading dashboard with session traversal (`<` and `>`), 15M candlestick chart, PriceSummaryBanner, and market status badge. Tab switchers and swing/positional modals are removed.

