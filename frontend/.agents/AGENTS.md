# Frontend Module Knowledge & Guidelines

## Tech Stack
- React 19, TypeScript 5.7, Vite 8, TailwindCSS 4, Lucide React, Lightweight Charts 5.
- Dev server runs on port `5174` (proxying `/api` to `http://localhost:8081`).

## Architecture & Layout
- Single-screen Intraday trading dashboard (no tabs, no swing/daily modals).
- Displays 15-minute candlestick chart for the selected trading session.
- Date traversal controls (`<` and `>`) allowing navigation between trading sessions.
- Top price summary banner: Open, High, Low, Close, Change %, Volume, Market Status badge.
- Ticker selection via Sidebar.

## Quality Standards
- `npm run lint` and `npm run build` must pass with zero errors.
