import type { Time } from 'lightweight-charts';
import type { DailyCandleData } from '../services/api';
import { getMarketParts } from './marketStatus';

export interface SanitizedCandle {
    time: Time;
    open: number;
    high: number;
    low: number;
    close: number;
    vol: number;
}

export const toChartTime = (dateStr: string): Time => {
    if (!dateStr) return NaN as unknown as Time;
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return NaN as unknown as Time;
    const parts = getMarketParts(d, 'Asia/Kolkata');
    return Math.floor(
        Date.UTC(
            Number(parts.year),
            Number(parts.month) - 1,
            Number(parts.day),
            parts.hour,
            parts.minute,
            0
        ) / 1000
    ) as Time;
};

export const sanitizeAndOrderCandles = (candles: DailyCandleData[]): SanitizedCandle[] => {
    const valid: SanitizedCandle[] = [];
    for (const c of candles) {
        if (!c || !c.date) continue;
        const time = toChartTime(c.date);
        const open = Number(c.open);
        const high = Number(c.high);
        const low = Number(c.low);
        const close = Number(c.close);
        const vol = Number(c.vol) || 0;
        if (
            typeof time !== 'number' ||
            isNaN(time) ||
            !Number.isFinite(open) ||
            !Number.isFinite(high) ||
            !Number.isFinite(low) ||
            !Number.isFinite(close)
        ) {
            continue;
        }
        valid.push({ time, open, high, low, close, vol });
    }

    // Sort strictly ascending by time
    valid.sort((a, b) => Number(a.time) - Number(b.time));

    // Deduplicate: merge entries sharing the exact same time bucket
    const deduplicated: SanitizedCandle[] = [];
    for (const item of valid) {
        if (deduplicated.length === 0) {
            deduplicated.push(item);
        } else {
            const prev = deduplicated[deduplicated.length - 1];
            if (Number(prev.time) === Number(item.time)) {
                // Update with latest candle stats
                deduplicated[deduplicated.length - 1] = {
                    time: prev.time,
                    open: prev.open,
                    high: Math.max(prev.high, item.high),
                    low: Math.min(prev.low, item.low),
                    close: item.close,
                    vol: item.vol || prev.vol,
                };
            } else if (Number(item.time) > Number(prev.time)) {
                deduplicated.push(item);
            }
        }
    }

    return deduplicated;
};
