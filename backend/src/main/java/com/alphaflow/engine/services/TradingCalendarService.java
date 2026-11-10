package com.alphaflow.engine.services;

import com.alphaflow.persistence.entities.MarketHoliday;
import com.alphaflow.persistence.repositories.MarketHolidayRepository;
import jakarta.annotation.PostConstruct;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TradingCalendarService {

  private static final Set<LocalDate> HOLIDAYS = new HashSet<>();
  private static final Map<LocalDate, String> HOLIDAY_NAMES = new HashMap<>();

  private final MarketHolidayRepository marketHolidayRepository;

  public TradingCalendarService(MarketHolidayRepository marketHolidayRepository) {
    this.marketHolidayRepository = marketHolidayRepository;
    if (marketHolidayRepository != null) {
      loadHolidays();
    } else {
      clearHolidays();
    }
  }

  @PostConstruct
  public void initializeHolidays() {
    if (marketHolidayRepository != null) {
      loadHolidays();
    } else {
      clearHolidays();
    }
  }

  public final synchronized void loadHolidays() {
    if (marketHolidayRepository != null) {
      List<MarketHoliday> holidayList = marketHolidayRepository.findAllByOrderByHolidayDateAsc();
      HOLIDAYS.clear();
      HOLIDAY_NAMES.clear();

      for (MarketHoliday holiday : holidayList) {
        HOLIDAYS.add(holiday.getHolidayDate());
        HOLIDAY_NAMES.put(holiday.getHolidayDate(), holiday.getName());
      }
    } else {
      clearHolidays();
    }
  }

  public static synchronized void clearHolidays() {
    HOLIDAYS.clear();
    HOLIDAY_NAMES.clear();
  }

  public static synchronized void setHolidays(Map<LocalDate, String> holidays) {
    clearHolidays();
    if (holidays != null) {
      HOLIDAYS.addAll(holidays.keySet());
      HOLIDAY_NAMES.putAll(holidays);
    }
  }

  public static Set<LocalDate> getHolidays() {
    return Set.copyOf(HOLIDAYS);
  }

  public static Optional<String> getHolidayName(LocalDate date) {
    return Optional.ofNullable(HOLIDAY_NAMES.get(date));
  }

  public static boolean isTradingDay(LocalDate date) {
    DayOfWeek dayOfWeek = date.getDayOfWeek();
    if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
      return !HOLIDAYS.contains(date);
    } else {
      return false;
    }
  }

  public static boolean isMarketSessionOpen(
      ZonedDateTime currentMarketTime,
      LocalTime sessionStartTime,
      LocalTime sessionEndTime) {
    LocalTime currentTime = currentMarketTime.toLocalTime();
    return isTradingDay(currentMarketTime.toLocalDate())
        && !currentTime.isBefore(sessionStartTime)
        && !currentTime.isAfter(sessionEndTime);
  }

  public static LocalDate getPreviousTradingDay(LocalDate date) {
    LocalDate candidate = date.minusDays(1);
    while (!isTradingDay(candidate)) {
      candidate = candidate.minusDays(1);
    }
    return candidate;
  }

  public static OffsetDateTime getExpectedLatestCompletedCandleTime(
      ZonedDateTime nowInMarket,
      LocalTime sessionStart,
      LocalTime sessionEnd,
      int candleMinutes) {
    LocalTime lastCandleStartTime = sessionEnd.minusMinutes(candleMinutes);
    LocalDate currentDate = nowInMarket.toLocalDate();
    LocalTime currentTime = nowInMarket.toLocalTime();
    LocalTime firstCandleEndTime = sessionStart.plusMinutes(candleMinutes);

    if (isTradingDay(currentDate) && !currentTime.isBefore(firstCandleEndTime)) {
      if (currentTime.isBefore(sessionEnd)) {
        long elapsedMinutes = Duration.between(sessionStart, currentTime).toMinutes();
        long completedCandleCount = elapsedMinutes / candleMinutes;
        LocalTime candleStartTime =
            sessionStart.plusMinutes((completedCandleCount - 1) * candleMinutes);
        return currentDate.atTime(candleStartTime).atZone(nowInMarket.getZone()).toOffsetDateTime();
      } else {
        return currentDate
            .atTime(lastCandleStartTime)
            .atZone(nowInMarket.getZone())
            .toOffsetDateTime();
      }
    } else {
      LocalDate previousTradingDay = getPreviousTradingDay(currentDate);
      return previousTradingDay
          .atTime(lastCandleStartTime)
          .atZone(nowInMarket.getZone())
          .toOffsetDateTime();
    }
  }
}
