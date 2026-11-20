import React, { useEffect, useRef } from 'react';
import type {
    ISeriesApi,
    ISeriesPrimitive,
    IPrimitivePaneView,
    IPrimitivePaneRenderer,
    AutoscaleInfo,
    IPriceLine,
} from 'lightweight-charts';
import { CandlestickSeries, ColorType, createChart, HistogramSeries, LineStyle } from 'lightweight-charts';
import { RefreshCw } from 'lucide-react';
import type { DailyCandleData } from '../services/api';
import { sanitizeAndOrderCandles } from '../utils/chartHelpers';

/* eslint-disable @typescript-eslint/no-explicit-any */

class PrevDayLevelsPrimitive implements ISeriesPrimitive {
    private _series: ISeriesApi<any> | null = null;
    private _requestUpdate?: () => void;
    private _pdh: number | null;
    private _pdl: number | null;

    constructor(pdh: number | null, pdl: number | null) {
        this._pdh = pdh;
        this._pdl = pdl;
    }

    attached(param: { series: ISeriesApi<any>; requestUpdate?: () => void }) {
        this._series = param.series;
        this._requestUpdate = param.requestUpdate;
    }

    detached() {
        this._series = null;
        this._requestUpdate = undefined;
    }

    paneViews() {
        return [new PrevDayLevelsPaneView(this)];
    }

    getPdh() { return this._pdh; }
    getPdl() { return this._pdl; }
    getSeries() { return this._series; }

    updateLevels(pdh: number | null, pdl: number | null) {
        this._pdh = pdh;
        this._pdl = pdl;
        if (this._requestUpdate) {
            this._requestUpdate();
        }
    }
}

class PrevDayLevelsPaneView implements IPrimitivePaneView {
    private _primitive: PrevDayLevelsPrimitive;

    constructor(primitive: PrevDayLevelsPrimitive) {
        this._primitive = primitive;
    }

    zOrder() {
        return 'top' as const;
    }

    renderer() {
        return new PrevDayLevelsRenderer(this._primitive);
    }
}

class PrevDayLevelsRenderer implements IPrimitivePaneRenderer {
    private _primitive: PrevDayLevelsPrimitive;

    constructor(primitive: PrevDayLevelsPrimitive) {
        this._primitive = primitive;
    }

    draw(target: any) {
        const series = this._primitive.getSeries();
        if (!series) return;

        const pdh = this._primitive.getPdh();
        const pdl = this._primitive.getPdl();
        if (pdh == null && pdl == null) return;

        target.useBitmapCoordinateSpace((scope: any) => {
            const ctx = scope.context;
            const hRatio = scope.horizontalPixelRatio;
            const vRatio = scope.verticalPixelRatio;

            const drawLabel = (price: number, label: string, color: string) => {
                const y = series.priceToCoordinate(price);
                if (y === null || isNaN(y)) return;

                ctx.save();
                ctx.font = `bold ${Math.max(10, Math.round(11 * vRatio))}px sans-serif`;
                const text = `${label} (${price.toFixed(2)})`;
                const textWidth = ctx.measureText(text).width;
                const x = 12 * hRatio;
                const boxHeight = 16 * vRatio;
                const boxY = (y - 8) * vRatio;
                const padding = 5 * hRatio;

                // Draw background pill
                ctx.fillStyle = 'rgba(15, 23, 42, 0.9)';
                ctx.strokeStyle = color;
                ctx.lineWidth = 1 * vRatio;
                ctx.beginPath();
                if (typeof ctx.roundRect === 'function') {
                    ctx.roundRect(x, boxY, textWidth + padding * 2, boxHeight, 3 * vRatio);
                } else {
                    ctx.rect(x, boxY, textWidth + padding * 2, boxHeight);
                }
                ctx.fill();
                ctx.stroke();

                // Draw text
                ctx.fillStyle = color;
                ctx.textBaseline = 'middle';
                ctx.fillText(text, x + padding, y * vRatio);
                ctx.restore();
            };

            if (pdh != null && !isNaN(pdh)) {
                drawLabel(pdh, 'Prev Day High', '#22c55e');
            }
            if (pdl != null && !isNaN(pdl)) {
                drawLabel(pdl, 'Prev Day Low', '#ef4444');
            }
        });
    }
}

