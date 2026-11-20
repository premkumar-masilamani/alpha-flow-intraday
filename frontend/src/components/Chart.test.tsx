import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import Chart from './Chart';
import { toChartTime, sanitizeAndOrderCandles } from '../utils/chartHelpers';
import type { DailyCandleData } from '../services/api';

describe('Chart Component & Candle Sanitization', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('toChartTime', () => {
    it('converts date strings to valid epoch timestamps based on Indian market time', () => {
      const time = toChartTime('2026-11-20T09:15:00+05:30');
      expect(typeof time).toBe('number');
      expect(Number.isFinite(time)).toBe(true);
      expect(time).toBeGreaterThan(0);
    });

    it('returns NaN for invalid or empty date strings', () => {
      expect(toChartTime('')).toBeNaN();
      expect(toChartTime('invalid-date')).toBeNaN();
    });
  });

  describe('sanitizeAndOrderCandles', () => {
    it('filters out candles with invalid/NaN price values or timestamps', () => {
      const dirtyCandles: DailyCandleData[] = [
        { date: 'invalid', open: 100, high: 110, low: 90, close: 105, vol: 10 },
        { date: '2026-11-20T09:15:00+05:30', open: NaN, high: 110, low: 90, close: 105, vol: 10 },
        { date: '2026-11-20T09:30:00+05:30', open: 100, high: 110, low: 90, close: 105, vol: 10 },
      ];

      const clean = sanitizeAndOrderCandles(dirtyCandles);
      expect(clean.length).toBe(1);
      expect(clean[0].open).toBe(100);
    });

    it('sorts candles into strictly ascending time order', () => {
      const unsortedCandles: DailyCandleData[] = [
        { date: '2026-11-20T10:00:00+05:30', open: 102, high: 108, low: 101, close: 105, vol: 20 },
        { date: '2026-11-20T09:15:00+05:30', open: 100, high: 105, low: 99, close: 101, vol: 10 },
        { date: '2026-11-20T09:45:00+05:30', open: 101, high: 106, low: 100, close: 102, vol: 15 },
      ];

      const sorted = sanitizeAndOrderCandles(unsortedCandles);
      expect(sorted.length).toBe(3);
      expect(Number(sorted[0].time)).toBeLessThan(Number(sorted[1].time));
      expect(Number(sorted[1].time)).toBeLessThan(Number(sorted[2].time));
    });

    it('deduplicates candles sharing identical timestamps without throwing assertions', () => {
      const duplicateCandles: DailyCandleData[] = [
        { date: '2026-11-20T09:15:00+05:30', open: 100, high: 105, low: 99, close: 102, vol: 10 },
        { date: '2026-11-20T09:15:00+05:30', open: 100, high: 108, low: 98, close: 104, vol: 25 },
        { date: '2026-11-20T09:30:00+05:30', open: 104, high: 110, low: 103, close: 109, vol: 30 },
      ];

      const deduplicated = sanitizeAndOrderCandles(duplicateCandles);
      expect(deduplicated.length).toBe(2);
      expect(deduplicated[0].high).toBe(108);
      expect(deduplicated[0].low).toBe(98);
      expect(deduplicated[0].close).toBe(104);
      expect(deduplicated[0].vol).toBe(25);
    });
  });

  describe('Chart component rendering', () => {
    it('renders container and reset zoom button without crashing', () => {
      const sampleCandles: DailyCandleData[] = [
        { date: '2026-11-20T09:15:00+05:30', open: 22000, high: 22100, low: 21950, close: 22050, vol: 500 },
        { date: '2026-11-20T09:30:00+05:30', open: 22050, high: 22150, low: 22000, close: 22120, vol: 600 },
      ];

      render(
        <Chart
          data={sampleCandles}
          symbol="NIFTY50"
          timeframe="15M"
          previousDayHigh={22200}
          previousDayLow={21800}
        />
      );

      expect(screen.getByRole('button', { name: /reset zoom/i })).toBeInTheDocument();
    });
  });
});
