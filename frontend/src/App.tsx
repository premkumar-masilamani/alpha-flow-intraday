import { useEffect, useState, useRef, useMemo } from "react";
import Header from "./components/Header";
import Sidebar from "./components/Sidebar";
import Chart from "./components/Chart";
import PriceSummaryBanner from "./components/PriceSummaryBanner";
import {
  type DailyCandleData,
  getCandleData,
  getTickers,
  type Ticker,
  getCachedMarketHolidays,
  type MarketHoliday,
} from "./services/api";
import {
  getMarketStatus,
  getExpectedIntradayDateKey,
  getCalendarDayRange,
  getDayNonTradingReason,
  type MarketCountry,
} from "./utils/marketStatus";
import {
  Loader2,
  AlertCircle,
  Clock,
} from "lucide-react";

const pctChange = (change: number, base: number): number => {
  if (!base || !Number.isFinite(base)) return 0;
  return (change / base) * 100;
};

const STORAGE_KEY_TICKER = "alphaflow_selected_ticker";

const getInitialTicker = (): string | null => {
  try {
    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      const urlTicker = params.get("ticker");
      if (urlTicker) return urlTicker;
      const savedTicker = localStorage.getItem(STORAGE_KEY_TICKER);
      if (savedTicker) return savedTicker;
    }
  } catch {
    // ignore
  }
  return null;
};

const getLocalDateKey = (dateStr: string): string => {
  const d = new Date(dateStr);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
};

const formatSectionDate = (dateStr?: string | null): string => {
  if (!dateStr || dateStr === "N/A") return "N/A";
  const parts = dateStr.split("T")[0].split("-");
  let d: Date;
  if (parts.length === 3) {
    d = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
  } else {
    d = new Date(dateStr);
  }
  if (isNaN(d.getTime())) return dateStr;
  const weekday = d.toLocaleDateString("en-US", { weekday: "short" });
  const month = d.toLocaleDateString("en-US", { month: "short" });
  const day = String(d.getDate()).padStart(2, "0");
  const year = d.getFullYear();
  return `${weekday}, ${month} ${day}, ${year}`;
};