export interface ChartProps {
    data: DailyCandleData[];
    symbol: string | null;
    timeframe?: string;
    showVolume?: boolean;
    previousDayHigh?: number | null;
    previousDayLow?: number | null;
    onLoadOlderData?: () => void;
}

const Chart: React.FC<ChartProps> = ({
    data,
    symbol,
    timeframe = '15M',
    showVolume = false,
    previousDayHigh = null,
    previousDayLow = null,
}) => {
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<ReturnType<typeof createChart> | null>(null);
    const candlestickSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
    const volumeSeriesRef = useRef<ISeriesApi<'Histogram'> | null>(null);
    const primitiveRef = useRef<PrevDayLevelsPrimitive | null>(null);
    const pdhLineRef = useRef<IPriceLine | null>(null);
    const pdlLineRef = useRef<IPriceLine | null>(null);
    const pdhRef = useRef<number | null>(previousDayHigh);
    const pdlRef = useRef<number | null>(previousDayLow);
    const hasFittedRef = useRef<boolean>(false);

    // Keep PDH/PDL refs current for autoscale provider
    useEffect(() => {
        pdhRef.current = previousDayHigh;
        pdlRef.current = previousDayLow;
    }, [previousDayHigh, previousDayLow]);

    // Reset fit flag when symbol or timeframe changes
    useEffect(() => {
        hasFittedRef.current = false;
    }, [symbol, timeframe]);

    // 1. Chart initialization effect: runs when container, symbol, or timeframe changes
    useEffect(() => {
        if (!chartContainerRef.current) {
            return;
        }

        const width = Math.max(chartContainerRef.current.clientWidth || 300, 100);
        const height = Math.max(chartContainerRef.current.clientHeight || 600, 100);

        const chart = createChart(chartContainerRef.current, {
            layout: {
                background: { type: ColorType.Solid, color: '#020617' },
                textColor: '#94a3b8',
            },
            grid: {
                vertLines: { color: '#1e293b' },
                horzLines: { color: '#1e293b' },
            },
            width: width,
            height: height,
            timeScale: {
                borderColor: '#334155',
                timeVisible: true,
                secondsVisible: false,
                rightOffset: 3,
            },
        });
        chartRef.current = chart;

        const candlestickSeries = chart.addSeries(CandlestickSeries, {
            upColor: '#22c55e',
            downColor: '#ef4444',
            borderVisible: false,
            wickUpColor: '#22c55e',
            wickDownColor: '#ef4444',
            autoscaleInfoProvider: (original: () => AutoscaleInfo | null) => {
                const res = original();
                if (!res || !res.priceRange) return res;
                let min = res.priceRange.minValue;
                let max = res.priceRange.maxValue;
                const pdl = pdlRef.current;
                const pdh = pdhRef.current;
                if (pdl != null && !isNaN(pdl)) {
                    min = Math.min(min, pdl);
                }
                if (pdh != null && !isNaN(pdh)) {
                    max = Math.max(max, pdh);
                }
                const span = max - min;
                const margin = span > 0 ? span * 0.02 : 5;
                return {
                    ...res,
                    priceRange: {
                        minValue: min - margin,
                        maxValue: max + margin,
                    },
                };
            },
        });

        candlestickSeries.priceScale().applyOptions({
            scaleMargins: {
                top: 0.04,
                bottom: 0.04,
            },
        });

        candlestickSeriesRef.current = candlestickSeries;

        const primitive = new PrevDayLevelsPrimitive(pdhRef.current, pdlRef.current);
        primitiveRef.current = primitive;
        candlestickSeries.attachPrimitive(primitive);

        if (showVolume) {
            const volumeSeries = chart.addSeries(HistogramSeries, {
                color: '#3b82f6',
                priceFormat: { type: 'volume' },
                priceScaleId: '',
            });
            volumeSeries.priceScale().applyOptions({ scaleMargins: { top: 0.8, bottom: 0 } });
            volumeSeriesRef.current = volumeSeries;
        }

        const resizeObserver = new ResizeObserver((entries) => {
            if (!entries || entries.length === 0) return;
            const { width: newWidth, height: newHeight } = entries[0].contentRect;
            if (newWidth > 0 && newHeight > 0 && chartRef.current) {
                chartRef.current.applyOptions({ width: newWidth, height: newHeight });
            }
        });

        if (chartContainerRef.current) {
            resizeObserver.observe(chartContainerRef.current);
        }

        return () => {
            resizeObserver.disconnect();
            candlestickSeriesRef.current = null;
            volumeSeriesRef.current = null;
            primitiveRef.current = null;
            pdhLineRef.current = null;
            pdlLineRef.current = null;
            chartRef.current = null;
            chart.remove();
        };
    }, [symbol, timeframe, showVolume]);

    // 2. Data update effect: safely updates series data without tearing down the chart instance
    useEffect(() => {
        const candlestickSeries = candlestickSeriesRef.current;
        const chart = chartRef.current;
        if (!candlestickSeries || !chart || data.length === 0) {
            return;
        }

        const sanitized = sanitizeAndOrderCandles(data);
        if (sanitized.length === 0) return;

        try {
            candlestickSeries.setData(sanitized.map((d) => ({
                time: d.time,
                open: d.open,
                high: d.high,
                low: d.low,
                close: d.close,
            })));

            if (volumeSeriesRef.current && showVolume) {
                volumeSeriesRef.current.setData(sanitized.map((d) => ({
                    time: d.time,
                    value: d.vol,
                    color: d.close >= d.open ? 'rgba(34, 197, 94, 0.5)' : 'rgba(239, 68, 68, 0.5)',
                })));
            }

            if (!hasFittedRef.current) {
                chart.timeScale().fitContent();
                hasFittedRef.current = true;
            }
        } catch (error) {
            console.error('Failed to update chart series data:', error);
        }
    }, [data, showVolume]);

    // 3. PDH / PDL level update effect: updates price lines and primitives dynamically
    useEffect(() => {
        const candlestickSeries = candlestickSeriesRef.current;
        if (!candlestickSeries) return;

        // Update PDH line
        if (pdhLineRef.current) {
            candlestickSeries.removePriceLine(pdhLineRef.current);
            pdhLineRef.current = null;
        }
        if (previousDayHigh !== null && previousDayHigh !== undefined && !isNaN(previousDayHigh)) {
            pdhLineRef.current = candlestickSeries.createPriceLine({
                price: previousDayHigh,
                color: '#22c55e',
                lineWidth: 1,
                lineStyle: LineStyle.Solid,
                axisLabelVisible: true,
                title: '',
            });
        }

        // Update PDL line
        if (pdlLineRef.current) {
            candlestickSeries.removePriceLine(pdlLineRef.current);
            pdlLineRef.current = null;
        }
        if (previousDayLow !== null && previousDayLow !== undefined && !isNaN(previousDayLow)) {
            pdlLineRef.current = candlestickSeries.createPriceLine({
                price: previousDayLow,
                color: '#ef4444',
                lineWidth: 1,
                lineStyle: LineStyle.Solid,
                axisLabelVisible: true,
                title: '',
            });
        }

        // Update primitive
        if (primitiveRef.current) {
            primitiveRef.current.updateLevels(previousDayHigh, previousDayLow);
        }
    }, [previousDayHigh, previousDayLow]);

    const handleResetZoom = () => {
        if (!chartRef.current || data.length === 0) return;
        try {
            chartRef.current.timeScale().fitContent();
        } catch (error) {
            console.warn('Failed to reset chart zoom:', error);
        }
    };

    return (
        <div className="relative w-full h-full min-h-0 flex-1 flex flex-col">
            <button
                onClick={handleResetZoom}
                className="absolute top-2 right-4 z-10 flex items-center gap-1.5 bg-slate-900/80 border border-slate-800 hover:bg-slate-800 text-slate-300 hover:text-white px-2.5 py-1 rounded shadow-lg backdrop-blur text-xs font-semibold transition-all hover:scale-105 active:scale-95"
                title="Reset zoom to fit all candles"
            >
                <RefreshCw size={12} className="animate-hover" />
                <span>Reset Zoom</span>
            </button>

            <div ref={chartContainerRef} className="w-full h-full" style={{ backgroundColor: '#020617' }} />
        </div>
    );
};

export default Chart;
