import axios from 'axios';

const API_BASE_URL = `${import.meta.env.NEXT_PUBLIC_API_URL || ''}/api`;

export const CHART_WINDOW = 250;

export interface Ticker {
  id: number;
  symbol: string;
  name: string;
}

export interface DailyCandleData {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  vol: number;
}

export type Timeframe = '15M';

export const getTickers = async (): Promise<Ticker[]> => {
  const response = await axios.get(`${API_BASE_URL}/tickers`);
  return response.data;
};

const INTRADAY_CACHE_DURATION = 30 * 1000; // 30 seconds
const MAX_CACHE_ENTRIES = 50;
const candleDataCache = new Map<string, { data: DailyCandleData[]; timestamp: number }>();

export const getCandleData = async (
  symbol: string,
  timeframe: Timeframe = '15M',
  page: number = 0,
  size: number = CHART_WINDOW,
  forceFresh = false,
  endDate?: string
): Promise<DailyCandleData[]> => {
  const now = Date.now();
  const cacheKey = `${symbol}:${timeframe}:${page}:${size ?? 'default'}:${endDate ?? 'latest'}`;
  const cached = candleDataCache.get(cacheKey);

  if (!forceFresh && cached && now - cached.timestamp < INTRADAY_CACHE_DURATION) {
    candleDataCache.delete(cacheKey);
    candleDataCache.set(cacheKey, cached);
    return cached.data;
  }

  const url = `${API_BASE_URL}/tickers/${symbol}/candle`;
  const params: Record<string, string | number> = {
    timeframe: timeframe.toLowerCase(),
    page,
    size,
  };
  if (endDate) {
    params.endDate = endDate;
  }

  const response = await axios.get(url, { params });
  candleDataCache.set(cacheKey, { data: response.data, timestamp: now });

  while (candleDataCache.size > MAX_CACHE_ENTRIES) {
    const oldestKey = candleDataCache.keys().next().value;
    if (oldestKey === undefined) break;
    candleDataCache.delete(oldestKey);
  }

  return response.data;
};

export interface MarketHoliday {
  id: number;
  country: 'IN' | 'US';
  holidayDate: string;
  name: string;
}

export const STORAGE_KEY_MARKET_HOLIDAYS = 'alphaflow_market_holidays';
export const MARKET_HOLIDAYS_TTL_MS = 24 * 60 * 60 * 1000;

export interface CachedMarketHolidays {
  timestamp: number;
  expiresAt: number;
  holidays: MarketHoliday[];
}

export const getMarketHolidays = async (): Promise<MarketHoliday[]> => {
  const url = `${API_BASE_URL}/markets/holidays`;
  const response = await axios.get(url);
  return response.data;
};

let inFlightHolidaysPromise: Promise<MarketHoliday[]> | null = null;

export const getCachedMarketHolidays = async (): Promise<MarketHoliday[]> => {
  if (typeof window !== 'undefined') {
    try {
      const raw = localStorage.getItem(STORAGE_KEY_MARKET_HOLIDAYS);
      if (raw) {
        const parsed: CachedMarketHolidays = JSON.parse(raw);
        if (
          parsed &&
          typeof parsed.expiresAt === 'number' &&
          Date.now() < parsed.expiresAt &&
          Array.isArray(parsed.holidays) &&
          parsed.holidays.length > 0
        ) {
          return parsed.holidays;
        }
      }
    } catch (error) {
      console.warn('Failed to read market holidays from localStorage:', error);
    }
  }

  if (!inFlightHolidaysPromise) {
    inFlightHolidaysPromise = getMarketHolidays()
      .then((holidays) => {
        if (typeof window !== 'undefined' && Array.isArray(holidays) && holidays.length > 0) {
          try {
            const cachePayload: CachedMarketHolidays = {
              timestamp: Date.now(),
              expiresAt: Date.now() + MARKET_HOLIDAYS_TTL_MS,
              holidays,
            };
            localStorage.setItem(STORAGE_KEY_MARKET_HOLIDAYS, JSON.stringify(cachePayload));
          } catch (error) {
            console.warn('Failed to save market holidays to localStorage:', error);
          }
        }
        return holidays;
      })
      .finally(() => {
        inFlightHolidaysPromise = null;
      });
  }

  return inFlightHolidaysPromise;
};