function App() {
  const [tickers, setTickers] = useState<Ticker[]>([]);
  const [selectedTicker, setSelectedTicker] = useState<string | null>(getInitialTicker);
  const [sidebarOpen] = useState(true);

  // Intraday states
  const [rawIntradayCandles, setRawIntradayCandles] = useState<DailyCandleData[]>([]);
  const [selectedIntradayDay, setSelectedIntradayDay] = useState<string | null>(null);
  const [intradayLoading, setIntradayLoading] = useState(false);
  const [loadingOlderIntraday, setLoadingOlderIntraday] = useState(false);
  const [hasOlderIntraday, setHasOlderIntraday] = useState(true);
  const [intradayError, setIntradayError] = useState<string | null>(null);
  const [marketHolidays, setMarketHolidays] = useState<MarketHoliday[]>([]);

  useEffect(() => {
    let isMounted = true;
    getCachedMarketHolidays()
      .then((data) => {
        if (isMounted) {
          setMarketHolidays(data);
        }
      })
      .catch((error) => {
        console.warn("Failed to load market holidays:", error);
      });
    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    let isMounted = true;
    getTickers()
      .then((data) => {
        if (isMounted) {
          setTickers(data);
          if (!selectedTicker && data.length > 0) {
            const defaultTicker = data.find((t) => t.symbol === "NIFTY50") || data[0];
            setSelectedTicker(defaultTicker.symbol);
          }
        }
      })
      .catch((err) => {
        console.error("Failed to load tickers:", err);
      });
    return () => {
      isMounted = false;
    };
  }, [selectedTicker]);

  useEffect(() => {
    if (selectedTicker) {
      try {
        localStorage.setItem(STORAGE_KEY_TICKER, selectedTicker);
      } catch {
        // ignore
      }
    }
  }, [selectedTicker]);

  useEffect(() => {
    setSelectedIntradayDay(null);
    setHasOlderIntraday(true);
    setLoadingOlderIntraday(false);
  }, [selectedTicker]);

  const selectedTickerObj = useMemo(() => {
    return tickers.find((t) => t.symbol === selectedTicker);
  }, [tickers, selectedTicker]);

  const selectedTickerCountry: MarketCountry = selectedTickerObj?.country ?? "IN";
  const selectedTickerName = selectedTickerObj?.name;

  const tickerCountryRef = useRef<MarketCountry>(selectedTickerCountry);
  const marketHolidaysRef = useRef<MarketHoliday[]>(marketHolidays);

  useEffect(() => {
    tickerCountryRef.current = selectedTickerCountry;
    marketHolidaysRef.current = marketHolidays;
  }, [selectedTickerCountry, marketHolidays]);

  const expectedIntradayDay = useMemo(() => {
    return getExpectedIntradayDateKey(new Date(), selectedTickerCountry);
  }, [selectedTickerCountry]);

  // Calculate all calendar days from oldest available candle day up to expected current date
  const distinctDays = useMemo(() => {
    const sorted = [...rawIntradayCandles].sort((a, b) => a.date.localeCompare(b.date));
    const candleDays = Array.from(new Set(sorted.map((c) => getLocalDateKey(c.date))));
    if (candleDays.length === 0) {
      return expectedIntradayDay ? [expectedIntradayDay] : [];
    }
    const startDay = candleDays[0];
    const lastCandleDay = candleDays[candleDays.length - 1];
    const endDay = lastCandleDay > expectedIntradayDay ? lastCandleDay : expectedIntradayDay;
    return getCalendarDayRange(startDay, endDay);
  }, [rawIntradayCandles, expectedIntradayDay]);

  // Determine active day: selected day if valid, otherwise defaults to latest available day
  const activeDay = useMemo(() => {
    if (distinctDays.length === 0) return null;
    if (selectedIntradayDay && distinctDays.includes(selectedIntradayDay)) {
      return selectedIntradayDay;
    }
    return distinctDays[distinctDays.length - 1];
  }, [distinctDays, selectedIntradayDay]);

  const activeDayIndex = useMemo(() => {
    if (!activeDay) return -1;
    return distinctDays.indexOf(activeDay);
  }, [distinctDays, activeDay]);

  const canGoPrev = activeDayIndex > 0 || (hasOlderIntraday && rawIntradayCandles.length > 0);
  const canGoNext = activeDayIndex >= 0 && activeDayIndex < distinctDays.length - 1;

  const fetchOlderIntradayCandles = async (autoSelectPrev = false) => {
    if (loadingOlderIntraday || !hasOlderIntraday || !selectedTicker) return;
    setLoadingOlderIntraday(true);
    try {
      const candleDays = Array.from(
        new Set(rawIntradayCandles.map((c) => getLocalDateKey(c.date))),
      ).sort();
      const oldestDay = candleDays.length > 0 ? candleDays[0] : null;
      let queryEndDate: string | undefined;
      if (oldestDay) {
        const parts = oldestDay.split("-").map(Number);
        if (parts.length === 3) {
          const prevUtc = new Date(Date.UTC(parts[0], parts[1] - 1, parts[2] - 1));
          const y = prevUtc.getUTCFullYear();
          const m = String(prevUtc.getUTCMonth() + 1).padStart(2, "0");
          const d = String(prevUtc.getUTCDate()).padStart(2, "0");
          queryEndDate = `${y}-${m}-${d}`;
        } else {
          queryEndDate = oldestDay;
        }
      }
      const older = await getCandleData(selectedTicker, "15M", 0, 250, false, queryEndDate);
      const strictlyOlder = oldestDay
        ? older.filter((c) => getLocalDateKey(c.date) < oldestDay)
        : older;

      if (strictlyOlder.length === 0) {
        setHasOlderIntraday(false);
        return;
      }

      const olderDays = Array.from(
        new Set(strictlyOlder.map((c) => getLocalDateKey(c.date))),
      ).sort();
      let prevDayToSelect = olderDays[olderDays.length - 1];
      if (oldestDay && olderDays.length > 0) {
        const bridgedRange = getCalendarDayRange(olderDays[0], oldestDay);
        if (bridgedRange.length >= 2) {
          prevDayToSelect = bridgedRange[bridgedRange.length - 2];
        }
      }

      setRawIntradayCandles((prev) => {
        const merged = [...strictlyOlder, ...prev];
        const unique = Array.from(new Map(merged.map((c) => [c.date, c])).values());
        return unique.sort((a, b) => a.date.localeCompare(b.date));
      });

      if (autoSelectPrev && prevDayToSelect) {
        setSelectedIntradayDay(prevDayToSelect);
      }
    } catch (err) {
      console.warn("Failed to fetch older intraday data:", err);
    } finally {
      setLoadingOlderIntraday(false);
    }
  };

  const goToPrevDay = () => {
    if (activeDayIndex > 0) {
      setSelectedIntradayDay(distinctDays[activeDayIndex - 1]);
      if (activeDayIndex <= 5 && hasOlderIntraday && !loadingOlderIntraday) {
        fetchOlderIntradayCandles(false);
      }
    } else if (hasOlderIntraday && !loadingOlderIntraday) {
      fetchOlderIntradayCandles(true);
    }
  };

  const goToNextDay = () => {
    if (canGoNext) {
      setSelectedIntradayDay(distinctDays[activeDayIndex + 1]);
    }
  };

  const intradayCandles = useMemo(() => {
    if (!activeDay) return [];
    const filtered = rawIntradayCandles.filter((c) => getLocalDateKey(c.date) === activeDay);
    return [...filtered].sort((a, b) => a.date.localeCompare(b.date));
  }, [rawIntradayCandles, activeDay]);

  const { prevDayHigh, prevDayLow, prevDayClose } = useMemo(() => {
    if (activeDay) {
      const priorCandles = rawIntradayCandles.filter((c) => getLocalDateKey(c.date) < activeDay);
      if (priorCandles.length > 0) {
        const sortedPrior = [...priorCandles].sort((a, b) => a.date.localeCompare(b.date));
        const latestPriorDayKey = getLocalDateKey(sortedPrior[sortedPrior.length - 1].date);
        const prevCandles = sortedPrior.filter(
          (c) => getLocalDateKey(c.date) === latestPriorDayKey,
        );
        return {
          prevDayHigh: Math.max(...prevCandles.map((c) => Number(c.high))),
          prevDayLow: Math.min(...prevCandles.map((c) => Number(c.low))),
          prevDayClose: Number(prevCandles[prevCandles.length - 1].close),
        };
      }
    }
    return { prevDayHigh: null, prevDayLow: null, prevDayClose: null };
  }, [rawIntradayCandles, activeDay]);

  useEffect(() => {
    if (!selectedTicker) return;

    let active = true;

    const fetchIntraday = async (isInitial = false) => {
      if (isInitial) {
        setIntradayLoading(true);
        setIntradayError(null);
      }
      try {
        const rawCandles = await getCandleData(selectedTicker, "15M", 0, 500, !isInitial);

        if (!active) return;

        if (rawCandles.length === 0) {
          if (isInitial) {
            setRawIntradayCandles([]);
            setHasOlderIntraday(false);
          }
          return;
        }

        if (isInitial) {
          setRawIntradayCandles(rawCandles);
        } else {
          setRawIntradayCandles((prev) => {
            if (prev.length === 0) return rawCandles;
            const byDate = new Map(prev.map((c) => [c.date, c]));
            for (const c of rawCandles) {
              byDate.set(c.date, c);
            }
            return Array.from(byDate.values()).sort((a, b) => a.date.localeCompare(b.date));
          });
        }
      } catch (err) {
        if (active && isInitial) {
          console.error("Failed to load intraday data:", err);
          setIntradayError("Failed to load intraday data");
        }
      } finally {
        if (active && isInitial) {
          setIntradayLoading(false);
        }
      }
    };

    fetchIntraday(true);

    const intervalTimer = setInterval(() => {
      const status = getMarketStatus(new Date(), tickerCountryRef.current, marketHolidaysRef.current);
      if (status.isOpen) {
        fetchIntraday(false);
      }
    }, 15_000);

    return () => {
      active = false;
      clearInterval(intervalTimer);
    };
  }, [selectedTicker]);

  const hasCandlesForActiveDay = intradayCandles.length > 0;
  const latestCandle = hasCandlesForActiveDay ? intradayCandles[intradayCandles.length - 1] : null;
  const firstCandle = hasCandlesForActiveDay ? intradayCandles[0] : null;

  const ltp: number | null = latestCandle ? Number(latestCandle.close) : null;
  const openPrice = firstCandle ? Number(firstCandle.open) : null;
  const highPrice = hasCandlesForActiveDay
    ? Math.max(...intradayCandles.map((c) => Number(c.high)))
    : null;
  const lowPrice = hasCandlesForActiveDay
    ? Math.min(...intradayCandles.map((c) => Number(c.low)))
    : null;
  const closePrice = latestCandle ? Number(latestCandle.close) : null;

  const changeBase = prevDayClose ?? openPrice;
  const referencePrice = closePrice;
  const hasValidChange = referencePrice !== null && changeBase !== null && changeBase > 0;
  const change = hasValidChange ? referencePrice - changeBase : 0;
  const pct = hasValidChange ? pctChange(change, changeBase) : 0;

  const chartDate = latestCandle?.date || activeDay || expectedIntradayDay;
  const chartDateFormatted = chartDate ? formatSectionDate(chartDate) : null;
  const totalDayVolume = hasCandlesForActiveDay
    ? intradayCandles.reduce((acc, c) => acc + (Number(c.vol) || 0), 0)
    : null;
  const nonTradingReason = activeDay
    ? getDayNonTradingReason(activeDay, selectedTickerCountry, marketHolidays)
    : null;

  return (
    <div className="flex flex-col h-screen bg-slate-900 overflow-hidden">
      <Header />
      <div className="flex flex-1 overflow-hidden">
        {sidebarOpen && (
          <Sidebar
            tickers={tickers}
            selectedTicker={selectedTicker}
            onSelectTicker={setSelectedTicker}
          />
        )}
        <main className="flex-1 flex flex-col bg-slate-950 overflow-hidden">
          {!selectedTicker ? (
            <div className="flex-1 flex items-center justify-center text-slate-500 italic">
              Select a ticker to view intraday charts
            </div>
          ) : intradayLoading && rawIntradayCandles.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center text-slate-500 gap-3">
              <Loader2 className="animate-spin text-blue-500" size={32} />
              <p className="text-sm font-semibold text-slate-400">Loading intraday 15-minute data...</p>
            </div>
          ) : (
            <div className="flex-1 flex flex-col gap-3 p-4 overflow-hidden">
              {/* Header Bar with Timeframe Badge */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-lg font-bold text-white font-mono">{selectedTicker}</span>
                  {selectedTickerName && (
                    <span className="text-sm text-slate-400 font-medium">({selectedTickerName})</span>
                  )}
                </div>
                <div className="flex items-center gap-1.5 px-3 py-1 bg-slate-900 border border-slate-700 rounded-lg text-xs font-semibold text-blue-400 font-mono shadow-sm">
                  <Clock size={13} className="text-blue-400" />
                  <span>15M Timeframe</span>
                </div>
              </div>

              {/* Quote / Summary Banner */}
              <PriceSummaryBanner
                ltp={ltp}
                change={hasValidChange ? change : null}
                changePct={hasValidChange ? pct : null}
                changeSubtitle="(vs last day close)"
                open={openPrice}
                high={highPrice}
                low={lowPrice}
                close={closePrice}
                volume={totalDayVolume}
                date={chartDateFormatted}
                traversal={{
                  canGoPrev,
                  canGoNext,
                  onPrev: goToPrevDay,
                  onNext: goToNextDay,
                  loadingOlder: loadingOlderIntraday,
                  isAtBoundary: activeDayIndex === 0,
                }}
              />

              {/* 15m Candlestick Chart */}
              <div className="flex-1 relative min-h-0 bg-slate-950 border border-slate-800 rounded-lg overflow-hidden">
                {intradayCandles.length > 0 ? (
                  <Chart
                    data={intradayCandles}
                    indicators={[]}
                    enabled={new Set()}
                    configs={[]}
                    symbol={selectedTicker}
                    timeframe="15M"
                    showVolume={false}
                    previousDayHigh={prevDayHigh}
                    previousDayLow={prevDayLow}
                    onLoadOlderData={() => {}}
                  />
                ) : (
                  <div className="absolute inset-0 flex flex-col items-center justify-center text-slate-500 gap-3 p-8">
                    {intradayLoading ? (
                      <>
                        <Loader2 className="animate-spin text-blue-500" size={32} />
                        <p className="text-sm font-semibold text-slate-400">Loading chart data...</p>
                      </>
                    ) : (
                      <div className="bg-amber-900/20 border border-amber-500/30 rounded-xl p-6 max-w-md w-full text-center">
                        <AlertCircle className="text-amber-500 mx-auto mb-4" size={48} />
                        <h3 className="text-lg font-bold text-white">
                          {intradayError
                            ? "Failed to Load Intraday Data"
                            : rawIntradayCandles.length === 0
                              ? "Intraday Data Unavailable"
                              : nonTradingReason?.type === "WEEKEND"
                                ? "Market Closed — Weekend"
                                : nonTradingReason?.type === "HOLIDAY"
                                  ? `Market Closed — ${nonTradingReason.label}`
                                  : "Awaiting Intraday Data"}
                        </h3>
                        <p className="text-amber-200/70 mt-2 text-sm leading-relaxed">
                          {intradayError ? (
                            <>
                              Failed to load 15-minute intraday data for{" "}
                              <span className="font-mono text-blue-400 font-semibold">{selectedTicker}</span>
                              {selectedTickerName && selectedTickerName !== selectedTicker
                                ? ` (${selectedTickerName})`
                                : ""}
                              . Please check your network connection or try again later.
                            </>
                          ) : rawIntradayCandles.length === 0 ? (
                            <>
                              Intraday data is not available for{" "}
                              <span className="font-mono text-blue-400 font-semibold">{selectedTicker}</span>
                              {selectedTickerName && selectedTickerName !== selectedTicker
                                ? ` (${selectedTickerName})`
                                : ""}
                              .
                            </>
                          ) : nonTradingReason?.type === "WEEKEND" ? (
                            <>
                              No 15-minute intraday data is available for{" "}
                              <span className="font-mono text-blue-400 font-semibold">{selectedTicker}</span> on{" "}
                              <span className="font-mono text-amber-300 font-semibold">{chartDateFormatted}</span> because the
                              market is closed for the weekend ({nonTradingReason.label}).
                            </>
                          ) : nonTradingReason?.type === "HOLIDAY" ? (
                            <>
                              No 15-minute intraday data is available for{" "}
                              <span className="font-mono text-blue-400 font-semibold">{selectedTicker}</span> on{" "}
                              <span className="font-mono text-amber-300 font-semibold">{chartDateFormatted}</span> because the
                              market is closed for{" "}
                              <span className="font-semibold text-amber-300">{nonTradingReason.label}</span>.
                            </>
                          ) : (
                            <>
                              15-minute intraday candles for{" "}
                              <span className="font-mono text-blue-400 font-semibold">{selectedTicker}</span> have not yet
                              been populated for{" "}
                              <span className="font-mono text-amber-300 font-semibold">{chartDateFormatted}</span>.
                            </>
                          )}
                        </p>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

export default App;
