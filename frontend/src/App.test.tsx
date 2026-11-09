import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import App from './App';
import * as api from './services/api';
import type { DailyCandleData } from './services/api';

vi.mock('./services/api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./services/api')>();
  return {
    ...actual,
    getTickers: vi.fn(),
    getCandleData: vi.fn(),
    getCachedMarketHolidays: vi.fn(),
  };
});

let lastRenderedChartData: DailyCandleData[] = [];

vi.mock('./components/Chart', () => ({
  default: ({ data }: { data: DailyCandleData[] }) => {
    lastRenderedChartData = data;
    return (
      <div data-testid="mock-chart" data-candle-count={data.length}>
        Mock Chart ({data.length} candles)
      </div>
    );
  },
}));

describe('App Intraday Root View', () => {
  const yesterdayCandles: DailyCandleData[] = [
    {
      date: '2026-10-15T09:15:00+05:30',
      open: 22671.3,
      high: 22800.0,
      low: 22637.4,
      close: 22750.0,
      vol: 1000,
    },
    {
      date: '2026-10-15T15:15:00+05:30',
      open: 22750.0,
      high: 22850.0,
      low: 22700.0,
      close: 22622.7,
      vol: 1200,
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    lastRenderedChartData = [];
    window.history.replaceState({}, '', '/?ticker=NIFTY50');

    vi.mocked(api.getTickers).mockResolvedValue([
      {
        id: 1,
        symbol: 'NIFTY50',
        name: 'NIFTY 50',
        type: 'INDEX',
        country: 'IN',
      },
    ]);
    vi.mocked(api.getCachedMarketHolidays).mockResolvedValue([
      {
        id: 1,
        country: 'IN',
        holidayDate: '2026-11-24',
        name: 'Prakash Gurpurb Sri Guru Nanak Dev',
      },
    ]);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders intraday dashboard directly at root URL without tab toggle buttons', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.setSystemTime(new Date('2026-10-16T04:01:15Z'));

    vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
      if (tf === '15M') {
        return yesterdayCandles;
      }
      return [];
    });

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('15M Timeframe')).toBeInTheDocument();
    });

    // Ensure tab toggles are eliminated
    expect(screen.queryByRole('button', { name: /overview/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /charts/i })).not.toBeInTheDocument();

    // Check header and sidebar
    expect(screen.getByText('Alpha Flow')).toBeInTheDocument();
    expect(screen.getAllByText('NIFTY50').length).toBeGreaterThanOrEqual(1);
  });

  it('defaults to expected day and shows Awaiting Intraday Data when today has no candles', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.setSystemTime(new Date('2026-10-16T04:01:15Z'));

    vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
      if (tf === '15M') {
        return yesterdayCandles;
      }
      return [];
    });

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('Awaiting Intraday Data')).toBeInTheDocument();
    });

    expect(screen.getAllByText(/Fri, Oct 16, 2026/i).length).toBeGreaterThanOrEqual(1);
  });

  it('navigates to previous day upon clicking prev button and renders chart', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.setSystemTime(new Date('2026-10-16T04:01:15Z'));

    vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
      if (tf === '15M') {
        return yesterdayCandles;
      }
      return [];
    });

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('Awaiting Intraday Data')).toBeInTheDocument();
    });

    const prevBtn = screen.getByRole('button', { name: /previous day/i });
    expect(prevBtn).toBeEnabled();
    fireEvent.click(prevBtn);

    await waitFor(() => {
      expect(screen.getByTestId('mock-chart')).toBeInTheDocument();
    });

    expect(screen.getByText(/Thu, Oct 15, 2026/i)).toBeInTheDocument();
    expect(lastRenderedChartData.length).toBe(2);
  });
});
