import React, { useEffect, useRef } from 'react';
import type {
    ISeriesApi,
    Time,
    ISeriesPrimitive,
    IPrimitivePaneView,
    IPrimitivePaneRenderer,
    AutoscaleInfo,
} from 'lightweight-charts';
import { CandlestickSeries, ColorType, createChart, HistogramSeries, LineStyle } from 'lightweight-charts';
import { RefreshCw } from 'lucide-react';
import type { DailyCandleData } from '../services/api';

/* eslint-disable @typescript-eslint/no-explicit-any */

class PrevDayLevelsPrimitive implements ISeriesPrimitive {
    private _series: ISeriesApi<any> | null = null;
    private _pdh: number | null;
    private _pdl: number | null;

    constructor(pdh: number | null, pdl: number | null) {
        this._pdh = pdh;
        this._pdl = pdl;
    }

    attached(param: { series: ISeriesApi<any> }) {
        this._series = param.series;
    }

    detached() {
        this._series = null;
    }

    paneViews() {
        return [new PrevDayLevelsPaneView(this)];
    }

    getPdh() { return this._pdh; }
    getPdl() { return this._pdl; }
    getSeries() { return this._series; }
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
                if (y === null) return;

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

const toChartTime = (dateStr: string): Time => {
    if (!dateStr) return dateStr as Time;
    const d = new Date(dateStr);
    return Math.floor(
        Date.UTC(
            d.getFullYear(),
            d.getMonth(),
            d.getDate(),
            d.getHours(),
            d.getMinutes(),
            d.getSeconds()
        ) / 1000
    ) as Time;
};

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

    useEffect(() => {
        if (!chartContainerRef.current || data.length === 0) {
            return;
        }

        const width = chartContainerRef.current.clientWidth;
        const height = chartContainerRef.current.clientHeight || 600;

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

        const sortedData = [...data].sort((a, b) => a.date.localeCompare(b.date));

        const candlestickSeries = chart.addSeries(CandlestickSeries, {
            upColor: '#22c55e',
            downColor: '#ef4444',
            borderVisible: false,
            wickUpColor: '#22c55e',
            wickDownColor: '#ef4444',
            ...(previousDayHigh != null || previousDayLow != null ? {
                autoscaleInfoProvider: (original: () => AutoscaleInfo | null) => {
                    const res = original();
                    if (!res || !res.priceRange) return res;
                    let min = res.priceRange.minValue;
                    let max = res.priceRange.maxValue;
                    if (previousDayLow != null && !isNaN(previousDayLow)) {
                        min = Math.min(min, previousDayLow);
                    }
                    if (previousDayHigh != null && !isNaN(previousDayHigh)) {
                        max = Math.max(max, previousDayHigh);
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
                }
            } : {}),
        });

        candlestickSeries.priceScale().applyOptions({
            scaleMargins: {
                top: 0.04,
                bottom: 0.04,
            },
        });

        candlestickSeries.setData(sortedData.map((d) => ({
            time: toChartTime(d.date),
            open: Number(d.open),
            high: Number(d.high),
            low: Number(d.low),
            close: Number(d.close),
        })));

        if (previousDayHigh !== undefined && previousDayHigh !== null && !isNaN(previousDayHigh)) {
            candlestickSeries.createPriceLine({
                price: previousDayHigh,
                color: '#22c55e',
                lineWidth: 1,
                lineStyle: LineStyle.Solid,
                axisLabelVisible: true,
                title: '',
            });
        }

        if (previousDayLow !== undefined && previousDayLow !== null && !isNaN(previousDayLow)) {
            candlestickSeries.createPriceLine({
                price: previousDayLow,
                color: '#ef4444',
                lineWidth: 1,
                lineStyle: LineStyle.Solid,
                axisLabelVisible: true,
                title: '',
            });
        }

        if ((previousDayHigh != null && !isNaN(previousDayHigh)) || (previousDayLow != null && !isNaN(previousDayLow))) {
            candlestickSeries.attachPrimitive(new PrevDayLevelsPrimitive(previousDayHigh, previousDayLow));
        }

        if (showVolume) {
            const volumeSeries = chart.addSeries(HistogramSeries, {
                color: '#3b82f6',
                priceFormat: { type: 'volume' },
                priceScaleId: '',
            });
            volumeSeries.priceScale().applyOptions({ scaleMargins: { top: 0.8, bottom: 0 } });
            volumeSeries.setData(sortedData.map((d) => ({
                time: toChartTime(d.date),
                value: Number(d.vol),
                color: Number(d.close) >= Number(d.open) ? 'rgba(34, 197, 94, 0.5)' : 'rgba(239, 68, 68, 0.5)',
            })));
        }

        chart.timeScale().fitContent();

        const resizeObserver = new ResizeObserver((entries) => {
            if (!entries || entries.length === 0) return;
            const { width: newWidth, height: newHeight } = entries[0].contentRect;
            chart.applyOptions({ width: newWidth, height: newHeight });
        });

        if (chartContainerRef.current) {
            resizeObserver.observe(chartContainerRef.current);
        }

        return () => {
            resizeObserver.disconnect();
            chartRef.current = null;
            chart.remove();
        };
    }, [data, symbol, timeframe, showVolume, previousDayHigh, previousDayLow]);

    const handleResetZoom = () => {
        if (!chartRef.current || data.length === 0) return;
        chartRef.current.timeScale().fitContent();
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
